package br.gov.ms.saude.ssd.application.service;

import br.gov.ms.saude.ssd.application.usecase.ExecutarSyncUseCase;
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
 * <p>Tabelas sincronizadas:</p>
 * <ul>
 *   <li>{@code DB_DIGSAUDE} → {@code atendimento} (incremental)</li>
 *   <li>{@code TEMPDB_USER} → {@code profissional} (incremental)</li>
 *   <li>{@code USERJORNADA} → {@code jornada_vagas} (full)</li>
 *   <li>{@code MUN_PILOTO} → {@code municipio_piloto} (truncate-reload)</li>
 *   <li>{@code MUN_SEMATIVIDADE} → {@code municipio_sem_atividade} (truncate-reload)</li>
 *   <li>{@code LINK} → {@code link} (truncate-reload)</li>
 *   <li>{@code MUNAPROV_PILOTO} → {@code munaprov_piloto} (truncate-reload)</li>
 *   <li>{@code MAPS_OFF} → {@code maps_off} (truncate-reload)</li>
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
    private static final String TABELA_QLIK_ATEND      = "DB_DIGSAUDE";

    /** Nome da tabela Qlik de profissionais. */
    private static final String TABELA_QLIK_PROF       = "TEMPDB_USER";

    /** Nome da tabela Qlik de jornadas/vagas. */
    private static final String TABELA_QLIK_JORNADA    = "USERJORNADA";

    /** Nome da tabela Qlik de municípios piloto. */
    private static final String TABELA_QLIK_MUN_PILOTO = "MUN_PILOTO";

    /** Nome da tabela Qlik de municípios sem atividade. */
    private static final String TABELA_QLIK_MUN_SEM    = "MUN_SEMATIVIDADE";

    /** Nome da tabela Qlik de ligações atendimento-profissional-jornada. */
    private static final String TABELA_QLIK_LINK     = "LINK";

    /** Nome da tabela Qlik de municípios aprovados no piloto. */
    private static final String TABELA_QLIK_MUNAPROV = "MUNAPROV_PILOTO";

    /** Nome da tabela Qlik do mapa offline. */
    private static final String TABELA_QLIK_MAPS_OFF = "MAPS_OFF";

    /** Nome da tabela de destino H2 para atendimentos. */
    private static final String TABELA_H2_ATEND      = "atendimento";

    /** Nome da tabela de destino H2 para profissionais. */
    private static final String TABELA_H2_PROF       = "profissional";

    /** Nome da tabela de destino H2 para municípios piloto. */
    private static final String TABELA_H2_MUN_PILOTO = "municipio_piloto";

    /** Nome da tabela de destino H2 para municípios sem atividade. */
    private static final String TABELA_H2_MUN_SEM    = "municipio_sem_atividade";

    /** Nome da tabela de destino H2 para link. */
    private static final String TABELA_H2_LINK     = "link";

    /** Nome da tabela de destino H2 para munaprov_piloto. */
    private static final String TABELA_H2_MUNAPROV = "munaprov_piloto";

    /** Nome da tabela de destino H2 para maps_off. */
    private static final String TABELA_H2_MAPS_OFF = "maps_off";

    /**
     * Campos a extrair da tabela DB_DIGSAUDE.
     * Extrair apenas os campos necessários reduz o volume de dados transferido.
     */
    /**
     * Todos os campos de atendimento extraídos em passagem única.
     * Caso DESFECHO ou CID voltem a 0% no SYNC_DIAGNOSTICO após a sync,
     * reverter para 4 passagens separadas (join implícito do Qlik corrompendo os campos).
     */
    private static final List<String> CAMPOS_ATENDIMENTO = List.of(
            "ID_ATENDIMENTO", "CNS_PACIENTE", "DT_NASC_PACIENTE", "RACA_PACIENTE",
            "ETNIA", "NOME_MUNICIPIO", "IBGE_ATEND", "DT_AGENDAMENTO", "HR_AGENDAMENTO",
            "NOME_MEDICO", "CBO_MEDICO", "STATUS_CONSULTA", "CLASSIF_CONCLUSAO",
            "TIPO_SERV_ID", "DESFECHO_ATEND", "CID_CONSULTA", "DT_SOLICITACAO",
            "TIPO_ZONA", "DT_NEW",
            "ID_ESTABELECIMENTO", "CNES_NESTABELECIMENTO", "ID_MEDICO",
            "CLASSFIC_COR", "TP_NW_CONCLUSAO", "ID_DIGSAUDE_REF",
            "TELEFONE", "CEP_PACIENTE", "RUA_PACIENTE", "NUM_PACIENTE",
            "BAIRRO_PACIENTE", "COMPLEMENTO_END_PACIENTE", "DESCRICAO_ENDERECO",
            "ENDERECO_COMPLETO", "DESCRICAO_CONSULTA"
    );

    /** Nome da tabela de destino H2 para jornadas/vagas. */
    private static final String TABELA_H2_JORNADA = "jornada_vagas";

    /** Campos a extrair da tabela USERJORNADA. Nomes confirmados via GET /api/v1/schema/campos?tabela=USERJORNADA. */
    private static final List<String> CAMPOS_JORNADA = List.of(
            "ID_JORNADA", "ID_USER_JORND", "DT_ATENDIMENTO", "ANO_VAGAS", "MES_NUM_VAGAS", "VAGAS"
    );

    /** Campos a extrair da tabela MUN_PILOTO. */
    private static final List<String> CAMPOS_MUN_PILOTO = List.of(
            "MUNICIPIO", "PILOTO_TF", "DEMANDANTE", "DT_TREINAMENTO"
    );

    /**
     * Campos a extrair da tabela MUN_SEMATIVIDADE.
     * Nomes assumidos por convenção — verificar com {@code GET /api/v1/schema/campos?tabela=MUN_SEMATIVIDADE}.
     */
    private static final List<String> CAMPOS_MUN_SEM_ATIV = List.of(
            "COD_IBGE", "MUNICIPIOCHECK"
    );

    /** Campos a extrair da tabela LINK. Nomes confirmados via {@code GET /api/v1/schema/campos?tabela=LINK}. */
    private static final List<String> CAMPOS_LINK = List.of(
            "CHAVE", "MUNICIPIO", "ID_DIGSAUDE_REF", "ID_JORND_REF", "ID_USER_REF", "ANO", "MES", "MES_ANO"
    );

    /** Campos a extrair da tabela MUNAPROV_PILOTO. Nomes confirmados via schema endpoint. */
    private static final List<String> CAMPOS_MUNAPROV = List.of(
            "MUNICIPIO", "CNES_INDICADO"
    );

    /** Campos a extrair da tabela MAPS_OFF. Nomes confirmados via schema endpoint. */
    private static final List<String> CAMPOS_MAPS_OFF = List.of(
            "co_IBGE", "co_MUNICIPIO"
    );

    /** Campos a extrair da tabela TEMPDB_USER. */
    private static final List<String> CAMPOS_PROFISSIONAL = List.of(
            "ID_USER", "NOME_USER", "CRM_USER", "NOME_ESPECIALIDADE",
            "MUNICIPIO_USER", "COD_IBGE", "TP_USER", "RANGE_IDADE", "RACA_COR_USER"
    );

    /** Diferencia o loader a usar em {@link #sincronizarTabela}. */
    private enum TipoTabela { ATENDIMENTO, PROFISSIONAL, JORNADA_VAGAS, MUNICIPIO_PILOTO, MUNICIPIO_SEM_ATIVIDADE,
        LINK, MUNAPROV_PILOTO, MAPS_OFF }

    /** Flag thread-safe para garantir exclusão mútua entre execuções de sync. */
    private final AtomicBoolean syncEmAndamento = new AtomicBoolean(false);

    private final DataExtractorPort dataExtractorPort;
    private final SyncRepositoryPort syncRepositoryPort;
    private final LoaderService loaderService;

    /**
     * Injeta dependências via construtor.
     *
     * @param dataExtractorPort  porta de extração (pode ser MockAdapter, QlikEngineAdapter, etc.)
     * @param syncRepositoryPort porta de persistência do histórico de sync
     * @param loaderService      serviço de carga em batch no H2
     */
    public SyncService(DataExtractorPort dataExtractorPort,
                       SyncRepositoryPort syncRepositoryPort,
                       LoaderService loaderService) {
        this.dataExtractorPort = dataExtractorPort;
        this.syncRepositoryPort = syncRepositoryPort;
        this.loaderService = loaderService;
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
                    CAMPOS_ATENDIMENTO, incremental, TipoTabela.ATENDIMENTO));
            logs.add(sincronizarTabela(TABELA_QLIK_PROF, TABELA_H2_PROF,
                    CAMPOS_PROFISSIONAL, incremental, TipoTabela.PROFISSIONAL));
            // Jornada e tabelas de lookup: sempre full (sem campo DT_NEW / tabelas pequenas)
            logs.add(sincronizarTabela(TABELA_QLIK_JORNADA, TABELA_H2_JORNADA,
                    CAMPOS_JORNADA, false, TipoTabela.JORNADA_VAGAS));
            logs.add(sincronizarTabela(TABELA_QLIK_MUN_PILOTO, TABELA_H2_MUN_PILOTO,
                    CAMPOS_MUN_PILOTO, false, TipoTabela.MUNICIPIO_PILOTO));
            logs.add(sincronizarTabela(TABELA_QLIK_MUN_SEM, TABELA_H2_MUN_SEM,
                    CAMPOS_MUN_SEM_ATIV, false, TipoTabela.MUNICIPIO_SEM_ATIVIDADE));
            logs.add(sincronizarTabela(TABELA_QLIK_LINK,     TABELA_H2_LINK,     CAMPOS_LINK,     false, TipoTabela.LINK));
            logs.add(sincronizarTabela(TABELA_QLIK_MUNAPROV, TABELA_H2_MUNAPROV, CAMPOS_MUNAPROV, false, TipoTabela.MUNAPROV_PILOTO));
            logs.add(sincronizarTabela(TABELA_QLIK_MAPS_OFF, TABELA_H2_MAPS_OFF, CAMPOS_MAPS_OFF, false, TipoTabela.MAPS_OFF));
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
     * @param tabelaQlik  nome da tabela na fonte Qlik
     * @param tabelaH2    nome da tabela de destino no H2
     * @param campos      lista de campos a extrair
     * @param incremental se {@code true}, usa watermark da última sync
     * @param tipo        identifica qual loader usar
     * @return log da execução desta tabela
     */
    private SyncLog sincronizarTabela(String tabelaQlik, String tabelaH2,
                                       List<String> campos, boolean incremental,
                                       TipoTabela tipo) {
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

            int carregados = switch (tipo) {
                case ATENDIMENTO             -> loaderService.carregarAtendimentos(result);
                case PROFISSIONAL            -> loaderService.carregarProfissionais(result);
                case JORNADA_VAGAS           -> loaderService.carregarJornadaVagas(result);
                case MUNICIPIO_PILOTO        -> loaderService.carregarMunicipiosPiloto(result);
                case MUNICIPIO_SEM_ATIVIDADE -> loaderService.carregarMunicipiosSemAtividade(result);
                case LINK            -> loaderService.carregarLink(result);
                case MUNAPROV_PILOTO -> loaderService.carregarMunaprovPiloto(result);
                case MAPS_OFF        -> loaderService.carregarMapsOff(result);
            };

            if (tipo == TipoTabela.ATENDIMENTO) {
                // DIAGNÓSTICO — persiste contagens no banco e loga resumo
                loaderService.registrarDiagnostico(tabelaH2);
            }

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
