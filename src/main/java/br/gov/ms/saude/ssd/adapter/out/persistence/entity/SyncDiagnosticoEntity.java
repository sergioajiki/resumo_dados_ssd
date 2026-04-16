package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Entidade JPA que mapeia a tabela {@code sync_diagnostico} no banco H2.
 *
 * <p>Cada linha representa a contagem de não-nulos de um campo específico
 * após a conclusão de uma sincronização ETL. Permite auditoria de completude
 * dos dados sem depender do terminal.</p>
 *
 * <p>Consultar: {@code SELECT * FROM SYNC_DIAGNOSTICO ORDER BY DT_SYNC DESC}</p>
 *
 * @see br.gov.ms.saude.ssd.adapter.out.persistence.repository.SyncDiagnosticoRepository
 */
@Entity
@Table(name = "sync_diagnostico")
public class SyncDiagnosticoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Timestamp do momento em que o diagnóstico foi registrado. */
    @Column(name = "dt_sync", nullable = false)
    private LocalDateTime dtSync;

    /** Nome da tabela de destino auditada (ex: "atendimento"). */
    @Column(name = "tabela", nullable = false, length = 50)
    private String tabela;

    /** Nome do campo auditado (ex: "desfecho", "cid", "tp_nw_conclusao"). */
    @Column(name = "campo", nullable = false, length = 50)
    private String campo;

    /** Total de registros na tabela no momento do diagnóstico. */
    @Column(name = "total", nullable = false)
    private long total;

    /** Quantidade de registros com o campo não-nulo. */
    @Column(name = "nao_nulos", nullable = false)
    private long naoNulos;

    /** Percentual de preenchimento: {@code naoNulos * 100 / total}, com 1 casa decimal. */
    @Column(name = "percentual", nullable = false, precision = 5, scale = 1)
    private BigDecimal percentual;

    /** Construtor padrão requerido pelo JPA. */
    protected SyncDiagnosticoEntity() {
    }

    /**
     * Cria uma linha de diagnóstico calculando o percentual automaticamente.
     *
     * @param tabela   nome da tabela de destino
     * @param campo    nome do campo auditado
     * @param total    total de registros na tabela
     * @param naoNulos quantidade de registros com o campo não-nulo
     * @return entidade pronta para persistência
     */
    public static SyncDiagnosticoEntity of(String tabela, String campo, long total, long naoNulos) {
        SyncDiagnosticoEntity e = new SyncDiagnosticoEntity();
        e.dtSync = LocalDateTime.now();
        e.tabela = tabela;
        e.campo = campo;
        e.total = total;
        e.naoNulos = naoNulos;
        e.percentual = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(naoNulos * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
        return e;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public LocalDateTime getDtSync() { return dtSync; }
    public String getTabela() { return tabela; }
    public String getCampo() { return campo; }
    public long getTotal() { return total; }
    public long getNaoNulos() { return naoNulos; }
    public BigDecimal getPercentual() { return percentual; }
}
