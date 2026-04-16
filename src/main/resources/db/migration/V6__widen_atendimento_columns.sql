-- =============================================================================
-- V6 — Amplia colunas de atendimento propensas a overflow com dados reais do Qlik
--
-- Campos identificados como potencialmente curtos para dados de saúde reais:
--   - etnia: nomes de etnias indígenas podem exceder 80 chars
--   - cid: pode conter múltiplos CIDs separados por vírgula
--   - status_consulta: descrições de status podem ser longas
--   - municipio: nomes de municípios com complementos
-- =============================================================================

ALTER TABLE atendimento ALTER COLUMN etnia           VARCHAR(255);
ALTER TABLE atendimento ALTER COLUMN cid             VARCHAR(100);
ALTER TABLE atendimento ALTER COLUMN status_consulta VARCHAR(200);
ALTER TABLE atendimento ALTER COLUMN municipio       VARCHAR(200);
