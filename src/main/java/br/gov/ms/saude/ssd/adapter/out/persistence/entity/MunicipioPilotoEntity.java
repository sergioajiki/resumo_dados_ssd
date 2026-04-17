package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entidade JPA para a tabela {@code municipio_piloto}.
 *
 * <p>Representa os municípios participantes do programa piloto de Telessaúde.
 * Populada via ETL a partir da tabela Qlik {@code MUN_PILOTO}.</p>
 *
 * <p>Estratégia de carga: truncate-reload (deleteAllInBatch + saveAll) a cada sync,
 * pois é uma tabela de lookup pequena sem referências externas.</p>
 */
@Entity
@Table(name = "municipio_piloto")
public class MunicipioPilotoEntity {

    /**
     * Chave primária auto-gerada (sem equivalente na fonte Qlik).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Nome do município conforme campo {@code MUNICIPIOCHECK} no Qlik. */
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    /**
     * Código IBGE do município.
     * Nullable: a tabela Qlik {@code MUN_PILOTO} não expõe este campo.
     */
    @Column(name = "cod_ibge", length = 7)
    private String codIbge;

    /** Indica se o município está ativo no piloto ({@code PILOTO_TF}). */
    @Column(name = "piloto", nullable = false)
    private Boolean piloto = false;

    /** Entidade demandante do município no programa ({@code DEMANDANTE}). */
    @Column(name = "demandante", length = 150)
    private String demandante;

    /** Data de treinamento do município ({@code DT_TREINAMENTO}). */
    @Column(name = "dt_treinamento")
    private LocalDate dtTreinamento;

    /** Indica se o registro está ativo. Sempre {@code true} na carga ETL. */
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    public MunicipioPilotoEntity() {}

    public Long getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodIbge() { return codIbge; }
    public void setCodIbge(String codIbge) { this.codIbge = codIbge; }

    public Boolean getPiloto() { return piloto; }
    public void setPiloto(Boolean piloto) { this.piloto = piloto != null ? piloto : false; }

    public String getDemandante() { return demandante; }
    public void setDemandante(String demandante) { this.demandante = demandante; }

    public LocalDate getDtTreinamento() { return dtTreinamento; }
    public void setDtTreinamento(LocalDate dtTreinamento) { this.dtTreinamento = dtTreinamento; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo != null ? ativo : true; }
}
