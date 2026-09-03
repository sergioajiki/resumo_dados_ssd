package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "atendimento_norm")
public class AtendimentoNormEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "cns_paciente", length = 50)
    private String cnsPaciente;

    @Column(name = "dt_nascimento")
    private LocalDate dtNascimento;

    @Column(name = "raca", length = 30)
    private String raca;

    @Column(name = "etnia", length = 255)
    private String etnia;

    @Column(name = "municipio", length = 200)
    private String municipio;

    @Column(name = "cod_ibge", length = 7)
    private String codIbge;

    @Column(name = "tipo_zona", length = 20)
    private String tipoZona;

    @Column(name = "telefone", length = 30)
    private String telefone;

    @Column(name = "cep_paciente", length = 10)
    private String cepPaciente;

    @Column(name = "rua_paciente", length = 200)
    private String ruaPaciente;

    @Column(name = "num_end_paciente", length = 20)
    private String numEndPaciente;

    @Column(name = "bairro_paciente", length = 150)
    private String bairroPaciente;

    @Column(name = "complemento_end_paciente", length = 150)
    private String complementoEndPaciente;

    @Column(name = "descricao_endereco_paciente", length = 300)
    private String descricaoEnderecoPaciente;

    @Column(name = "dt_agendamento")
    private LocalDateTime dtAgendamento;

    @Column(name = "hr_agendamento")
    private LocalTime hrAgendamento;

    @Column(name = "dt_solicitacao")
    private LocalDateTime dtSolicitacao;

    @Column(name = "status_consulta", length = 200)
    private String statusConsulta;

    @Column(name = "classif_conclusao", length = 100)
    private String classifConclusao;

    @Column(name = "tipo_servico", length = 50)
    private String tipoServico;

    @Column(name = "desfecho", columnDefinition = "TEXT")
    private String desfecho;

    @Column(name = "cid", length = 100)
    private String cid;

    @Column(name = "nome_medico", length = 500)
    private String nomeMedico;

    @Column(name = "especialidade", length = 100)
    private String especialidade;

    @Column(name = "cbo_medico", length = 10)
    private String cboMedico;

    @Column(name = "id_medico", length = 100)
    private String idMedico;

    @Column(name = "cns_profissional", length = 50)
    private String cnsProfissional;

    @Column(name = "cnes_estabelecimento", length = 20)
    private String cnesEstabelecimento;

    @Column(name = "nome_estabelecimento", length = 200)
    private String nomeEstabelecimento;

    @Column(name = "id_estabelecimento", length = 200)
    private String idEstabelecimento;

    @Column(name = "id_digsaude_ref", length = 50)
    private String idDigsaudeRef;

    protected AtendimentoNormEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCnsPaciente() { return cnsPaciente; }
    public void setCnsPaciente(String cnsPaciente) { this.cnsPaciente = cnsPaciente; }

    public LocalDate getDtNascimento() { return dtNascimento; }
    public void setDtNascimento(LocalDate dtNascimento) { this.dtNascimento = dtNascimento; }

    public String getRaca() { return raca; }
    public void setRaca(String raca) { this.raca = raca; }

    public String getEtnia() { return etnia; }
    public void setEtnia(String etnia) { this.etnia = etnia; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getCodIbge() { return codIbge; }
    public void setCodIbge(String codIbge) { this.codIbge = codIbge; }

    public String getTipoZona() { return tipoZona; }
    public void setTipoZona(String tipoZona) { this.tipoZona = tipoZona; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCepPaciente() { return cepPaciente; }
    public void setCepPaciente(String cepPaciente) { this.cepPaciente = cepPaciente; }

    public String getRuaPaciente() { return ruaPaciente; }
    public void setRuaPaciente(String ruaPaciente) { this.ruaPaciente = ruaPaciente; }

    public String getNumEndPaciente() { return numEndPaciente; }
    public void setNumEndPaciente(String numEndPaciente) { this.numEndPaciente = numEndPaciente; }

    public String getBairroPaciente() { return bairroPaciente; }
    public void setBairroPaciente(String bairroPaciente) { this.bairroPaciente = bairroPaciente; }

    public String getComplementoEndPaciente() { return complementoEndPaciente; }
    public void setComplementoEndPaciente(String complementoEndPaciente) { this.complementoEndPaciente = complementoEndPaciente; }

    public String getDescricaoEnderecoPaciente() { return descricaoEnderecoPaciente; }
    public void setDescricaoEnderecoPaciente(String descricaoEnderecoPaciente) { this.descricaoEnderecoPaciente = descricaoEnderecoPaciente; }

    public LocalDateTime getDtAgendamento() { return dtAgendamento; }
    public void setDtAgendamento(LocalDateTime dtAgendamento) { this.dtAgendamento = dtAgendamento; }

    public LocalTime getHrAgendamento() { return hrAgendamento; }
    public void setHrAgendamento(LocalTime hrAgendamento) { this.hrAgendamento = hrAgendamento; }

    public LocalDateTime getDtSolicitacao() { return dtSolicitacao; }
    public void setDtSolicitacao(LocalDateTime dtSolicitacao) { this.dtSolicitacao = dtSolicitacao; }

    public String getStatusConsulta() { return statusConsulta; }
    public void setStatusConsulta(String statusConsulta) { this.statusConsulta = statusConsulta; }

    public String getClassifConclusao() { return classifConclusao; }
    public void setClassifConclusao(String classifConclusao) { this.classifConclusao = classifConclusao; }

    public String getTipoServico() { return tipoServico; }
    public void setTipoServico(String tipoServico) { this.tipoServico = tipoServico; }

    public String getDesfecho() { return desfecho; }
    public void setDesfecho(String desfecho) { this.desfecho = desfecho; }

    public String getCid() { return cid; }
    public void setCid(String cid) { this.cid = cid; }

    public String getNomeMedico() { return nomeMedico; }
    public void setNomeMedico(String nomeMedico) { this.nomeMedico = nomeMedico; }

    public String getEspecialidade() { return especialidade; }
    public void setEspecialidade(String especialidade) { this.especialidade = especialidade; }

    public String getCboMedico() { return cboMedico; }
    public void setCboMedico(String cboMedico) { this.cboMedico = cboMedico; }

    public String getIdMedico() { return idMedico; }
    public void setIdMedico(String idMedico) { this.idMedico = idMedico; }

    public String getCnsProfissional() { return cnsProfissional; }
    public void setCnsProfissional(String cnsProfissional) { this.cnsProfissional = cnsProfissional; }

    public String getCnesEstabelecimento() { return cnesEstabelecimento; }
    public void setCnesEstabelecimento(String cnesEstabelecimento) { this.cnesEstabelecimento = cnesEstabelecimento; }

    public String getNomeEstabelecimento() { return nomeEstabelecimento; }
    public void setNomeEstabelecimento(String nomeEstabelecimento) { this.nomeEstabelecimento = nomeEstabelecimento; }

    public String getIdEstabelecimento() { return idEstabelecimento; }
    public void setIdEstabelecimento(String idEstabelecimento) { this.idEstabelecimento = idEstabelecimento; }

    public String getIdDigsaudeRef() { return idDigsaudeRef; }
    public void setIdDigsaudeRef(String idDigsaudeRef) { this.idDigsaudeRef = idDigsaudeRef; }
}
