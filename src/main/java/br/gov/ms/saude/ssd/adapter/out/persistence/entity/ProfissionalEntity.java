package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import jakarta.persistence.*;

/**
 * Entidade JPA que mapeia a tabela {@code profissional} no banco H2.
 *
 * <p>Representa o cadastro de profissionais de saúde extraído da tabela
 * {@code TEMPDB_USER} do Qlik Sense. O {@link #id} corresponde ao {@code ID_USER}
 * da fonte para garantir idempotência no upsert.</p>
 *
 * @see br.gov.ms.saude.ssd.adapter.out.persistence.repository.ProfissionalRepository
 */
@Entity
@Table(name = "profissional")
public class ProfissionalEntity {

    /**
     * Identificador único — corresponde ao {@code ID_USER} do Qlik.
     * Não usa {@code @GeneratedValue} pois o ID vem da fonte de dados.
     */
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    /** Nome completo do profissional de saúde. */
    @Column(name = "nome", length = 150)
    private String nome;

    /** Número do CRM (pode conter letras do estado, ex: "12345/MS"). */
    @Column(name = "crm", length = 20)
    private String crm;

    /** ID numérico da especialidade no sistema de origem. */
    @Column(name = "especialidade_id")
    private Integer especialidadeId;

    /** Nome da especialidade médica (desnormalizado para evitar join em consultas frequentes). */
    @Column(name = "especialidade_nome", length = 100)
    private String especialidadeNome;

    /** Município onde o profissional atende. */
    @Column(name = "municipio", length = 100)
    private String municipio;

    /** Código IBGE de 7 dígitos do município de atuação. */
    @Column(name = "cod_ibge", length = 7)
    private String codIbge;

    /** Tipo de usuário no sistema de origem (ex: "MEDICO", "ADMIN", "GESTOR"). */
    @Column(name = "tipo_usuario", length = 50)
    private String tipoUsuario;

    /** Faixa etária do profissional (indicador de diversidade em RH de saúde). */
    @Column(name = "faixa_etaria", length = 50)
    private String faixaEtaria;

    /** Raça/cor autodeclarada do profissional (indicador de diversidade). */
    @Column(name = "raca_cor", length = 30)
    private String racaCor;

    /** Construtor padrão requerido pelo JPA. */
    protected ProfissionalEntity() {
    }

    /**
     * Construtor de conveniência para criação a partir do ID da fonte.
     *
     * @param id identificador proveniente do Qlik (ID_USER)
     */
    public ProfissionalEntity(Long id) {
        this.id = id;
    }

    // -------------------------------------------------------------------------
    // Getters e setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCrm() { return crm; }
    public void setCrm(String crm) { this.crm = crm; }

    public Integer getEspecialidadeId() { return especialidadeId; }
    public void setEspecialidadeId(Integer especialidadeId) { this.especialidadeId = especialidadeId; }

    public String getEspecialidadeNome() { return especialidadeNome; }
    public void setEspecialidadeNome(String especialidadeNome) { this.especialidadeNome = especialidadeNome; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getCodIbge() { return codIbge; }
    public void setCodIbge(String codIbge) { this.codIbge = codIbge; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public String getFaixaEtaria() { return faixaEtaria; }
    public void setFaixaEtaria(String faixaEtaria) { this.faixaEtaria = faixaEtaria; }

    public String getRacaCor() { return racaCor; }
    public void setRacaCor(String racaCor) { this.racaCor = racaCor; }
}
