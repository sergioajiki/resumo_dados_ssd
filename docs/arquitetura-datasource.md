# Arquitetura DataSource — Projeto resumo_dados_ssd
**Data:** 2026-04-13

---

## A API do Qlik Sense — Como Funciona

O Qlik Sense expõe **duas APIs completamente diferentes** em natureza:

### 1. REST API (HTTP convencional)
Usada para metadados e administração. As rotas com acesso público:

```
GET /api/v1/apps/{appId}
    → metadados do app (nome, dono, datas, status)

GET /api/v1/apps/{appId}/data/metadata
    → modelo de dados: tabelas, campos, cardinalidades, relacionamentos
```

**App ID do projeto:**
```
10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb
```

**URLs completas:**
```
https://paineispublicos.saude.ms.gov.br/api/v1/apps/10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb
https://paineispublicos.saude.ms.gov.br/api/v1/apps/10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb/data/metadata
```

---

### 2. Engine API (WebSocket + JSON-RPC)
Esta é a API **principal de dados**. Ela não é REST — usa **WebSocket** com protocolo **JSON-RPC 2.0**.
É através dela que os objetos Qlik (gráficos, KPIs, filtros) realmente carregam os dados.

**Endpoint WebSocket:**
```
ws://paineispublicos.saude.ms.gov.br/app/{appId}
```

**Toda comunicação é via mensagens JSON-RPC:**
```json
// Envio
{ "jsonrpc": "2.0", "id": 1, "method": "OpenDoc", "params": ["{appId}"] }

// Retorno
{ "jsonrpc": "2.0", "id": 1, "result": { "qReturn": { "qType": "Doc", "qHandle": 1 } } }
```

**Para buscar dados de um objeto (gráfico):**
```json
// 1. Obtém handle do objeto
{ "method": "GetObject", "params": ["{objectId}"] }

// 2. Obtém estrutura e dados
{ "method": "GetLayout", "handle": X }

// 3. Obtém valores da matriz de dados
{ "method": "GetHyperCubeData", "handle": X, "params": [...] }
```

**Fluxo completo de uma consulta Engine API:**
```
Cliente                          Qlik Engine
  │                                   │
  │── WS connect /app/{appId} ───────>│
  │<─ session established ────────────│
  │── OpenDoc({appId}) ──────────────>│
  │<─ DocHandle: 1 ───────────────────│
  │── GetObject({objectId}) ─────────>│
  │<─ ObjectHandle: 5 ────────────────│
  │── GetLayout(handle=5) ───────────>│
  │<─ { dimensões, medidas, dados } ──│
  │── Disconnect ────────────────────>│
```

---

## O Problema de Acoplamento

Se o sistema for construído amarrado diretamente ao Qlik, qualquer mudança de plataforma
(trocar Qlik por uma API REST própria, por um banco direto, ou por um arquivo CSV) quebraria tudo.

---

## Arquitetura de Desacoplamento — Ports & Adapters (Hexagonal)

```
┌─────────────────────────────────────────────────────────┐
│                    DOMÍNIO / CORE                        │
│                                                          │
│   AgentService ──────> DataSourcePort (interface)        │
│   SchemaService                  │                       │
│   ReportService                  │                       │
└──────────────────────────────────┼──────────────────────┘
                                   │
              ┌────────────────────┼─────────────────────┐
              │                    │                      │
   ┌──────────▼──────┐  ┌──────────▼──────┐  ┌──────────▼──────┐
   │  QlikRestAdapter │  │ QlikEngineAdapter│  │  MockAdapter     │
   │  (HTTP REST)     │  │  (WebSocket)     │  │  (H2/JSON fixo)  │
   └──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## A Interface (Port)

```java
// O domínio só conhece este contrato — nunca os adaptadores
public interface DataSourcePort {

    // Metadados do app/fonte
    AppMetadata getAppMetadata();

    // Schema: tabelas e campos disponíveis
    DataSchema getDataSchema();

    // Dados de um objeto específico (gráfico, KPI, tabela)
    ObjectData getObjectData(String objectId, QueryOptions options);

    // Lista de objetos disponíveis
    List<ObjectDescriptor> listAvailableObjects();

    // Verifica conectividade
    HealthStatus checkHealth();
}
```

---

## Os Adaptadores

```java
// Adaptador 1 — Qlik REST (metadados, acesso público)
@Component("qlikRestAdapter")
@ConditionalOnProperty(name = "datasource.adapter", havingValue = "qlik-rest")
public class QlikRestAdapter implements DataSourcePort {
    // Usa RestTemplate → /api/v1/apps/{appId}/data/metadata
}

