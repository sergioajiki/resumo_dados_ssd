package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import jakarta.persistence.*;

/**
 * Entidade JPA para a tabela {@code maps_off}.
 *
 * <p>Representa os dados geográficos municipais para o mapa offline.
 * Populada via ETL a partir da tabela Qlik {@code MAPS_OFF} (~79 registros).</p>
 *
 * <p>PK natural: {@code co_ibge} (código IBGE, chave natural do Qlik).
 * Estratégia de carga: truncate-reload a cada sync.</p>
 */
@Entity
@Table(name = "maps_off")
public class MapsOffEntity {

    @Id
    @Column(name = "co_ibge", length = 10)
    private String coIbge;

    @Column(name = "municipio", length = 100)
    private String municipio;

    public MapsOffEntity(String coIbge) {
        this.coIbge = coIbge;
    }

    protected MapsOffEntity() {}

    public String getCoIbge() { return coIbge; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
}
