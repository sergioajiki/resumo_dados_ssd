package br.gov.ms.saude.ssd.application.service;

import br.gov.ms.saude.ssd.application.usecase.ExecutarSyncUseCase;
import br.gov.ms.saude.ssd.config.SyncProperties;
import br.gov.ms.saude.ssd.domain.exception.DataSourceUnavailableException;
import br.gov.ms.saude.ssd.domain.exception.SyncAlreadyRunningException;
import br.gov.ms.saude.ssd.domain.model.ExtractOptions;
import br.gov.ms.saude.ssd.domain.model.ExtractResult;
import br.gov.ms.saude.ssd.domain.model.SyncLog;
import br.gov.ms.saude.ssd.domain.port.out.DataExtractorPort;
import br.gov.ms.saude.ssd.domain.port.out.SyncRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serviço de sincronização ETL — implementa {@link ExecutarSyncUseCase}.
 *
 * <p>Orquestra o pipeline completo de extração, transformação e carga:</p>
 * <ol>
 *   <li>Verifica a saúde da fonte de dados</li>
 *   <li>Para incremental: obtém o watermark da última sync bem-sucedida</li>
 *   <li>Extrai dados via {@link DataExtractorPort}</li>
 *   <li>Persiste via {@link LoaderService} (que aplica o transformador internamente)</li>
 *   <li>Registra o resultado no {@link SyncRepositoryPort}</li>
 * </ol>
 *
 * <p>Um {@link AtomicBoolean} garante que apenas uma sync rode por vez,
 * lançando {@link SyncAlreadyRunningException} se já houver uma em andamento.</p>
 *
 * <p>Tabelas sincronizadas nesta versão:</p>
 * <ul>
 *   <li>{@code DB_DIGSAUDE} → tabela {@code atendimento}</li>
 *   <li>{@code TEMPDB_USER} → tabela {@code profissional}</li>
 * </ul>
 *
 * @see LoaderService
 * @see DataExtractorPort
 * @see SyncRepositoryPort
 */