// Adaptador 2 — Qlik Engine API (dados reais via WebSocket)
@Component("qlikEngineAdapter")
@ConditionalOnProperty(name = "datasource.adapter", havingValue = "qlik-engine")
public class QlikEngineAdapter implements DataSourcePort {
    // Usa WebSocket + JSON-RPC
}

// Adaptador 3 — Stub para testes / desenvolvimento offline
@Component("mockAdapter")
@ConditionalOnProperty(name = "datasource.adapter", havingValue = "mock")
public class MockDataSourceAdapter implements DataSourcePort {
    // Retorna dados fixos do H2 — sem precisar do Qlik
}
```

---

## Troca por Configuração (application.yml)

```yaml
# Para usar Qlik REST (metadados públicos — Fase 1)
datasource:
  adapter: qlik-rest
  qlik:
    host: paineispublicos.saude.ms.gov.br
    app-id: 10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb

---
# Para usar Qlik Engine (dados completos via WebSocket — Fase 2)
datasource:
  adapter: qlik-engine

---
# Para desenvolvimento offline sem Qlik
datasource:
  adapter: mock
```

---

## Modelo de Dados Agnóstico (Domain Objects)

Esses objetos representam os dados **independente de onde vieram**:

| Classe | Campos principais |
|--------|------------------|
| `AppMetadata` | id, nome, descrição, última atualização |
| `DataSchema` | List\<TableSchema\> |
| `TableSchema` | nome, rowCount, List\<FieldSchema\> |
| `FieldSchema` | nome, tipo, cardinalidade, isPrimaryKey |
| `ObjectDescriptor` | id, tipo (CHART/KPI/FILTER), título |
| `ObjectData` | objectId, List\<String\> headers, List\<List\<Object\>\> rows |
| `QueryOptions` | filters, pagination, sortBy |
| `HealthStatus` | status (UP/DOWN/DEGRADED), latencyMs, message |

---

## Cronograma por Fase

```
Fase 1 — Core + Adaptador REST (público, sem autenticação)
  ├── DataSourcePort interface
  ├── QlikRestAdapter (2 endpoints públicos)
  ├── MockAdapter (para testes offline)
  ├── H2 + entidades de cache das respostas
  └── UI básica: schema e status de conexão

Fase 2 — Adaptador Engine API (WebSocket)
  ├── QlikEngineAdapter com Java-WebSocket
  ├── Coleta de dados reais dos 19 Object IDs mapeados
  └── UI mostrando dados por objeto

Fase 3 — Análise e Relatório
  ├── SchemaAnalysisAgent, ReportAgent
  ├── Exportação dos dados coletados (CSV/Excel)
  └── Avaliação de substituição do Qlik por API própria
```

---

## Object IDs Mapeados (referência)

### Filtros
| Object ID | Elemento DOM |
|-----------|-------------|
| ADyMGB | FILTER-1 |
| WZmJhX | FILTER-2 |
| LPcjyRn | FILTER-3 (Período/Mês) |
| EFjdWp | FILTER-4 |
| wMPmU | FILTER-5 |
| GYdpS | FILTER-6 |
| GamJYb | FILTER-7 |
| mVRkgB | FILTER-8 |

### Gráficos e KPIs
| Object ID | Elemento DOM | Tipo |
|-----------|-------------|------|
| PrajTs | relacao-CHART-1 | Gráfico (ha-550) |
| GyRt | relacao-CHART-2 | Gráfico (ha-550) |
| tYCvvA | relacao-CHART-3 | KPI (ha-90) |
| ZHhJpw | relacao-CHART-4 | KPI (ha-90) |
| GDwr | relacao-CHART-5 | KPI (ha-90) |
| UNKYJU | relacao-CHART-6 | Gráfico (ha-300) |
| rLtLP | relacao-CHART-7 | Gráfico (ha-300) |
| ywaWPYj | relacao-CHART-8 | Gráfico |
| LPdcDWm | relacao-CHART-10 | Gráfico (ha-400) |
| kTpX | relacao-CHART-11 | Gráfico (ha-400) |
| MEVrLj | relacao-CHART-12 | Gráfico (ha-400) |

---

## Referências

- Análise completa da página original: `../saude-digital-analise.md`
- Qlik Engine API docs: https://help.qlik.com/en-US/sense-developer/latest/APIs/EngineAPI/
- Qlik REST API docs: https://help.qlik.com/en-US/sense-developer/latest/APIs/REST/
