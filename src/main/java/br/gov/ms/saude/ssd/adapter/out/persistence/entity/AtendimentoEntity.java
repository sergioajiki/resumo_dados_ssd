package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidade JPA que mapeia a tabela {@code atendimento} no banco H2.
 *
 * <p>Cada instância representa um atendimento/consulta extraído do Qlik Sense.
 * A chave primária ({@link #id}) corresponde ao {@code ID_ATENDIMENTO} da fonte
 * para garantir idempotência no upsert.</p>
 *
 * <p>Os campos de data são convertidos pelo {@code FieldTransformerService}
 * durante a carga ETL: strings vindo do Qlik são convertidas para
 * {@link LocalDate}/{@link LocalDateTime} antes da persistência.</p>
 *
 * @see br.gov.ms.saude.ssd.adapter.out.persistence.repository.AtendimentoRepository
 */
@Entity
@Table(name = "atendimento")
public class AtendimentoEntity {

    /**
     * Identificador único. Usa o ID da fonte Qlik para controle de idempotência.
     * Não usa {@code @GeneratedValue} pois o ID vem da fonte de dados.
     */
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    /** Cartão Nacional de Saúde do paciente (15 dígitos). */
    @Column(name = "cns_paciente", length = 20)
    private String cnsPaciente;

    /** Data de nascimento do paciente. Convertida de string pelo transformer ETL. */
    @Column(name = "dt_nascimento")
    private LocalDate dtNascimento;

    /**
     * Faixa etária pré-calculada pelo {@code AgeCalculator} durante a transformação ETL.
     * Exemplos: "0-17", "18-29", "30-59", "60+".
     */
    @Column(name = "faixa_etaria", length = 50)
    private String faixaEtaria;

    /** Raça/cor autodeclarada conforme classificação IBGE. */
    @Column(name = "raca", length = 30)
    private String raca;

    /** Etnia (aplicável a populações indígenas). */
    @Column(name = "etnia", length = 80)
    private String etnia;

    /** Nome do município de residência do paciente. */
    @Column(name = "municipio", length = 100)
    private String municipio;

    /** Código IBGE de 7 dígitos do município. */
    @Column(name = "ibge", length = 7)
    private String ibge;

    /** Data e hora do agendamento da consulta. */
    @Column(name = "dt_agendamento")
    private LocalDateTime dtAgendamento;

    /**
     * Horário do agendamento. Convertido do campo numérico {@code HR_AGENDAMENTO}
     * do Qlik pelo {@code TimeConverter} no pipeline ETL.
     */
    @Column(name = "hr_agendamento")
    private LocalTime hrAgendamento;

    /** Nome completo do profissional de saúde responsável. */
    @Column(name = "nome_medico", length = 150)
    private String nomeMedico;

    /** Código CBO (Classificação Brasileira de Ocupações) do profissional. */
    @Column(name = "cbo_medico", length = 10)
    private String cboMedico;

    /** Especialidade médica (ex: "CARDIOLOGIA"). Derivada do CBO ou preenchida diretamente. */
    @Column(name = "especialidade", length = 100)
    private String especialidade;

    /** Status da consulta (ex: "AGENDADO", "REALIZADO", "CANCELADO"). */
    @Column(name = "status_consulta", length = 50)
    private String statusConsulta;

    /** Desfecho clínico registrado pelo profissional ao final do atendimento. */
    @Column(name = "desfecho", length = 4000)
    private String desfecho;

    /** Código CID-10 do diagnóstico principal. */
    @Column(name = "cid", length = 20)
    private String cid;

    /** Data e hora em que a solicitação foi gerada no sistema de origem. */
    @Column(name = "dt_solicitacao")
    private LocalDateTime dtSolicitacao;

    /** Classificação de zona geográfica do paciente (ex: "URBANA", "RURAL"). */
    @Column(name = "tipo_zona", length = 20)
    private String tipoZona;

    /**
     * Corresponde ao campo {@code DT_NEW} da fonte. Usado como watermark
     * para extração incremental — apenas registros com {@code DT_NEW}
     * posterior à última sync são extraídos.
     */
    @Column(name = "dt_criacao")
    private LocalDateTime dtCriacao;

    /** Timestamp da última vez que este registro foi sincronizado. Gerenciado pelo banco. */
    @Column(name = "sincronizado_em", insertable = false, updatable = false)
    private LocalDateTime sincronizadoEm;

    /** Construtor padrão requerido pelo JPA. */
    protected AtendimentoEntity() {
    }

    /**
     * Construtor completo para criação de entidades no pipeline ETL.
     * Todos os parâmetros aceitam {@code null} exceto {@code id}.
     *
     * @param id identificador único (proveniente da fonte Qlik)
     */
    public AtendimentoEntity(Long id) {
        this.id = id;
    }

    // -------------------------------------------------------------------------
    // Getters e setters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCnsPaciente() { return cnsPaciente; }
    public void setCnsPaciente(String cnsPaciente) { this.cnsPaciente = cnsPaciente; }

    public LocalDate getDtNascimento() { return dtNascimento; }
    public void setDtNascimento(LocalDate dtNascimento) { this.dtNascimento = dtNascimento; }

    public String getFaixaEtaria() { return faixaEtaria; }
    public void setFaixaEtaria(String faixaEtaria) { this.faixaEtaria = faixaEtaria; }

    public String getRaca() { return raca; }
    public void setRaca(String raca) { this.raca = raca; }

    public String getEtnia() { return etnia; }
    public void setEtnia(String etnia) { this.etnia = etnia; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getIbge() { return ibge; }
    public void setIbge(String ibge) { this.ibge = ibge; }

    public LocalDateTime getDtAgendamento() { return dtAgendamento; }
    public void setDtAgendamento(LocalDateTime dtAgendamento) { this.dtAgendamento = dtAgendamento; }

    public LocalTime getHrAgendamento() { return hrAgendamento; }
    public void setHrAgendamento(LocalTime hrAgendamento) { this.hrAgendamento = hrAgendamento; }

    public String getNomeMedico() { return nomeMedico; }
    public void setNomeMedico(String nomeMedico) { this.nomeMedico = nomeMedico; }

    public String getCboMedico() { return cboMedico; }
    public void setCboMedico(String cboMedico) { this.cboMedico = cboMedico; }

    public String getEspecialidade() { return especialidade; }
    public void setEspecialidade(String especialidade) { this.especialidade = especialidade; }

    public String getStatusConsulta() { return statusConsulta; }
    public void setStatusConsulta(String statusConsulta) { this.statusConsulta = statusConsulta; }

    public String getDesfecho() { return desfecho; }
    public void setDesfecho(String desfecho) { this.desfecho = desfecho; }

    public String getCid() { return cid; }
    public void setCid(String cid) { this.cid = cid; }

    public LocalDateTime getDtSolicitacao() { return dtSolicitacao; }
    public void setDtSolicitacao(LocalDateTime dtSolicitacao) { this.dtSolicitacao = dtSolicitacao; }

    public String getTipoZona() { return tipoZona; }
    public void setTipoZona(String tipoZona) { this.tipoZona = tipoZona; }

    public LocalDateTime getDtCriacao() { return dtCriacao; }
    public void setDtCriacao(LocalDateTime dtCriacao) { this.dtCriacao = dtCriacao; }

    public LocalDateTime getSincronizadoEm() { return sincronizadoEm; }
}
