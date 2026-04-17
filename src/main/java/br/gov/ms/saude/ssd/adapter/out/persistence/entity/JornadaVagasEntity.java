package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Entidade JPA que mapeia a tabela {@code jornada_vagas} no banco H2.
 *
 * <p>Cada instância representa a oferta de vagas de um profissional em
 * uma data específica. Usada para calcular a taxa de ocupação da agenda
 * (vagas disponíveis × atendimentos realizados).</p>
 *
 * <p>A chave primária ({@link #id}) é proveniente da fonte Qlik para
 * garantir idempotência no upsert.</p>
 */
@Entity
@Table(name = "jornada_vagas")
public class JornadaVagasEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    /** FK para {@code profissional.id} — identifica o profissional dono da jornada. */
    @Column(name = "profissional_id", nullable = false)
    private Long profissionalId;

    /** Data do atendimento (dia específico da jornada). */
    @Column(name = "dt_atendimento")
    private LocalDate dtAtendimento;

    /** Ano da jornada — extraído para facilitar agrupamento em relatórios. */
    @Column(name = "ano")
    private Integer ano;

    /** Mês da jornada — extraído para facilitar agrupamento em relatórios. */
    @Column(name = "mes")
    private Integer mes;

    /** Quantidade de vagas disponíveis nesta jornada. */
    @Column(name = "vagas")
    private Integer vagas;

    /** Construtor padrão requerido pelo JPA. */
    protected JornadaVagasEntity() {
    }

    /**
     * Construtor para criação no pipeline ETL.
     *
     * @param id identificador único proveniente da fonte Qlik
     */
    public JornadaVagasEntity(Long id) {
        this.id = id;
    }

    // -------------------------------------------------------------------------
    // Getters e setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }

    public Long getProfissionalId() { return profissionalId; }
    public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }

    public LocalDate getDtAtendimento() { return dtAtendimento; }
    public void setDtAtendimento(LocalDate dtAtendimento) { this.dtAtendimento = dtAtendimento; }

    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }

    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }

    public Integer getVagas() { return vagas; }
    public void setVagas(Integer vagas) { this.vagas = vagas; }
}
