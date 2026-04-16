-- =============================================================================
-- V7 — Amplia cns_paciente para acomodar valores sentinela da fonte Qlik
--
-- O campo CNS_PACIENTE na fonte pode conter strings de zeros (ex: 48 dígitos)
-- como marcador de "CNS desconhecido", excedendo o limite anterior de 20 chars.
-- =============================================================================

ALTER TABLE atendimento ALTER COLUMN cns_paciente VARCHAR(50);
