-- =============================================================================
-- V2__create_lookup_tables.sql
-- Tabelas de lookup (referência) para o domínio geográfico/operacional.
--
-- Estas tabelas são gerenciadas manualmente (não via ETL automático) e
-- servem de base para filtros e indicadores dos relatórios.
-- Separá-las em migração própria facilita recarregar apenas os lookups
-- sem afetar o schema principal.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabela: municipio_piloto
-- Lista os municípios participantes do projeto-piloto SSD.
-- Usada para filtrar os dados exibidos nos relatórios e dashboards.
-- Os dados são alimentados manualmente pela equipe gestora do projeto.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS municipio_piloto (
    -- ID sequencial interno; desacoplado do cod_ibge para permitir
    -- múltiplos registros históricos para o mesmo município se necessário.
    id              BIGINT          NOT NULL,

    -- Nome oficial do município
    nome            VARCHAR(100)    NOT NULL,

    -- Código IBGE de 7 dígitos — identificador único nacional
    cod_ibge        VARCHAR(7)      NOT NULL,

    -- Indica se o município está ativo no piloto no momento atual.
    -- Municípios inativos são mantidos para preservar histórico.
    piloto          BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Órgão ou entidade que demandou a inclusão do município no piloto
    demandante      VARCHAR(150),

    -- Data em que a equipe do município recebeu treinamento no sistema
    dt_treinamento  DATE,

    -- Flag de ativo/inativo para controle de ciclo de vida sem exclusão física
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_municipio_piloto    PRIMARY KEY (id),
    -- Unicidade por código IBGE para evitar duplicatas no piloto ativo
    CONSTRAINT uq_municipio_piloto_ibge UNIQUE (cod_ibge)
);

COMMENT ON TABLE  municipio_piloto              IS 'Municípios participantes do projeto-piloto SSD';
COMMENT ON COLUMN municipio_piloto.piloto       IS 'TRUE se município está no piloto ativo';
COMMENT ON COLUMN municipio_piloto.demandante   IS 'Secretaria/entidade que solicitou a inclusão';
COMMENT ON COLUMN municipio_piloto.dt_treinamento IS 'Data do treinamento da equipe municipal';
COMMENT ON COLUMN municipio_piloto.ativo        IS 'Soft-delete: FALSE = excluído logicamente';


-- -----------------------------------------------------------------------------
-- Tabela: municipio_sem_atividade
-- Registra municípios-piloto que não apresentaram nenhum registro de
-- atendimento no período analisado. Usada para destacar ausências nos
-- relatórios e acionar a equipe de suporte/implantação.
--
-- Usa cod_ibge como PK natural para simplicidade e para garantir que
-- cada município apareça no máximo uma vez nesta lista.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS municipio_sem_atividade (
    -- Código IBGE como PK natural (sem necessidade de surrogate key aqui)
    cod_ibge    VARCHAR(7)      NOT NULL,

    -- Nome do município para exibição direta sem necessidade de join
    nome        VARCHAR(100),

    CONSTRAINT pk_municipio_sem_atividade PRIMARY KEY (cod_ibge)
);

COMMENT ON TABLE  municipio_sem_atividade         IS 'Municípios-piloto sem registros de atendimento no período';
COMMENT ON COLUMN municipio_sem_atividade.cod_ibge IS 'Código IBGE de 7 dígitos (PK natural)';
