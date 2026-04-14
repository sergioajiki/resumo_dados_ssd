# Log de Progresso — Projeto resumo_dados_ssd
**Última atualização:** 2026-04-13

---

## CONTEXTO DO PROJETO

Reformulação da página **Saúde Digital / Telessaúde MS** atualmente hospedada em:
`https://paineispublicos.saude.ms.gov.br/extensions/saude-digital/saude-digital.html`

O novo projeto extrairá os dados do Qlik Sense, armazenará em banco de dados próprio e
servirá uma nova interface mais rápida e desacoplada da plataforma Qlik.

---

## SESSÃO 1 — Análise da página original (2026-04-13)

### Descobertas

- Plataforma: **Qlik Sense Enterprise on Windows** (não Grafana como se supunha inicialmente)
- Arquitetura: Mashup Qlik Sense com template **DAR One**
- Servidor web: nginx/1.25.4
- Autenticação: sessão cookie `X-Qlik-Session-HTTP` — acesso anônimo habilitado para páginas públicas

### App Qlik Sense identificado

| Campo | Valor |
|-------|-------|
| App ID | `10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb` |
| Nome | Saude Digital - FIOCRUZ |
| Proprietário | SES\elenitof |
| Criado em | 2025-09-10 |
| Publicado em | 2025-12-01 |
| Último reload | 2026-03-29 |

### Object IDs mapeados (HTML → Qlik)

**Filtros:** ADyMGB, WZmJhX, LPcjyRn, EFjdWp, wMPmU, GYdpS, GamJYb, mVRkgB

**Gráficos/KPIs:** PrajTs, GyRt, tYCvvA, ZHhJpw, GDwr, UNKYJU, rLtLP, ywaWPYj, LPdcDWm, kTpX, MEVrLj

### Tabelas e volumes identificados

| Tabela | Registros | Função |
|--------|-----------|--------|
| DB_DIGSAUDE | 16.426 | Atendimentos realizados |
| TEMPDB_USER | 9.213 | Profissionais/usuários |
| LINK | 22.325 | Ligação atendimentos × usuários |
| USERJORNADA | 5.310 | Jornadas dos profissionais |
| MUN_PILOTO | 68 | Municípios no programa piloto |
| MUNAPROV_PILOTO | 207 | Municípios aprovados no piloto |
| MUN_SEMATIVIDADE | 68 | Municípios sem atividade |
| INCID_SUPORTE | 314 | Incidentes de suporte técnico |
| MAPS_OFF | 79 | Dados geográficos para mapa |

---

## SESSÃO 2 — Definição da arquitetura (2026-04-13)

### Decisão arquitetural: Ports & Adapters (Hexagonal)

**Problema identificado:** acoplamento direto ao Qlik impede troca de plataforma no futuro.

**Solução adotada:** interface `DataSourcePort` com adaptadores intercambiáveis via `application.yml`.

```
DataSourcePort (interface)
├── QlikRestAdapter      → HTTP REST (metadados públicos) — Fase 1
├── QlikEngineAdapter    → WebSocket + JSON-RPC (dados reais) — Fase 2
└── MockAdapter          → H2/JSON fixo (desenvolvimento offline)
```

**Troca por configuração:**
```yaml
datasource:
  adapter: qlik-rest | qlik-engine | mock
  qlik:
    host: paineispublicos.saude.ms.gov.br
    app-id: 10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb
```

### Domain objects (agnósticos de plataforma)

`AppMetadata`, `DataSchema`, `TableSchema`, `FieldSchema`, `ObjectDescriptor`,
`ObjectData`, `QueryOptions`, `HealthStatus`

---

## SESSÃO 3 — Levantamento de campos disponíveis (2026-04-13)

### Campos disponíveis por categoria

