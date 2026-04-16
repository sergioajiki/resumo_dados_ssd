-- =============================================================================
-- V8 — Adiciona coluna classif_conclusao à tabela atendimento
--
-- Campo extraído do Qlik que indica a classificação da conclusão do atendimento.
-- Valores esperados: "Com atendimento", "Sem atendimento".
-- Usado para filtrar registros válidos para o BPAi (Boletim de Produção Ambulatorial).
-- =============================================================================

ALTER TABLE atendimento ADD COLUMN classif_conclusao VARCHAR(100);
