-- =============================================================================
-- V9 — Adiciona coluna tipo_servico à tabela atendimento
--
-- Campo extraído do Qlik que indica a modalidade do atendimento.
-- Valores esperados: "Teleconsulta", "Teleinterconsulta", entre outros.
-- Usado para filtrar registros válidos para o BPAi (apenas telessaúde).
-- =============================================================================

ALTER TABLE atendimento ADD COLUMN tipo_servico VARCHAR(50);
