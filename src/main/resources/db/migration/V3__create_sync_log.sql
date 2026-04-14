-- =============================================================================
-- V3__create_sync_log.sql
-- Tabela de log para rastreabilidade das sincronizações ETL.
--
-- Cada execução do ETL (por tabela de destino) gera um registro aqui.
-- Permite:
--   - Saber quando foi a última sync bem-sucedida de cada tabela.
--   - Diagnosticar falhas com a mensagem de erro registrada.
--   - Calcular métricas de desempenho da extração (registros/segundo).
--   - Auditar quantos registros foram inseridos vs. atualizados.
--
-- Esta tabela é escrita apenas pela camada de infraestrutura (adaptador ETL)
-- e lida pela camada de apresentação (relatório de status da sync).
-- =============================================================================

CREATE TABLE IF NOT EXISTS sync_log (
    -- PK auto-incrementada: o banco gerencia o ID para garantir unicidade
    -- mesmo em execuções concorrentes (ex.: múltiplas tabelas em paralelo).
    id                      BIGINT          NOT NULL AUTO_INCREMENT,

    -- Nome da tabela de destino que foi sincronizada
    -- (ex.: 'atendimento', 'profissional', 'jornada_vagas')
    tabela                  VARCHAR(100)    NOT NULL,

    -- Timestamp de início da sincronização (para calcular duração)
    iniciado_em             TIMESTAMP       NOT NULL,

    -- Timestamp de conclusão (NULL enquanto a sync ainda está em andamento)
    concluido_em            TIMESTAMP,

    -- Total de registros lidos da fonte (Qlik)
    registros_extraidos     INT             DEFAULT 0,

    -- Registros inseridos pela primeira vez no banco local
    registros_novos         INT             DEFAULT 0,

    -- Registros que já existiam e foram atualizados (upsert)
    registros_atualizados   INT             DEFAULT 0,

    -- Status da sincronização:
    --   INICIADO  → em andamento
    --   CONCLUIDO → finalizado com sucesso
    --   ERRO      → falhou (ver coluna 'erro')
    --   PARCIAL   → concluído mas com advertências
    status                  VARCHAR(20)     NOT NULL DEFAULT 'INICIADO',

    -- Mensagem de erro completa em caso de falha.
    -- Tipo TEXT para acomodar stack traces longas se necessário.
    -- NULL quando status = 'CONCLUIDO'.
    erro                    TEXT,

    CONSTRAINT pk_sync_log PRIMARY KEY (id)
);

COMMENT ON TABLE  sync_log                      IS 'Histórico de execuções do ETL por tabela';
COMMENT ON COLUMN sync_log.tabela               IS 'Nome da tabela de destino sincronizada';
COMMENT ON COLUMN sync_log.iniciado_em          IS 'Início da execução do ETL';
COMMENT ON COLUMN sync_log.concluido_em         IS 'Fim da execução; NULL se ainda em andamento';
COMMENT ON COLUMN sync_log.registros_extraidos  IS 'Total de registros lidos da fonte Qlik';
COMMENT ON COLUMN sync_log.registros_novos      IS 'Registros inseridos (INSERT)';
COMMENT ON COLUMN sync_log.registros_atualizados IS 'Registros atualizados (UPDATE/upsert)';
COMMENT ON COLUMN sync_log.status               IS 'INICIADO | CONCLUIDO | ERRO | PARCIAL';
COMMENT ON COLUMN sync_log.erro                 IS 'Stack trace ou mensagem de erro quando status=ERRO';

-- Índice para consultas de status recente (ex.: "última sync de cada tabela")
CREATE INDEX IF NOT EXISTS idx_sync_log_tabela_iniciado
    ON sync_log (tabela, iniciado_em DESC);
