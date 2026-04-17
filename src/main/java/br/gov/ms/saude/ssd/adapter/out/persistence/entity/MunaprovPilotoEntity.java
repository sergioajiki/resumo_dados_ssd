package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import jakarta.persistence.*;

/**
 * Entidade JPA para a tabela {@code munaprov_piloto}.
 *
 * <p>Representa os municípios aprovados no programa piloto de Telessaúde.
 * Populada via ETL a partir da tabela Qlik {@code MUNAPROV_PILOTO} (~207 registros).</p>
 *
 * <p>PK auto-gerada pois o Qlik não expõe ID numérico para esta tabela.
 * Estratégia de carga: truncate-reload a cada sync.</p>
 */
@Entity
@Table(name = "munaprov_piloto")
public class MunaprovPilotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "municipio", nullable = false, length = 100)
    private String municipio;

    @Column(name = "cnes_indicado", length = 20)
    private String cnesIndicado;

    public MunaprovPilotoEntity() {}

    public Long getId() { return id; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getCnesIndicado() { return cnesIndicado; }
    public void setCnesIndicado(String cnesIndicado) { this.cnesIndicado = cnesIndicado; }
}