| Categoria | Disponível | Campos principais |
|-----------|-----------|------------------|
| Cidades atendidas | ✅ | NOME_MUNICIPIO, IBGE_ATEND |
| Especialidades | ✅ | NOME_ESPECIALIDADE (16 tipos), CBO_MEDICO |
| Médicos | ✅ | NOME_MEDICO, CRM_USER, ID_MEDICO |
| Datas | ✅ | DT_AGENDAMENTO, DT_SOLICITACAO |
| Horários | ✅ | HR_AGENDAMENTO (81 valores, formato numérico) |
| Faixa etária | ⚠️ | DT_NASC_PACIENTE (calcular no ETL) |
| Raça/cor | ✅ | RACA_PACIENTE (5 valores) |
| Etnia | ✅ | ETNIA (9 valores) |
| CNS | ✅ | CNS_PACIENTE (8.518 registros) |
| CPF | ❌ | **Ausente** — não existe no dataset |
| Sexo/Gênero | ❌ | **Ausente** — não existe no dataset |

### Campos ausentes — ação necessária

- **CPF:** verificar no sistema transacional SES / DigiSaúde FIOCRUZ
- **Sexo/Gênero:** solicitar inclusão no script de carga do app Qlik
- **Nome do estabelecimento:** código CNES disponível → lookup na API CNES/DATASUS
- **Descrição CBO:** código disponível → lookup na tabela CBO DATASUS
- **População municipal:** ausente → integrar com IBGE para calcular cobertura %

---

## SESSÃO 4 — Intervalo de datas dos atendimentos (2026-04-13)

### Resultado da análise

| Perspectiva | Data mínima (inferida) | Data máxima (confirmada) |
|-------------|----------------------|--------------------------|
| Agendamentos | Janeiro/2025 | Março/2026 |
| Solicitações | Dezembro/2024 | Março/2026 |
| Modelo completo | **Outubro/2024** | **Março/2026** |

### Base da inferência

- `MES_ANO` (LINK) = 18 combinações → out/2024 + 12 meses 2025 + jan-mar/2026
- `ANO` (LINK) = 3 valores → confirma 2024, 2025, 2026
- `DT_SOLICITACAO_MES_ANO` = 16 meses → dez/2024 a mar/2026
- Data máxima confirmada pelo reload: 2026-03-29

### Limitação identificada

A API REST pública expõe apenas **cardinalidade** dos campos, não os valores reais.
Confirmação exata das datas requer acesso via **Engine API WebSocket**.

---

## SESSÃO 5 — Procedimento de extração e estratégia de BD (2026-04-13)

### Decisão: extrair do Qlik e gravar em BD próprio ✅

**Justificativa:** Qlik não foi projetado para servir consultas de aplicações externas em
tempo real. Sessões WebSocket são custosas. BD próprio elimina dependência no caminho crítico.

### Protocolo de extração — Qlik Engine API

```
1. WebSocket: wss://paineispublicos.saude.ms.gov.br/app/{appId}
2. OpenDoc(appId)              → DocHandle
3. CreateSessionObject(HyperCubeDef com campos desejados)
4. GetHyperCubeData(paginado, máx. 5.000 linhas/chamada)
5. DestroySessionObject + fechar WS
```

### Estratégia de sincronização

- **Carga inicial:** Full Load uma vez (< 2 minutos para todos os 50k registros)
- **Carga incremental:** diária às 09h via `@Scheduled` (após reload do Qlik às 08h)
- **Watermark:** campo `DT_NEW` (16.403 valores distintos — maior granularidade)

### Etapas de implementação definidas

```
ETAPA 1 — Estrutura base (Spring Boot + H2 file + Flyway + JPA)
ETAPA 2 — QlikEngineAdapter (WebSocket + JSON-RPC + paginação)
ETAPA 3 — Pipeline ETL (extração → transformação → upsert + SyncLog)
ETAPA 4 — Agendamento e sync incremental (@Scheduled + watermark DT_NEW)
ETAPA 5 — API REST própria sobre o BD (endpoints de consulta)
ETAPA 6 — Interface de verificação (dashboard de status + explorer)
```

---

## SESSÃO 6 — Dados de interesse por área (2026-04-13)

### Telessaúde — dados prioritários

