package br.gov.ms.saude.ssd.application.service;

import br.gov.ms.saude.ssd.adapter.out.persistence.entity.AtendimentoEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.JornadaVagasEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.LinkEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.MapsOffEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.MunaprovPilotoEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.MunicipioPilotoEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.MunicipioSemAtividadeEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.ProfissionalEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.entity.SyncDiagnosticoEntity;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.AtendimentoRepository;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.JornadaVagasRepository;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.LinkRepository;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.MapsOffRepository;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.MunaprovPilotoRepository;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.MunicipioPilotoRepository;
import br.gov.ms.saude.ssd.adapter.out.persistence.repository.MunicipioSemAtividadeRepository;
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
    private final JornadaVagasRepository jornadaVagasRepository;
    private final MunicipioPilotoRepository municipioPilotoRepository;
    private final MunicipioSemAtividadeRepository municipioSemAtividadeRepository;
    private final LinkRepository linkRepository;
    private final MunaprovPilotoRepository munaprovPilotoRepository;
    private final MapsOffRepository mapsOffRepository;
    private final SyncDiagnosticoRepository syncDiagnosticoRepository;
    private final FieldTransformerService transformer;

    /**
     * Injeta os repositórios e o serviço de transformação via construtor.
     *
     * @param atendimentoRepository           repositório de atendimentos
     * @param profissionalRepository          repositório de profissionais
     * @param jornadaVagasRepository          repositório de jornadas/vagas
     * @param municipioPilotoRepository       repositório de municípios piloto
     * @param municipioSemAtividadeRepository repositório de municípios sem atividade
     * @param linkRepository                  repositório de link
     * @param munaprovPilotoRepository        repositório de munaprov_piloto
     * @param mapsOffRepository               repositório de maps_off
     * @param syncDiagnosticoRepository       repositório de diagnóstico de sync
     * @param transformer                     serviço de transformação de campos
     */
    public LoaderService(AtendimentoRepository atendimentoRepository,
                         ProfissionalRepository profissionalRepository,
                         JornadaVagasRepository jornadaVagasRepository,
                         MunicipioPilotoRepository municipioPilotoRepository,
                         MunicipioSemAtividadeRepository municipioSemAtividadeRepository,
                         LinkRepository linkRepository,
                         MunaprovPilotoRepository munaprovPilotoRepository,
                         MapsOffRepository mapsOffRepository,
                         SyncDiagnosticoRepository syncDiagnosticoRepository,
                         FieldTransformerService transformer) {
        this.atendimentoRepository = atendimentoRepository;
        this.profissionalRepository = profissionalRepository;
        this.jornadaVagasRepository = jornadaVagasRepository;
        this.municipioPilotoRepository = municipioPilotoRepository;
        this.municipioSemAtividadeRepository = municipioSemAtividadeRepository;
        this.linkRepository = linkRepository;
        this.munaprovPilotoRepository = munaprovPilotoRepository;
        this.mapsOffRepository = mapsOffRepository;
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
     * Carrega jornadas/vagas do {@link ExtractResult} no banco H2.
     *
     * <p>Segue a mesma estratégia de batch e upsert de {@link #carregarProfissionais}.</p>
     *
     * @param result resultado da extração de jornadas/vagas
     * @return contagem de registros persistidos
     */
    @Transactional
    public int carregarJornadaVagas(ExtractResult result) {
        if (result.isEmpty()) {
            log.info("Nenhuma jornada/vaga a carregar.");
            return 0;
        }

        List<String> headers = result.headers();
        List<JornadaVagasEntity> batch = new ArrayList<>(BATCH_SIZE);
        int total = 0;

        for (List<Object> row : result.rows()) {
            JornadaVagasEntity entity = mapJornadaVagas(headers, row);
            if (entity == null) continue;

            batch.add(entity);

            if (batch.size() >= BATCH_SIZE) {
                jornadaVagasRepository.saveAll(batch);
                total += batch.size();
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            jornadaVagasRepository.saveAll(batch);
            total += batch.size();
        }

        log.info("Carga de jornadas/vagas concluída: {} registros persistidos.", total);
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
     * Mapeia uma linha de dados do Qlik para {@link JornadaVagasEntity}.
     *
     * @param headers lista de nomes de campos (ex: ["ID_JORNADA", "ID_USER", ...])
     * @param row     valores correspondentes aos headers
     * @return entidade mapeada, ou {@code null} se o ID for inválido
     */
    private JornadaVagasEntity mapJornadaVagas(List<String> headers, List<Object> row) {
        Long id = transformer.asLong(get(headers, row, "ID_JORNADA"));
        if (id == null) {
            log.warn("Linha ignorada: ID_JORNADA inválido — {}", row);
            return null;
        }

        JornadaVagasEntity e = new JornadaVagasEntity(id);
        e.setProfissionalId(transformer.asLong(get(headers, row, "ID_USER_JORND")));
        e.setDtAtendimento(transformer.parseDate(get(headers, row, "DT_ATENDIMENTO")));

        Long ano   = transformer.asLong(get(headers, row, "ANO_VAGAS"));
        Long mes   = transformer.asLong(get(headers, row, "MES_NUM_VAGAS"));
        Long vagas = transformer.asLong(get(headers, row, "VAGAS"));
        e.setAno(ano   != null ? ano.intValue()   : null);
        e.setMes(mes   != null ? mes.intValue()   : null);
        e.setVagas(vagas != null ? vagas.intValue() : null);

        return e;
    }

    /**
     * Carrega municípios piloto do {@link ExtractResult} no banco H2.
     *
     * <p>Estratégia truncate-reload: apaga todos os registros anteriores e reinsere.
     * Seguro porque a tabela {@code municipio_piloto} não é referenciada por FK externas.</p>
     *
     * @param result resultado da extração de {@code MUN_PILOTO}
     * @return contagem de registros carregados
     */
    @Transactional
    public int carregarMunicipiosPiloto(ExtractResult result) {
        municipioPilotoRepository.deleteAllInBatch();

        if (result.isEmpty()) {
            log.info("Nenhum município piloto a carregar.");
            return 0;
        }

        List<String> headers = result.headers();
        List<MunicipioPilotoEntity> entidades = new ArrayList<>();

        for (List<Object> row : result.rows()) {
            MunicipioPilotoEntity entity = mapMunicipioPiloto(headers, row);
            if (entity != null) entidades.add(entity);
        }

        municipioPilotoRepository.saveAll(entidades);
        log.info("municipio_piloto: {} registros carregados.", entidades.size());
        return entidades.size();
    }

    /**
     * Carrega municípios sem atividade do {@link ExtractResult} no banco H2.
     *
     * <p>Estratégia truncate-reload. Campos assumidos no Qlik {@code MUN_SEMATIVIDADE}:
     * {@code COD_IBGE} e {@code MUNICIPIO}. Verificar via
     * {@code GET /api/v1/schema/campos?tabela=MUN_SEMATIVIDADE} se os nomes divergirem.</p>
     *
     * @param result resultado da extração de {@code MUN_SEMATIVIDADE}
     * @return contagem de registros carregados
     */
    @Transactional
    public int carregarMunicipiosSemAtividade(ExtractResult result) {
        municipioSemAtividadeRepository.deleteAllInBatch();

        if (result.isEmpty()) {
            log.info("Nenhum município sem atividade a carregar.");
            return 0;
        }

        List<String> headers = result.headers();
        List<MunicipioSemAtividadeEntity> entidades = new ArrayList<>();

        for (List<Object> row : result.rows()) {
            MunicipioSemAtividadeEntity entity = mapMunicipioSemAtividade(headers, row);
            if (entity != null) entidades.add(entity);
        }

        municipioSemAtividadeRepository.saveAll(entidades);
        log.info("municipio_sem_atividade: {} registros carregados.", entidades.size());
        return entidades.size();
    }

    /**
     * Mapeia uma linha de {@code MUN_PILOTO} para {@link MunicipioPilotoEntity}.
     *
     * @param headers lista de nomes de campos
     * @param row     valores correspondentes
     * @return entidade mapeada, ou {@code null} se o nome do município for inválido
     */
    private MunicipioPilotoEntity mapMunicipioPiloto(List<String> headers, List<Object> row) {
        String nome = transformer.asString(get(headers, row, "MUNICIPIO"));
        if (nome == null || nome.isBlank()) {
            log.warn("Linha ignorada: MUNICIPIO inválido — {}", row);
            return null;
        }

        MunicipioPilotoEntity e = new MunicipioPilotoEntity();
        e.setNome(nome);
        String pilotoStr = transformer.asString(get(headers, row, "PILOTO_TF"));
        e.setPiloto(pilotoStr != null
                && (pilotoStr.equalsIgnoreCase("true")
                    || pilotoStr.equals("1")
                    || pilotoStr.equalsIgnoreCase("sim")));
        e.setDemandante(transformer.asString(get(headers, row, "DEMANDANTE")));
        e.setDtTreinamento(transformer.parseDate(get(headers, row, "DT_TREINAMENTO")));
        e.setAtivo(true);
        return e;
    }

    /**
     * Mapeia uma linha de {@code MUN_SEMATIVIDADE} para {@link MunicipioSemAtividadeEntity}.
     *
     * <p>Campos assumidos: {@code COD_IBGE} e {@code MUNICIPIO}.
     * Se os nomes reais diferirem, ajustar aqui e em {@code SyncService.CAMPOS_MUN_SEM_ATIV}.</p>
     *
     * @param headers lista de nomes de campos
     * @param row     valores correspondentes
     * @return entidade mapeada, ou {@code null} se cod_ibge for inválido
     */
    private MunicipioSemAtividadeEntity mapMunicipioSemAtividade(List<String> headers, List<Object> row) {
        String codIbge = transformer.asString(get(headers, row, "COD_IBGE"));
        if (codIbge == null || codIbge.isBlank()) {
            log.warn("Linha ignorada: COD_IBGE inválido — {}", row);
            return null;
        }

        MunicipioSemAtividadeEntity e = new MunicipioSemAtividadeEntity(codIbge);
        e.setNome(transformer.asString(get(headers, row, "MUNICIPIOCHECK")));
        return e;
    }

    /**
     * Carrega registros de link do {@link ExtractResult} no banco H2.
     *
     * <p>Estratégia truncate-reload: apaga todos os registros anteriores e reinsere.</p>
     *
     * @param result resultado da extração de {@code LINK}
     * @return contagem de registros carregados
     */
    @Transactional
    public int carregarLink(ExtractResult result) {
        linkRepository.deleteAllInBatch();

        if (result.isEmpty()) {
            log.info("Nenhum link a carregar.");
            return 0;
        }

        List<LinkEntity> entidades = new ArrayList<>();
        for (List<Object> row : result.rows()) {
            LinkEntity entity = mapLink(result.headers(), row);
            if (entity != null) entidades.add(entity);
        }

        linkRepository.saveAll(entidades);
        log.info("link: {} registros carregados.", entidades.size());
        return entidades.size();
    }

    /**
     * Carrega municípios aprovados no piloto do {@link ExtractResult} no banco H2.
     *
     * <p>Estratégia truncate-reload.</p>
     *
     * @param result resultado da extração de {@code MUNAPROV_PILOTO}
     * @return contagem de registros carregados
     */
    @Transactional
    public int carregarMunaprovPiloto(ExtractResult result) {
        munaprovPilotoRepository.deleteAllInBatch();

        if (result.isEmpty()) {
            log.info("Nenhum munaprov_piloto a carregar.");
            return 0;
        }

        List<MunaprovPilotoEntity> entidades = new ArrayList<>();
        for (List<Object> row : result.rows()) {
            String municipio = transformer.asString(get(result.headers(), row, "MUNICIPIO"));
            if (municipio == null || municipio.isBlank()) continue;
            MunaprovPilotoEntity e = new MunaprovPilotoEntity();
            e.setMunicipio(municipio);
            e.setCnesIndicado(transformer.asString(get(result.headers(), row, "CNES_INDICADO")));
            entidades.add(e);
        }

        munaprovPilotoRepository.saveAll(entidades);
        log.info("munaprov_piloto: {} registros carregados.", entidades.size());
        return entidades.size();
    }

    /**
     * Carrega municípios para mapa offline do {@link ExtractResult} no banco H2.
     *
     * <p>Estratégia truncate-reload.</p>
     *
     * @param result resultado da extração de {@code MAPS_OFF}
     * @return contagem de registros carregados
     */
    @Transactional
    public int carregarMapsOff(ExtractResult result) {
        mapsOffRepository.deleteAllInBatch();

        if (result.isEmpty()) {
            log.info("Nenhum maps_off a carregar.");
            return 0;
        }

        List<MapsOffEntity> entidades = new ArrayList<>();
        for (List<Object> row : result.rows()) {
            String coIbge = transformer.asString(get(result.headers(), row, "co_IBGE"));
            if (coIbge == null || coIbge.isBlank()) continue;
            MapsOffEntity e = new MapsOffEntity(coIbge);
            e.setMunicipio(transformer.asString(get(result.headers(), row, "co_MUNICIPIO")));
            entidades.add(e);
        }

        mapsOffRepository.saveAll(entidades);
        log.info("maps_off: {} registros carregados.", entidades.size());
        return entidades.size();
    }

    /**
     * Mapeia uma linha de {@code LINK} para {@link LinkEntity}.
     *
     * @param headers lista de nomes de campos
     * @param row     valores correspondentes
     * @return entidade mapeada, ou {@code null} se CHAVE for inválida
     */
    private LinkEntity mapLink(List<String> headers, List<Object> row) {
        // O campo CHAVE no Qlik retorna sempre nulo; ID_DIGSAUDE_REF contém a chave composta real
        String chave = transformer.asString(get(headers, row, "ID_DIGSAUDE_REF"));
        if (chave == null || chave.isBlank()) {
            log.warn("Linha ignorada: ID_DIGSAUDE_REF inválido — {}", row);
            return null;
        }

        LinkEntity e = new LinkEntity(chave);
        e.setMunicipio(transformer.asString(get(headers, row, "MUNICIPIO")));
        e.setIdDigsaudeRef(chave); // mesmo valor que a chave
        e.setIdJorndRef(transformer.asString(get(headers, row, "ID_JORND_REF")));
        e.setIdUserRef(transformer.asLong(get(headers, row, "ID_USER_REF")));
        Long ano = transformer.asLong(get(headers, row, "ANO"));
        e.setAno(ano != null ? ano.intValue() : null);
        e.setMes(transformer.asString(get(headers, row, "MES")));
        e.setMesAno(transformer.asString(get(headers, row, "MES_ANO")));
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
