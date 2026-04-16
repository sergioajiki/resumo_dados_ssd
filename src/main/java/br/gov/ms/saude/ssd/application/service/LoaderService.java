package br.gov.ms.saude.ssd.application.service;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.AtendimentoEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.ProfissionalEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.SyncDiagnosticoEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.AtendimentoRepository;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.ProfissionalRepository;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.SyncDiagnosticoRepository;
import br.gov.ms.saude.ssd.domain.model.ExtractResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável pela carga (L do ETL) de dados transformados no banco H2.
 *
 * <p>Recebe um {@link ExtractResult} já transformado pelo {@code SyncService} e persiste
 * os registros usando upsert em batch. O {@code saveAll()} do Spring Data JPA usa
 * {@code merge} quando o ID já existe (atualização) e {@code persist} quando é novo
 * (inserção), garantindo idempotência na carga.</p>
 *
 * <p>O flush parcial a cada {@value #BATCH_SIZE} registros evita estouro de memória
 * na persistência de grandes volumes (16.000+ atendimentos).</p>
 *
 * @see FieldTransformerService
 * @see SyncService
 */
@Service
public class LoaderService {

    private static final Logger log = LoggerFactory.getLogger(LoaderService.class);

    /**
     * Tamanho do batch para flush parcial ao salvar no banco.
     * A cada 500 registros o contexto JPA é limpo para liberar memória.
     */
    static final int BATCH_SIZE = 500;

    private final AtendimentoRepository atendimentoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final SyncDiagnosticoRepository syncDiagnosticoRepository;
    private final FieldTransformerService transformer;

    /**
     * Injeta os repositórios e o serviço de transformação via construtor.
     *
     * @param atendimentoRepository    repositório de atendimentos
     * @param profissionalRepository   repositório de profissionais
     * @param syncDiagnosticoRepository repositório de diagnóstico de sync
     * @param transformer              serviço de transformação de campos
     */
    public LoaderService(AtendimentoRepository atendimentoRepository,
                         ProfissionalRepository profissionalRepository,
                         SyncDiagnosticoRepository syncDiagnosticoRepository,
                         FieldTransformerService transformer) {
        this.atendimentoRepository = atendimentoRepository;
        this.profissionalRepository = profissionalRepository;
        this.syncDiagnosticoRepository = syncDiagnosticoRepository;
        this.transformer = transformer;
    }

    /**
     * Carrega atendimentos do {@link ExtractResult} no banco H2.
     *
     * <p>Mapeia cada linha do resultado usando os índices dos headers para construir
     * as entidades {@link AtendimentoEntity}. Registros com ID inválido são ignorados
     * com aviso no log.</p>
     *
     * <p>Usa {@code @Transactional} para garantir atomicidade do batch inteiro.
     * Em caso de exceção, todos os registros do batch atual são revertidos.</p>
     *
     * @param result resultado da extração com headers e linhas de dados
     * @return contagem de registros persistidos (inseridos + atualizados)
     */
    @Transactional
    public int carregarAtendimentos(ExtractResult result) {
        if (result.isEmpty()) {
            log.info("Nenhum atendimento a carregar.");
            return 0;
        }

        List<String> headers = result.headers();
        List<AtendimentoEntity> batch = new ArrayList<>(BATCH_SIZE);
        int total = 0;

        for (List<Object> row : result.rows()) {
            AtendimentoEntity entity = mapAtendimento(headers, row);
            if (entity == null) continue;

            batch.add(entity);

            if (batch.size() >= BATCH_SIZE) {
                atendimentoRepository.saveAll(batch);
                total += batch.size();
                log.debug("Batch de {} atendimentos persistido. Total até agora: {}", batch.size(), total);
                batch.clear();
            }
        }

        // Persiste o restante do último batch (que pode ter menos que BATCH_SIZE)
        if (!batch.isEmpty()) {
            atendimentoRepository.saveAll(batch);
            total += batch.size();
        }

        log.info("Carga de atendimentos concluída: {} registros persistidos.", total);
        return total;
    }

    /**
     * Carrega profissionais do {@link ExtractResult} no banco H2.
     *
     * <p>Segue a mesma estratégia de batch e upsert de {@link #carregarAtendimentos}.</p>
     *
     * @param result resultado da extração de profissionais
     * @return contagem de registros persistidos
     */
    @Transactional
    public int carregarProfissionais(ExtractResult result) {
        if (result.isEmpty()) {
            log.info("Nenhum profissional a carregar.");
            return 0;
        }

        List<String> headers = result.headers();
        List<ProfissionalEntity> batch = new ArrayList<>(BATCH_SIZE);
        int total = 0;

        for (List<Object> row : result.rows()) {
            ProfissionalEntity entity = mapProfissional(headers, row);
            if (entity == null) continue;

            batch.add(entity);

            if (batch.size() >= BATCH_SIZE) {
                profissionalRepository.saveAll(batch);
                total += batch.size();
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            profissionalRepository.saveAll(batch);
            total += batch.size();
        }

        log.info("Carga de profissionais concluída: {} registros persistidos.", total);
        return total;
    }

    /**
     * Registra na tabela {@code sync_diagnostico} as contagens de campos-chave
     * logo após a conclusão de todas as passagens de uma sync de atendimentos.
     *
     * <p>Campos auditados: desfecho, cid, tp_nw_conclusao, cnes_estabelecimento,
     * endereco_completo, descricao_consulta.</p>
     *
     * <p>Consultar: {@code SELECT * FROM SYNC_DIAGNOSTICO ORDER BY DT_SYNC DESC}</p>
     *
     * @param tabela nome da tabela de destino (ex: "atendimento")
     */
    @Transactional
    public void registrarDiagnostico(String tabela) {
        long total = atendimentoRepository.count();
        List<SyncDiagnosticoEntity> rows = List.of(
                SyncDiagnosticoEntity.of(tabela, "desfecho",              total, atendimentoRepository.countByDesfechoIsNotNull()),
                SyncDiagnosticoEntity.of(tabela, "cid",                   total, atendimentoRepository.countByCidIsNotNull()),
                SyncDiagnosticoEntity.of(tabela, "tp_nw_conclusao",       total, atendimentoRepository.countByTpNwConclusaoIsNotNull()),
                SyncDiagnosticoEntity.of(tabela, "cnes_estabelecimento",  total, atendimentoRepository.countByCnesEstabelecimentoIsNotNull()),
                SyncDiagnosticoEntity.of(tabela, "endereco_completo",     total, atendimentoRepository.countByEnderecoCompletoIsNotNull()),
                SyncDiagnosticoEntity.of(tabela, "descricao_consulta",    total, atendimentoRepository.countByDescricaoConsultaIsNotNull())
        );
        syncDiagnosticoRepository.saveAll(rows);
        rows.forEach(r -> log.info("DIAG {}.{}: {}/{} ({}%)",
                r.getTabela(), r.getCampo(), r.getNaoNulos(), r.getTotal(), r.getPercentual()));
    }

    // -------------------------------------------------------------------------
    // Mapeamento de linhas para entidades
    // -------------------------------------------------------------------------

    /**
     * Mapeia uma linha de dados do Qlik para {@link AtendimentoEntity}.
     *
     * <p>Usa os headers para localizar cada campo por nome, independente da
     * ordem das colunas retornada pela fonte. Isso garante resiliência a
     * mudanças na ordem dos campos no Qlik.</p>
     *
     * @param headers lista de nomes de campos (ex: ["ID_ATENDIMENTO", "CNS_PACIENTE", ...])
     * @param row     valores correspondentes aos headers
     * @return entidade mapeada, ou {@code null} se o ID for inválido
     */
    private AtendimentoEntity mapAtendimento(List<String> headers, List<Object> row) {
        Long id = transformer.asLong(get(headers, row, "ID_ATENDIMENTO"));
        if (id == null) {
            log.warn("Linha ignorada: ID_ATENDIMENTO inválido — {}", row);
            return null;
        }

        AtendimentoEntity e = new AtendimentoEntity(id);
        e.setCnsPaciente(transformer.asCns(get(headers, row, "CNS_PACIENTE")));
        e.setDtNascimento(transformer.parseDate(get(headers, row, "DT_NASC_PACIENTE")));
        e.setRaca(transformer.asString(get(headers, row, "RACA_PACIENTE")));
        e.setEtnia(transformer.asString(get(headers, row, "ETNIA")));
        e.setMunicipio(transformer.asString(get(headers, row, "NOME_MUNICIPIO")));
        e.setIbge(transformer.asString(get(headers, row, "IBGE_ATEND")));
        e.setDtAgendamento(transformer.parseDateTime(get(headers, row, "DT_AGENDAMENTO")));
        e.setHrAgendamento(transformer.convertTime(get(headers, row, "HR_AGENDAMENTO")));
        e.setNomeMedico(transformer.asString(get(headers, row, "NOME_MEDICO")));
        e.setCboMedico(transformer.asString(get(headers, row, "CBO_MEDICO")));
        e.setStatusConsulta(transformer.asString(get(headers, row, "STATUS_CONSULTA")));
        e.setClassifConclusao(transformer.asString(get(headers, row, "CLASSIF_CONCLUSAO")));

        e.setDesfecho(transformer.asString(get(headers, row, "DESFECHO_ATEND")));
        e.setCid(transformer.asString(get(headers, row, "CID_CONSULTA")));
        e.setDtSolicitacao(transformer.parseDateTime(get(headers, row, "DT_SOLICITACAO")));
        e.setTipoZona(transformer.asString(get(headers, row, "TIPO_ZONA")));
        e.setTipoServico(transformer.asString(get(headers, row, "TIPO_SERV_ID")));
        e.setDtCriacao(transformer.parseDateTime(get(headers, row, "DT_NEW")));

        // Todos os campos mapeados em passagem única
        e.setCnesEstabelecimento(transformer.asString(get(headers, row, "CNES_NESTABELECIMENTO")));
        e.setIdEstabelecimento(transformer.asString(get(headers, row, "ID_ESTABELECIMENTO")));
        e.setIdMedico(transformer.asString(get(headers, row, "ID_MEDICO")));
        e.setClassificCor(transformer.asString(get(headers, row, "CLASSFIC_COR")));
        e.setTpNwConclusao(transformer.asString(get(headers, row, "TP_NW_CONCLUSAO")));
        e.setIdDigsaudeRef(transformer.asString(get(headers, row, "ID_DIGSAUDE_REF")));
        e.setTelefone(transformer.asString(get(headers, row, "TELEFONE")));
        e.setCepPaciente(transformer.asString(get(headers, row, "CEP_PACIENTE")));
        e.setRuaPaciente(transformer.asString(get(headers, row, "RUA_PACIENTE")));
        e.setNumPaciente(transformer.asString(get(headers, row, "NUM_PACIENTE")));
        e.setBairroPaciente(transformer.asString(get(headers, row, "BAIRRO_PACIENTE")));
        e.setComplementoEnd(transformer.asString(get(headers, row, "COMPLEMENTO_END_PACIENTE")));
        e.setDescricaoEndereco(transformer.asString(get(headers, row, "DESCRICAO_ENDERECO")));
        e.setEnderecoCompleto(transformer.asString(get(headers, row, "ENDERECO_COMPLETO")));
        e.setDescricaoConsulta(transformer.asString(get(headers, row, "DESCRICAO_CONSULTA")));

        // Faixa etária calculada a partir da data de nascimento
        e.setFaixaEtaria(transformer.calcularFaixaEtaria(e.getDtNascimento()));

        return e;
    }

    /**
     * Mapeia uma linha de dados do Qlik para {@link ProfissionalEntity}.
     *
     * @param headers lista de nomes de campos (ex: ["ID_USER", "NOME_USER", ...])
     * @param row     valores correspondentes aos headers
     * @return entidade mapeada, ou {@code null} se o ID for inválido
     */
    private ProfissionalEntity mapProfissional(List<String> headers, List<Object> row) {
        Long id = transformer.asLong(get(headers, row, "ID_USER"));
        if (id == null) {
            log.warn("Linha ignorada: ID_USER inválido — {}", row);
            return null;
        }

        ProfissionalEntity e = new ProfissionalEntity(id);
        e.setNome(transformer.asString(get(headers, row, "NOME_USER")));
        e.setCrm(transformer.asString(get(headers, row, "CRM_USER")));
        e.setEspecialidadeNome(transformer.asString(get(headers, row, "NOME_ESPECIALIDADE")));
        e.setMunicipio(transformer.asString(get(headers, row, "MUNICIPIO_USER")));
        e.setCodIbge(transformer.asString(get(headers, row, "COD_IBGE")));
        e.setTipoUsuario(transformer.asString(get(headers, row, "TP_USER")));
        e.setFaixaEtaria(transformer.asString(get(headers, row, "RANGE_IDADE")));
        e.setRacaCor(transformer.asString(get(headers, row, "RACA_COR_USER")));

        return e;
    }

    /**
     * Retorna o valor de uma coluna pelo nome do header, ou {@code null} se não encontrado.
     *
     * @param headers lista de nomes dos campos
     * @param row     valores correspondentes
     * @param campo   nome do campo a buscar
     * @return valor da coluna, ou {@code null} se o campo não existir na lista de headers
     */
    private Object get(List<String> headers, List<Object> row, String campo) {
        int idx = headers.indexOf(campo);
        if (idx < 0 || idx >= row.size()) return null;
        return row.get(idx);
    }
}
