package br.gov.ms.saude.ssd.adapter.out.persistence.entity;

import jakarta.persistence.*;

/**
 * Entidade JPA para a tabela {@code link}.
 *
 * <p>Representa a ligação entre atendimentos, profissionais e jornadas no Qlik.
 * Populada via ETL a partir da tabela Qlik {@code LINK} (~22.325 registros).</p>
 *
 * <p>PK natural: {@code chave} mapeada a partir do campo Qlik {@code ID_DIGSAUDE_REF}
 * (chave composta gerada pelo Qlik, ex: {@code 14_fev_2025_DEODÁPOLIS}).
 * O campo Qlik {@code CHAVE} retorna sempre nulo na extração e não é usado.
 * Estratégia de carga: truncate-reload a cada sync.</p>
 */
@Entity
@Table(name = "link")
public class LinkEntity {

    @Id
    @Column(name = "chave", length = 100)
    private String chave;

    @Column(name = "municipio", length = 100)
    private String municipio;

    @Column(name = "id_digsaude_ref", length = 100)
    private String idDigsaudeRef;

    @Column(name = "id_jornd_ref", length = 100)
    private String idJorndRef;

    @Column(name = "id_user_ref")
    private Long idUserRef;

    @Column(name = "ano")
    private Integer ano;

    @Column(name = "mes", length = 10)
    private String mes;

    @Column(name = "mes_ano", length = 20)
    private String mesAno;

    public LinkEntity(String chave) {
        this.chave = chave;
    }

    protected LinkEntity() {}

    public String getChave() { return chave; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getIdDigsaudeRef() { return idDigsaudeRef; }
    public void setIdDigsaudeRef(String idDigsaudeRef) { this.idDigsaudeRef = idDigsaudeRef; }

    public String getIdJorndRef() { return idJorndRef; }
    public void setIdJorndRef(String idJorndRef) { this.idJorndRef = idJorndRef; }

    public Long getIdUserRef() { return idUserRef; }
    public void setIdUserRef(Long idUserRef) { this.idUserRef = idUserRef; }

    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }

    public String getMes() { return mes; }
    public void setMes(String mes) { this.mes = mes; }

    public String getMesAno() { return mesAno; }
    public void setMesAno(String mesAno) { this.mesAno = mesAno; }
}
