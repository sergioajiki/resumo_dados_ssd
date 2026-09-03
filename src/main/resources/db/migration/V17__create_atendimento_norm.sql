CREATE TABLE atendimento_norm (
    id                          BIGINT PRIMARY KEY,
    -- Paciente
    cns_paciente                VARCHAR(50),
    dt_nascimento               DATE,
    raca                        VARCHAR(30),
    etnia                       VARCHAR(255),
    municipio                   VARCHAR(200),
    cod_ibge                    VARCHAR(7),
    tipo_zona                   VARCHAR(20),
    -- Endereço
    telefone                    VARCHAR(30),
    cep_paciente                VARCHAR(10),
    rua_paciente                VARCHAR(200),
    num_end_paciente            VARCHAR(20),
    bairro_paciente             VARCHAR(150),
    complemento_end_paciente    VARCHAR(150),
    descricao_endereco_paciente VARCHAR(300),
    -- Agendamento / Consulta
    dt_agendamento              TIMESTAMP,
    hr_agendamento              TIME,
    dt_solicitacao              TIMESTAMP,
    status_consulta             VARCHAR(200),
    classif_conclusao           VARCHAR(100),
    tipo_servico                VARCHAR(50),
    desfecho                    TEXT,
    cid                         VARCHAR(100),
    -- Profissional
    nome_medico                 VARCHAR(500),
    especialidade               VARCHAR(100),
    cbo_medico                  VARCHAR(10),
    id_medico                   VARCHAR(100),
    cns_profissional            VARCHAR(50),
    -- Estabelecimento
    cnes_estabelecimento        VARCHAR(20),
    nome_estabelecimento        VARCHAR(200),
    id_estabelecimento          VARCHAR(200),
    id_digsaude_ref             VARCHAR(50)
);

INSERT INTO atendimento_norm (
    id,
    cns_paciente,
    dt_nascimento,
    raca,
    etnia,
    municipio,
    cod_ibge,
    tipo_zona,
    telefone,
    cep_paciente,
    rua_paciente,
    num_end_paciente,
    bairro_paciente,
    complemento_end_paciente,
    descricao_endereco_paciente,
    dt_agendamento,
    hr_agendamento,
    dt_solicitacao,
    status_consulta,
    classif_conclusao,
    tipo_servico,
    desfecho,
    cid,
    nome_medico,
    especialidade,
    cbo_medico,
    id_medico,
    cns_profissional,
    cnes_estabelecimento,
    nome_estabelecimento,
    id_estabelecimento,
    id_digsaude_ref
)
SELECT
    id,
    REPLACE(cns_paciente, ' ', ''),
    dt_nascimento,
    raca,
    etnia,
    municipio,
    ibge,
    tipo_zona,
    telefone,
    REPLACE(REPLACE(cep_paciente, '-', ''), ' ', ''),
    rua_paciente,
    num_paciente,
    bairro_paciente,
    complemento_end,
    descricao_endereco,
    dt_agendamento,
    hr_agendamento,
    dt_solicitacao,
    status_consulta,
    classif_conclusao,
    tipo_servico,
    desfecho,
    cid,
    CASE
        WHEN nome_medico IS NULL OR LOCATE(' - ', nome_medico) = 0 THEN nome_medico
        ELSE TRIM(SUBSTRING(nome_medico, LOCATE(' - ', nome_medico) + 3))
    END,
    CASE
        WHEN nome_medico IS NULL OR LOCATE(' - ', nome_medico) = 0 THEN NULL
        ELSE TRIM(SUBSTRING(nome_medico, 1, LOCATE(' - ', nome_medico) - 1))
    END,
    cbo_medico,
    id_medico,
    NULL,
    CASE
        WHEN cnes_estabelecimento IS NULL OR LOCATE(' ', cnes_estabelecimento) = 0 THEN cnes_estabelecimento
        ELSE TRIM(SUBSTRING(cnes_estabelecimento, 1, LOCATE(' ', cnes_estabelecimento) - 1))
    END,
    CASE
        WHEN cnes_estabelecimento IS NULL OR LOCATE(' ', cnes_estabelecimento) = 0 THEN NULL
        ELSE TRIM(SUBSTRING(cnes_estabelecimento, LOCATE(' ', cnes_estabelecimento) + 1))
    END,
    id_estabelecimento,
    id_digsaude_ref
FROM atendimento;

CREATE INDEX idx_atendimento_norm_dt_agendamento ON atendimento_norm (dt_agendamento);
CREATE INDEX idx_atendimento_norm_cns_paciente    ON atendimento_norm (cns_paciente);
CREATE INDEX idx_atendimento_norm_especialidade   ON atendimento_norm (especialidade);
CREATE INDEX idx_atendimento_norm_municipio       ON atendimento_norm (municipio);