**Produção assistencial:**
- Volume de atendimentos por município (`NOME_MUNICIPIO`)
- Série temporal de produção (`DT_AGENDAMENTO`)
- Distribuição por turno (`HR_AGENDAMENTO`)
- Taxa de realização vs cancelamento (`STATUS_CONSULTA`)
- Resolutividade (`DESFECHO_ATEND`)
- Perfil epidemiológico (`CID_CONSULTA`)

**Perfil do paciente:**
- Faixa etária calculada (`DT_NASC_PACIENTE`)
- Equidade racial (`RACA_PACIENTE`, `ETNIA`)
- Urbano vs rural (`TIPO_ZONA`)
- Identificação única (`CNS_PACIENTE`)

**Oferta de serviços:**
- Especialidades disponíveis (`NOME_ESPECIALIDADE` — 16 tipos)
- Vagas ofertadas (`USERJORNADA.VAGAS`)
- Profissionais por município (`MUNICIPIO_USER`)

### Superintendência de Saúde Digital — dados prioritários

**Cobertura e adesão:**
- Municípios no piloto (`MUN_PILOTO.MUNICIPIOCHECK`, `PILOTO_TF`)
- Municípios inativos (`MUN_SEMATIVIDADE`)
- Pipeline de expansão (`MUNAPROV_PILOTO`)

**Qualidade da plataforma:**
- Incidentes por tipo (`INCID_SUPORTE.ESPEC_ERRO`, `CLASSIFICACAO`)
- SLA de resolução (`DT_INCIDENTE` → `DT_SOL_INCIDENTE`)
- Técnicos sobrecarregados (`TECNICO`, `TEC_RESP_SOLUCAO`)

**Utilização da capacidade:**
- Taxa de ocupação = `atendimentos realizados / vagas ofertadas × 100`

### Indicadores calculados no ETL (não existem prontos no Qlik)

| Indicador | Campos fonte | Cálculo |
|-----------|-------------|---------|
| Faixa etária | `DT_NASC_PACIENTE` | Idade = hoje − nascimento → faixa |
| Tempo de espera | `DT_SOLICITACAO` + `DT_AGENDAMENTO` | Diferença em dias |
| Taxa de resolutividade | `DESFECHO_ATEND` | % desfechos resolutivos / total |
| Taxa de ocupação de vagas | `VAGAS` + `ID_ATENDIMENTO` | Realizados / ofertados |
| Taxa de inatividade municipal | `MUN_SEMATIVIDADE` + `MUN_PILOTO` | Inativos / total aderidos |
| SLA de suporte | `DT_INCIDENTE` + `DT_SOL_INCIDENTE` | Tempo médio de resolução |

---

## PENDÊNCIAS E DECISÕES ABERTAS

| # | Pendência | Prioridade |
|---|-----------|------------|
| 1 | Confirmar fonte do campo SEXO/GÊNERO do paciente | Alta |
| 2 | Confirmar fonte do campo CPF do paciente | Alta |
| 3 | Definir se MockAdapter usa H2 ou JSON fixo | Média |
| 4 | Definir se agentes ETL disparam automaticamente ou por botão | Média |
| 5 | Avaliar migração de H2 para PostgreSQL quando entrar em produção | Baixa |
| 6 | Integrar tabela CBO DATASUS para descrição dos CBO_MEDICO | Baixa |
| 7 | Integrar API CNES/DATASUS para nome dos estabelecimentos | Baixa |
| 8 | Integrar dados IBGE de população para calcular cobertura % | Baixa |

---

## PRÓXIMO PASSO DEFINIDO

Iniciar **Etapa 1** — estrutura base do projeto:
- Spring Boot 3.x + Maven
- H2 em modo file (persiste entre restarts)
- Flyway para versionamento do schema
- Entidades JPA mapeadas para as tabelas prioritárias
- Endpoints `/health` e `/status`

---

## ARQUIVOS DO PROJETO

```
resumo_dados_ssd/
└── docs/
    ├── log-progresso.md              ← este arquivo
    ├── arquitetura-datasource.md     ← Ports & Adapters, Engine API, WebSocket
    └── campos-disponiveis-telessaude.md ← todos os campos mapeados com disponibilidade

../saude-digital-analise.md           ← análise completa da página original
```
