-- V10 — Adiciona todos os campos restantes do DB_DIGSAUDE
-- Campos de estabelecimento (essenciais para BPAi)
-- cnes_estabelecimento contém código + nome: "9318348 - UBSF SEBASTIANA DE BRITO PASCOAL"
ALTER TABLE atendimento ADD COLUMN cnes_estabelecimento VARCHAR(200);
ALTER TABLE atendimento ADD COLUMN id_estabelecimento VARCHAR(200);
ALTER TABLE atendimento ADD COLUMN id_medico VARCHAR(100);
-- Campos complementares de classificação
ALTER TABLE atendimento ADD COLUMN classif_cor VARCHAR(50);
ALTER TABLE atendimento ADD COLUMN tp_nw_conclusao VARCHAR(50);
ALTER TABLE atendimento ADD COLUMN id_digsaude_ref VARCHAR(50);
-- Dados de contato/endereço do paciente
ALTER TABLE atendimento ADD COLUMN telefone VARCHAR(30);
ALTER TABLE atendimento ADD COLUMN cep_paciente VARCHAR(10);
ALTER TABLE atendimento ADD COLUMN rua_paciente VARCHAR(200);
ALTER TABLE atendimento ADD COLUMN num_paciente VARCHAR(20);
ALTER TABLE atendimento ADD COLUMN bairro_paciente VARCHAR(150);
ALTER TABLE atendimento ADD COLUMN complemento_end VARCHAR(150);
ALTER TABLE atendimento ADD COLUMN descricao_endereco VARCHAR(300);
ALTER TABLE atendimento ADD COLUMN endereco_completo VARCHAR(500);
-- Notas clínicas (TEXT para comportar 350+ caracteres)
ALTER TABLE atendimento ADD COLUMN descricao_consulta TEXT;
