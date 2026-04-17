package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import jakarta.persistence.*;

/**
 * Entidade JPA para a tabela {@code municipio_sem_atividade}.
 *
 * <p>Representa os municípios que estão cadastrados no sistema mas não possuem
 * atividade de Telessaúde registrada.
 * Populada via ETL a partir da tabela Qlik {@code MUN_SEMATIVIDADE} (~68 registros).</p>
 *
 * <p>A chave primária é o próprio {@code cod_ibge} (chave natural VARCHAR),
 * pois a tabela H2 não usa surrogate key.</p>
 *
 * <p>Estratégia de carga: truncate-reload a cada sync.</p>
 */
@Entity
@Table(name = "municipio_sem_atividade")
public class MunicipioSemAtividadeEntity {

    /** Código IBGE do município — chave primária natural. */
    @Id
    @Column(name = "cod_ibge", length = 7)
    private String codIbge;

    /** Nome do município. */
    @Column(name = "nome", length = 100)
    private String nome;

    /**
     * Construtor para uso no ETL — recebe o cod_ibge como identificador.
     *
     * @param codIbge código IBGE do município
     */
    public MunicipioSemAtividadeEntity(String codIbge) {
        this.codIbge = codIbge;
    }

    protected MunicipioSemAtividadeEntity() {}

    public String getCodIbge() { return codIbge; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
