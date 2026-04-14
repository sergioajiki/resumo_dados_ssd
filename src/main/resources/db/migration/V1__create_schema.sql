-- =============================================================================
-- V1__create_schema.sql
-- Migração inicial: cria as tabelas do domínio principal.
--
-- Convenções adotadas:
--   - Nomes em snake_case, minúsculos.
--   - Colunas de auditoria (dt_criacao, sincronizado_em) em todas as tabelas
--     que recebem dados do Qlik para rastreabilidade da sincronização ETL.
--   - AUTO_INCREMENT com BIGINT para suportar volumes futuros sem reindexação.
--   - Campos TEXT para valores que podem ser nulos ou de tamanho variável
--     (cid, desfecho) evitam truncamento silencioso.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabela: atendimento
-- Representa cada consulta/atendimento registrado no Qlik Sense.
-- É a tabela principal do domínio e concentra o maior volume de dados.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS atendimento (
    -- Chave primária: corresponde ao ID do registro no Qlik.
    -- Usar o mesmo ID da fonte facilita o controle de idempotência na sincronização.
    id                  BIGINT          NOT NULL,

    -- Cartão Nacional de Saúde do paciente. Não é CPF — pode ser compartilhado
    -- entre membros da mesma família em casos de cadastros antigos.
    cns_paciente        VARCHAR(20),

    -- Data de nascimento para cálculo de indicadores por faixa etária
    dt_nascimento       DATE,

    -- Faixa etária pré-calculada pela fonte (ex.: "20-29 anos").
    -- Armazenada como string porque o critério de classificação é da fonte.
    faixa_etaria        VARCHAR(50),

    -- Raça/cor conforme classificação IBGE (autodeclarada)
    raca                VARCHAR(30),

    -- Etnia (para população indígena)
    etnia               VARCHAR(80),

    -- Nome do município do paciente (desnormalizado para performance em relatórios)
    municipio           VARCHAR(100),

    -- Código IBGE do município (7 dígitos).
    -- Usado para join com tabelas de lookup de municípios-piloto.
    ibge                VARCHAR(7),

    -- Data e hora do agendamento da consulta
    dt_agendamento      TIMESTAMP,

    -- Horário do agendamento (separado para facilitar agrupamentos por turno)
    hr_agendamento      TIME,

    -- Nome completo do profissional de saúde
    nome_medico         VARCHAR(150),

    -- Código Brasileiro de Ocupações do profissional
    cbo_medico          VARCHAR(10),

    -- Especialidade médica (ex.: "CARDIOLOGIA", "PEDIATRIA")
    especialidade       VARCHAR(100),

    -- Status atual da consulta (ex.: "AGENDADO", "REALIZADO", "CANCELADO")
    status_consulta     VARCHAR(50),

    -- Desfecho clínico registrado pelo profissional
    desfecho            TEXT,

    -- CID-10: código internacional de doenças.
    -- Pode ter múltiplos códigos separados por vírgula em alguns registros.
    cid                 VARCHAR(20),

    -- Data/hora em que a solicitação foi gerada no sistema de origem
    dt_solicitacao      TIMESTAMP,

    -- Tipo de zona geográfica do paciente (ex.: "URBANA", "RURAL")
    tipo_zona           VARCHAR(20),

    -- Data de criação do registro na fonte (usado como watermark na sync incremental)
    dt_criacao          TIMESTAMP,

    -- Data/hora em que este registro foi sincronizado pela última vez.
    -- Preenchido automaticamente pelo banco; atualizado a cada sync.
    sincronizado_em     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_atendimento PRIMARY KEY (id)
);

COMMENT ON TABLE  atendimento                IS 'Atendimentos/consultas extraídos do Qlik Sense via ETL';
COMMENT ON COLUMN atendimento.cns_paciente   IS 'Cartão Nacional de Saúde (15 dígitos)';
COMMENT ON COLUMN atendimento.ibge           IS 'Código IBGE do município (7 dígitos)';
COMMENT ON COLUMN atendimento.cid            IS 'CID-10 registrado no atendimento';
COMMENT ON COLUMN atendimento.dt_criacao     IS 'Campo DT_NEW da fonte — usado como watermark ETL';
COMMENT ON COLUMN atendimento.sincronizado_em IS 'Timestamp da última sincronização ETL';


-- -----------------------------------------------------------------------------
-- Tabela: profissional
-- Cadastro de profissionais de saúde extraído do Qlik.
-- Relacionada com jornada_vagas para controle de agenda.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS profissional (
    -- ID do profissional conforme cadastro na fonte
    id                  BIGINT          NOT NULL,

    -- Nome completo do profissional
    nome                VARCHAR(150),

    -- Número do CRM (pode conter letras do estado, ex.: "12345/MS")
    crm                 VARCHAR(20),

    -- ID numérico da especialidade (código interno da fonte)
    especialidade_id    INT,

    -- Nome da especialidade (desnormalizado para evitar join em consultas frequentes)
    especialidade_nome  VARCHAR(100),

    -- Município onde o profissional atende
    municipio           VARCHAR(100),

    -- Código IBGE do município de atuação
    cod_ibge            VARCHAR(7),

    -- Tipo de usuário no sistema de origem (ex.: "MEDICO", "ADMIN")
    tipo_usuario        VARCHAR(50),

    -- Faixa etária do profissional (para indicadores de RH em saúde)
    faixa_etaria        VARCHAR(50),

    -- Raça/cor autodeclarada (indicador de diversidade)
    raca_cor            VARCHAR(30),

    CONSTRAINT pk_profissional PRIMARY KEY (id)
);

COMMENT ON TABLE  profissional               IS 'Cadastro de profissionais de saúde extraído do Qlik';
COMMENT ON COLUMN profissional.cod_ibge      IS 'Código IBGE do município de atuação (7 dígitos)';
COMMENT ON COLUMN profissional.tipo_usuario  IS 'Perfil de acesso do profissional na fonte';


-- -----------------------------------------------------------------------------
-- Tabela: jornada_vagas
-- Registra a oferta de vagas por profissional, data e período.
-- Usada para calcular a taxa de ocupação da agenda (vagas x atendimentos).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS jornada_vagas (
    id                  BIGINT          NOT NULL,

    -- Referência ao profissional dono da jornada
    profissional_id     BIGINT          NOT NULL,

    -- Data do atendimento (permite filtrar por dia específico)
    dt_atendimento      DATE,

    -- Ano e mês separados para agrupar facilmente em relatórios mensais/anuais
    -- sem precisar de funções de data (melhora performance em H2)
    ano                 INT,
    mes                 INT,

    -- Quantidade de vagas disponíveis nesta jornada
    vagas               INT,

    CONSTRAINT pk_jornada_vagas        PRIMARY KEY (id),
    CONSTRAINT fk_jornada_profissional FOREIGN KEY (profissional_id)
        REFERENCES profissional (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

COMMENT ON TABLE  jornada_vagas                IS 'Oferta de vagas por profissional e período';
COMMENT ON COLUMN jornada_vagas.profissional_id IS 'FK para profissional.id';
COMMENT ON COLUMN jornada_vagas.vagas           IS 'Número de vagas disponíveis na jornada';
