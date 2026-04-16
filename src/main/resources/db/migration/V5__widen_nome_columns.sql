-- =============================================================================
-- V5 — Amplia colunas nome/nome_medico para acomodar dados reais do Qlik
--
-- Motivo: dados reais de TEMPDB_USER contêm valores com 212+ caracteres
-- no campo nome (ex: anotações clínicas), acima do limite anterior de 150.
-- =============================================================================

ALTER TABLE profissional ALTER COLUMN nome VARCHAR(500);
ALTER TABLE atendimento ALTER COLUMN nome_medico VARCHAR(500);
