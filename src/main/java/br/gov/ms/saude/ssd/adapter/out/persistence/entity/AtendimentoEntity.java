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
    @Column(name = "cns_paciente", length = 50)
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
    @Column(name = "etnia", length = 255)
    private String etnia;

    /** Nome do município de residência do paciente. */
    @Column(name = "municipio", length = 200)
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
    @Column(name = "nome_medico", length = 500)
    private String nomeMedico;

    /** Código CBO (Classificação Brasileira de Ocupações) do profissional. */
    @Column(name = "cbo_medico", length = 10)
    private String cboMedico;

    /** Especialidade médica (ex: "CARDIOLOGIA"). Derivada do CBO ou preenchida diretamente. */
    @Column(name = "especialidade", length = 100)
    private String especialidade;

    /** Status da consulta (ex: "AGENDADO", "REALIZADO", "CANCELADO"). */
    @Column(name = "status_consulta", length = 200)
    private String statusConsulta;

    /** Classificação da conclusão do atendimento (ex: "Com atendimento", "Sem atendimento"). */
    @Column(name = "classif_conclusao", length = 100)
    private String classifConclusao;

    /** Desfecho clínico registrado pelo profissional ao final do atendimento. */
    @Column(name = "desfecho", length = 4000)
    private String desfecho;

    /** Código CID-10 do diagnóstico principal. */
    @Column(name = "cid", length = 100)
    private String cid;

    /** Data e hora em que a solicitação foi gerada no sistema de origem. */
    @Column(name = "dt_solicitacao")
    private LocalDateTime dtSolicitacao;

    /** Classificação de zona geográfica do paciente (ex: "URBANA", "RURAL"). */
    @Column(name = "tipo_zona", length = 20)
    private String tipoZona;

    /** Modalidade do atendimento (ex: "Teleconsulta", "Teleinterconsulta"). */
    @Column(name = "tipo_servico", length = 50)
    private String tipoServico;

    /** Código CNES + nome do estabelecimento (ex: "9318348 - UBSF SEBASTIANA DE BRITO PASCOAL"). */
    @Column(name = "cnes_estabelecimento", length = 200)
    private String cnesEstabelecimento;

    /** Identificador do estabelecimento na fonte. */
    @Column(name = "id_estabelecimento", length = 200)
    private String idEstabelecimento;

    /** Identificador do médico/profissional na fonte. */
    @Column(name = "id_medico", length = 100)
    private String idMedico;

    /** Segunda classificação de raça/cor do paciente. */
    @Column(name = "classif_cor", length = 50)
    private String classificCor;

    /** Tipo de conclusão (campo TP_NW_CONCLUSAO da fonte). */
    @Column(name = "tp_nw_conclusao", length = 50)
    private String tpNwConclusao;

    /** Identificador de referência do registro no DigSaúde. */
    @Column(name = "id_digsaude_ref", length = 50)
    private String idDigsaudeRef;

    /** Telefone de contato do paciente. */
    @Column(name = "telefone", length = 30)
    private String telefone;

    /** CEP do endereço do paciente. */
    @Column(name = "cep_paciente", length = 10)
    private String cepPaciente;

    /** Logradouro do endereço do paciente. */
    @Column(name = "rua_paciente", length = 200)
    private String ruaPaciente;

    /** Número do endereço do paciente. */
    @Column(name = "num_paciente", length = 20)
    private String numPaciente;

    /** Bairro do endereço do paciente. */
    @Column(name = "bairro_paciente", length = 150)
    private String bairroPaciente;

    /** Complemento do endereço do paciente. */
    @Column(name = "complemento_end", length = 150)
    private String complementoEnd;

    /** Descrição textual do endereço. */
    @Column(name = "descricao_endereco", length = 300)
    private String descricaoEndereco;

    /** Endereço completo formatado. */
    @Column(name = "endereco_completo", length = 500)
    private String enderecoCompleto;

    /** Notas clínicas da consulta (campo DESCRICAO_CONSULTA — 350+ chars). */
    @Column(name = "descricao_consulta", columnDefinition = "TEXT")
    private String descricaoConsulta;

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

    public String getClassifConclusao() { return classifConclusao; }
    public void setClassifConclusao(String classifConclusao) { this.classifConclusao = classifConclusao; }

    public String getDesfecho() { return desfecho; }
    public void setDesfecho(String desfecho) { this.desfecho = desfecho; }

    public String getCid() { return cid; }
    public void setCid(String cid) { this.cid = cid; }

    public LocalDateTime getDtSolicitacao() { return dtSolicitacao; }
    public void setDtSolicitacao(LocalDateTime dtSolicitacao) { this.dtSolicitacao = dtSolicitacao; }

    public String getTipoZona() { return tipoZona; }
    public void setTipoZona(String tipoZona) { this.tipoZona = tipoZona; }

    public String getTipoServico() { return tipoServico; }
    public void setTipoServico(String tipoServico) { this.tipoServico = tipoServico; }

    public String getCnesEstabelecimento() { return cnesEstabelecimento; }
    public void setCnesEstabelecimento(String cnesEstabelecimento) { this.cnesEstabelecimento = cnesEstabelecimento; }

    public String getIdEstabelecimento() { return idEstabelecimento; }
    public void setIdEstabelecimento(String idEstabelecimento) { this.idEstabelecimento = idEstabelecimento; }

    public String getIdMedico() { return idMedico; }
    public void setIdMedico(String idMedico) { this.idMedico = idMedico; }

    public String getClassificCor() { return classificCor; }
    public void setClassificCor(String classificCor) { this.classificCor = classificCor; }

    public String getTpNwConclusao() { return tpNwConclusao; }
    public void setTpNwConclusao(String tpNwConclusao) { this.tpNwConclusao = tpNwConclusao; }

    public String getIdDigsaudeRef() { return idDigsaudeRef; }
    public void setIdDigsaudeRef(String idDigsaudeRef) { this.idDigsaudeRef = idDigsaudeRef; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCepPaciente() { return cepPaciente; }
    public void setCepPaciente(String cepPaciente) { this.cepPaciente = cepPaciente; }

    public String getRuaPaciente() { return ruaPaciente; }
    public void setRuaPaciente(String ruaPaciente) { this.ruaPaciente = ruaPaciente; }

    public String getNumPaciente() { return numPaciente; }
    public void setNumPaciente(String numPaciente) { this.numPaciente = numPaciente; }

    public String getBairroPaciente() { return bairroPaciente; }
    public void setBairroPaciente(String bairroPaciente) { this.bairroPaciente = bairroPaciente; }

    public String getComplementoEnd() { return complementoEnd; }
    public void setComplementoEnd(String complementoEnd) { this.complementoEnd = complementoEnd; }

    public String getDescricaoEndereco() { return descricaoEndereco; }
    public void setDescricaoEndereco(String descricaoEndereco) { this.descricaoEndereco = descricaoEndereco; }

    public String getEnderecoCompleto() { return enderecoCompleto; }
    public void setEnderecoCompleto(String enderecoCompleto) { this.enderecoCompleto = enderecoCompleto; }

    public String getDescricaoConsulta() { return descricaoConsulta; }
    public void setDescricaoConsulta(String descricaoConsulta) { this.descricaoConsulta = descricaoConsulta; }

    public LocalDateTime getDtCriacao() { return dtCriacao; }
    public void setDtCriacao(LocalDateTime dtCriacao) { this.dtCriacao = dtCriacao; }

    public LocalDateTime getSincronizadoEm() { return sincronizadoEm; }
}