@Service
public class SyncService implements ExecutarSyncUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    /** Nome da tabela Qlik de atendimentos. */
    private static final String TABELA_QLIK_ATEND = "DB_DIGSAUDE";

    /** Nome da tabela Qlik de profissionais. */
    private static final String TABELA_QLIK_PROF = "TEMPDB_USER";

    /** Nome da tabela de destino H2 para atendimentos. */
    private static final String TABELA_H2_ATEND = "atendimento";

    /** Nome da tabela de destino H2 para profissionais. */
    private static final String TABELA_H2_PROF = "profissional";

    /**
     * Campos a extrair da tabela DB_DIGSAUDE.
     * Extrair apenas os campos necessários reduz o volume de dados transferido.
     */
    private static final List<String> CAMPOS_ATENDIMENTO = List.of(
            "ID_ATENDIMENTO", "CNS_PACIENTE", "DT_NASC_PACIENTE", "RACA_PACIENTE",
            "ETNIA", "NOME_MUNICIPIO", "IBGE_ATEND", "DT_AGENDAMENTO", "HR_AGENDAMENTO",
            "NOME_MEDICO", "CBO_MEDICO", "STATUS_CONSULTA", "DESFECHO_ATEND",
            "CID_CONSULTA", "DT_SOLICITACAO", "TIPO_ZONA", "DT_NEW"
    );

    /** Campos a extrair da tabela TEMPDB_USER. */
    private static final List<String> CAMPOS_PROFISSIONAL = List.of(
            "ID_USER", "NOME_USER", "CRM_USER", "NOME_ESPECIALIDADE",
            "MUNICIPIO_USER", "COD_IBGE", "TP_USER", "RANGE_IDADE", "RACA_COR_USER"
    );

    /** Flag thread-safe para garantir exclusão mútua entre execuções de sync. */
    private final AtomicBoolean syncEmAndamento = new AtomicBoolean(false);

    private final DataExtractorPort dataExtractorPort;
    private final SyncRepositoryPort syncRepositoryPort;
    private final LoaderService loaderService;
    private final SyncProperties syncProperties;

    /**
     * Injeta dependências via construtor.
     *
     * @param dataExtractorPort  porta de extração (pode ser MockAdapter, QlikEngineAdapter, etc.)
     * @param syncRepositoryPort porta de persistência do histórico de sync
     * @param loaderService      serviço de carga em batch no H2
     * @param syncProperties     configurações de sync (strategy, watermarkField, schedule)
     */
    public SyncService(DataExtractorPort dataExtractorPort,
                       SyncRepositoryPort syncRepositoryPort,
                       LoaderService loaderService,
                       SyncProperties syncProperties) {
        this.dataExtractorPort = dataExtractorPort;
        this.syncRepositoryPort = syncRepositoryPort;
        this.loaderService = loaderService;
        this.syncProperties = syncProperties;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extrai todos os registros de cada tabela (sem watermark) e os carrega
     * no banco H2. Útil para inicialização do sistema ou recuperação após falha.</p>
     */
    @Override
    public SyncResult executarFullSync() {
        log.info("Iniciando full sync.");
        return executarSync(false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extrai apenas os registros com {@code DT_NEW} posterior ao watermark
     * da última sync bem-sucedida. Se não houver sync anterior, executa full sync.</p>
     */
    @Override
    public SyncResult executarIncrementalSync() {
        log.info("Iniciando incremental sync.");
        return executarSync(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SyncLog getUltimoSync(String tabela) {
        return syncRepositoryPort.getHistory(tabela, 1)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Nenhuma sincronização encontrada para a tabela: " + tabela));
    }

    // -------------------------------------------------------------------------
    // Lógica interna
    // -------------------------------------------------------------------------

    /**
     * Executa o pipeline ETL para todas as tabelas configuradas.
     *
     * <p>Garante exclusão mútua via {@link #syncEmAndamento} e registra
     * o resultado de cada tabela em {@link SyncRepositoryPort}.</p>
     *
     * @param incremental {@code true} para incremental, {@code false} para full
     * @return resultado consolidado com logs por tabela
     */
    private SyncResult executarSync(boolean incremental) {
        if (!syncEmAndamento.compareAndSet(false, true)) {
            throw new SyncAlreadyRunningException("Já existe uma sincronização em andamento.");
        }

        List<SyncLog> logs = new ArrayList<>();
        try {
            logs.add(sincronizarTabela(TABELA_QLIK_ATEND, TABELA_H2_ATEND,
                    CAMPOS_ATENDIMENTO, incremental, true));
            logs.add(sincronizarTabela(TABELA_QLIK_PROF, TABELA_H2_PROF,
                    CAMPOS_PROFISSIONAL, incremental, false));
        } finally {
            syncEmAndamento.set(false);
        }

        boolean sucesso = logs.stream()
                .allMatch(l -> l.status() == SyncLog.SyncStatus.SUCCESS);

        long totalExtraidos = logs.stream()
                .mapToLong(SyncLog::registrosExtraidos).sum();

        String resumo = String.format("%d tabela(s) sincronizada(s), %d registro(s) extraídos, %s erros.",
                logs.size(), totalExtraidos, sucesso ? "0" : "1+");

        return new SyncResult(logs, sucesso, resumo);
    }

    /**
     * Sincroniza uma única tabela: extrai, carrega e registra o log.
     *
     * @param tabelaQlik   nome da tabela na fonte Qlik
     * @param tabelaH2     nome da tabela de destino no H2
     * @param campos       lista de campos a extrair
     * @param incremental  se {@code true}, usa watermark da última sync
     * @param isAtendimento diferencia o loader a usar (atendimento vs. profissional)
     * @return log da execução desta tabela
     */
    private SyncLog sincronizarTabela(String tabelaQlik, String tabelaH2,
                                       List<String> campos, boolean incremental,
                                       boolean isAtendimento) {
        SyncLog logIniciado = SyncLog.iniciando(tabelaH2);
        syncRepositoryPort.recordSync(logIniciado);

        try {
            ExtractResult result;

            if (incremental) {
                LocalDateTime watermark = syncRepositoryPort
                        .getLastSyncTime(tabelaH2)
                        .orElse(null);

                if (watermark != null) {
                    log.info("Extraindo {} incrementalmente desde {}", tabelaQlik, watermark);
                    result = dataExtractorPort.extractSince(tabelaQlik, campos, watermark);
                } else {
                    log.info("Sem watermark para {}. Executando full extract.", tabelaQlik);
                    result = dataExtractorPort.extractTable(tabelaQlik, campos, ExtractOptions.defaults());
                }
            } else {
                log.info("Extraindo {} (full).", tabelaQlik);
                result = dataExtractorPort.extractTable(tabelaQlik, campos, ExtractOptions.defaults());
            }

            int carregados = isAtendimento
                    ? loaderService.carregarAtendimentos(result)
                    : loaderService.carregarProfissionais(result);

            SyncLog logConcluido = logIniciado.concluido(result.totalExtraidos(), carregados, 0);
            syncRepositoryPort.recordSync(logConcluido);

            log.info("Sync de {} concluída: {} registros.", tabelaQlik, carregados);
            return logConcluido;

        } catch (Exception ex) {
            log.error("Falha na sync de {}: {}", tabelaQlik, ex.getMessage(), ex);
            SyncLog logFalhou = logIniciado.falhou(ex.getMessage());
            syncRepositoryPort.recordSync(logFalhou);
            return logFalhou;
        }
    }
}
