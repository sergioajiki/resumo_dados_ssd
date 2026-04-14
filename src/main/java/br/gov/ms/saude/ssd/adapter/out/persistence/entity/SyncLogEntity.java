package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import br.gov.ms.saude.ssd.domain.model.SyncLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidade JPA que mapeia a tabela {@code sync_log} no banco H2.
 *
 * <p>Cada linha representa uma execução do pipeline ETL para uma tabela específica.
 * O mapeamento entre esta entidade e o domain object {@link SyncLog} é feito
 * pelo {@code SyncRepositoryAdapter}.</p>
 *
 * <p>O {@code id} é gerado pelo banco (AUTO_INCREMENT) — ao contrário das entidades
 * de atendimento e profissional, o sync_log não tem ID na fonte de dados.</p>
 *
 * @see br.gov.ms.saude.ssd.adapter.out.persistence.repository.SyncLogRepository
 * @see br.gov.ms.saude.ssd.adapter.out.persistence.SyncRepositoryAdapter
 */
@Entity
@Table(name = "sync_log")
public class SyncLogEntity {

    /** PK auto-incrementada gerenciada pelo banco H2. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Nome da tabela de destino sincronizada (ex: "atendimento", "profissional"). */
    @Column(name = "tabela", nullable = false, length = 100)
    private String tabela;

    /** Timestamp de início da execução do ETL. */
    @Column(name = "iniciado_em", nullable = false)
    private LocalDateTime iniciadoEm;

    /** Timestamp de conclusão. {@code null} enquanto a sync estiver em andamento. */
    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    /** Total de registros lidos da fonte de dados. */
    @Column(name = "registros_extraidos")
    private int registrosExtraidos;

    /** Registros inseridos pela primeira vez no banco local. */
    @Column(name = "registros_novos")
    private int registrosNovos;

    /** Registros já existentes que foram atualizados (upsert). */
    @Column(name = "registros_atualizados")
    private int registrosAtualizados;

    /**
     * Status da sincronização. Armazenado como string para legibilidade nas
     * consultas SQL diretas. Mapeado para/de {@link SyncLog.SyncStatus} pelo adapter.
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Mensagem de erro em caso de falha. {@code null} em execuções bem-sucedidas. */
    @Column(name = "erro", length = 4000)
    private String erro;

    /** Construtor padrão requerido pelo JPA. */
    protected SyncLogEntity() {
    }

    /**
     * Cria uma entidade de log a partir de um domain object {@link SyncLog}.
     *
     * <p>Usado pelo {@code SyncRepositoryAdapter} ao persistir um novo log.</p>
     *
     * @param log domain object de sincronização
     * @return nova entidade pronta para persistência
     */
    public static SyncLogEntity of(SyncLog log) {
        SyncLogEntity entity = new SyncLogEntity();
        entity.id = log.id();
        entity.tabela = log.tabela();
        entity.iniciadoEm = log.iniciadoEm();
        entity.concluidoEm = log.concluidoEm();
        entity.registrosExtraidos = log.registrosExtraidos();
        entity.registrosNovos = log.registrosNovos();
        entity.registrosAtualizados = log.registrosAtualizados();
        entity.status = log.status().name();
        entity.erro = log.erro();
        return entity;
    }

    /**
     * Converte esta entidade para o domain object {@link SyncLog}.
     *
     * <p>Usado pelo {@code SyncRepositoryAdapter} ao ler registros do banco.</p>
     *
     * @return domain object equivalente a esta entidade
     */
    public SyncLog toDomain() {
        return new SyncLog(
                id,
                tabela,
                iniciadoEm,
                concluidoEm,
                registrosExtraidos,
                registrosNovos,
                registrosAtualizados,
                SyncLog.SyncStatus.valueOf(status),
                erro
        );
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public String getTabela() { return tabela; }
    public LocalDateTime getIniciadoEm() { return iniciadoEm; }
    public LocalDateTime getConcluidoEm() { return concluidoEm; }
    public int getRegistrosExtraidos() { return registrosExtraidos; }
    public int getRegistrosNovos() { return registrosNovos; }
    public int getRegistrosAtualizados() { return registrosAtualizados; }
    public String getStatus() { return status; }
    public String getErro() { return erro; }
}
