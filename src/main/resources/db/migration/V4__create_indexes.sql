-- =============================================================================
-- V4__create_indexes.sql
-- Índices de performance para as colunas de maior uso em consultas.
--
-- Estratégia de indexação:
--   1. Colunas de filtro (WHERE): municipio, status_consulta, especialidade
--   2. Colunas de ordenação/range (ORDER BY, BETWEEN): dt_agendamento, dt_atendimento
--   3. Colunas de join (FK): profissional_id
--   4. Colunas de busca por identificador: cns_paciente, cod_ibge
--
-- Índices compostos foram evitados nesta versão inicial para não antecipar
-- padrões de consulta que ainda não foram validados. Adicionar conforme
-- o profiler de queries indicar necessidade (V5+ se necessário).
--
-- Convenção de nomes: idx_{tabela}_{coluna(s)}
-- =============================================================================

-- =============================================================================
-- Índices da tabela: atendimento
-- =============================================================================

-- Filtro por município: relatórios são quase sempre filtrados por município
CREATE INDEX IF NOT EXISTS idx_atendimento_municipio
    ON atendimento (municipio);

-- Filtro/ordenação por data de agendamento: consultas por período (mês, ano)
CREATE INDEX IF NOT EXISTS idx_atendimento_dt_agendamento
    ON atendimento (dt_agendamento);

-- Filtro por especialidade: indicadores de produção por especialidade
CREATE INDEX IF NOT EXISTS idx_atendimento_especialidade
    ON atendimento (especialidade);

-- Filtro por status: separar agendados x realizados x cancelados
CREATE INDEX IF NOT EXISTS idx_atendimento_status_consulta
    ON atendimento (status_consulta);

-- Busca por CNS: para verificar histórico de um paciente específico
-- (uso administrativo; não usado nos dashboards públicos)
CREATE INDEX IF NOT EXISTS idx_atendimento_cns_paciente
    ON atendimento (cns_paciente);

-- Índice na coluna de watermark ETL: a sincronização incremental usa
-- dt_criacao para buscar registros novos ("WHERE dt_criacao > ?"),
-- tornando este índice essencial para performance do ETL
CREATE INDEX IF NOT EXISTS idx_atendimento_dt_criacao
    ON atendimento (dt_criacao);

-- =============================================================================
-- Índices da tabela: profissional
-- =============================================================================

-- Filtro por município: relatórios de profissionais por localidade
CREATE INDEX IF NOT EXISTS idx_profissional_municipio
    ON profissional (municipio);

-- Filtro por especialidade: listagem de profissionais por área de atuação
CREATE INDEX IF NOT EXISTS idx_profissional_especialidade_nome
    ON profissional (especialidade_nome);

-- =============================================================================
-- Índices da tabela: jornada_vagas
-- =============================================================================

-- FK para profissional: usada em todos os joins com a tabela profissional
CREATE INDEX IF NOT EXISTS idx_jornada_vagas_profissional_id
    ON jornada_vagas (profissional_id);

-- Filtro por data: consultas de oferta de vagas em um período específico
CREATE INDEX IF NOT EXISTS idx_jornada_vagas_dt_atendimento
    ON jornada_vagas (dt_atendimento);
