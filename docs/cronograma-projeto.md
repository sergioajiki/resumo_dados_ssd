# Cronograma Completo — Projeto resumo_dados_ssd
**Data:** 2026-04-13 · **Atualizado:** 2026-04-14  
**Stack:** Java 21 · Spring Boot 3.4 · H2 (file) · Flyway · Maven  
**Princípios:** SOLID · Hexagonal Architecture · CI/CD

> **Nota:** Testes automatizados removidos do escopo desta fase de desenvolvimento.
> Os arquivos de contract test e ArchUnit já criados nas Fases 3–4 são mantidos
> como documentação viva da arquitetura e referência para futuras implementações.

---

## VISÃO GERAL DAS FASES

```
FASE 0 │ Setup e Governança         │ GitHub, branches, CI/CD, templates
FASE 1 │ Estrutura base             │ Spring Boot, pacotes, profiles, Swagger
FASE 2 │ Domain Model               │ Contratos, entidades, exceções de domínio
FASE 3 │ MockAdapter                │ Primeiro adaptador — desenvolvimento offline
FASE 4 │ Contract Tests             │ Testes que garantem a troca de API sem quebrar
FASE 5 │ QlikRestAdapter            │ Metadados públicos via HTTP REST
FASE 6 │ QlikEngineAdapter          │ Dados reais via WebSocket + JSON-RPC
FASE 7 │ ETL Pipeline               │ Extração → Transformação → Carga no BD
FASE 8 │ API REST própria           │ Endpoints rápidos sobre o BD local
FASE 9 │ Documentação               │ JavaDoc, README, Wiki, diagramas
FASE 10│ Interface de verificação   │ Dashboard de status + explorer de dados
```

---

## FASE 0 — Setup e Governança do Repositório

### 0.1 Criar repositório GitHub
- [ ] Criar repositório `resumo-dados-ssd` no GitHub (público ou privado conforme política SES)
- [ ] Definir descrição: *"ETL e API para extração e consulta dos dados do Núcleo de Telessaúde e Superintendência de Saúde Digital — SES/MS"*
- [ ] Adicionar tópicos: `spring-boot`, `qlik-sense`, `telessaude`, `saude-digital`, `ms`
- [ ] Configurar `.gitignore` para Java/Maven
- [ ] Adicionar `LICENSE` (Apache 2.0 ou MIT)

### 0.2 Estratégia de branches
```
main         → código estável, apenas via PR aprovado
develop      → integração contínua das features
feature/*    → cada funcionalidade nova
hotfix/*     → correções urgentes em produção
release/*    → preparação de versão
```
- [ ] Criar branch `develop` a partir de `main`
- [ ] Configurar **branch protection** em `main`:
  - Requer PR com pelo menos 1 aprovação
  - Requer CI passando antes do merge
  - Proibir push direto

### 0.3 GitHub Actions — CI/CD
- [ ] Pipeline `.github/workflows/ci.yml`:
  ```yaml
  on: [push, pull_request]
  jobs:
    build:
      - mvn clean compile       # compila e valida dependências
  ```
- [ ] Pipeline `.github/workflows/cd.yml` (opcional — deploy em ambiente de homologação)

### 0.4 Templates e padrões
- [ ] Criar `.github/ISSUE_TEMPLATE/feature.md`
- [ ] Criar `.github/ISSUE_TEMPLATE/bug.md`
- [ ] Criar `.github/PULL_REQUEST_TEMPLATE.md` com checklist:
  - [ ] JavaDoc nos métodos públicos novos
  - [ ] Sem violações SOLID introduzidas
  - [ ] Código compila sem erros
- [ ] Criar `CONTRIBUTING.md` com padrões de commit (Conventional Commits)

### 0.5 Milestones GitHub
```
Milestone 1 → Fase 0-2: Estrutura e contratos
Milestone 2 → Fase 3-4: MockAdapter e arquitetura
Milestone 3 → Fase 5-6: Adaptadores Qlik
Milestone 4 → Fase 7-8: ETL e API REST
Milestone 5 → Fase 9-10: Documentação e UI
```

---

## FASE 1 — Estrutura Base do Projeto

### 1.1 Scaffold Spring Boot
- [ ] Criar projeto via Spring Initializr:
  - Java 17, Spring Boot 3.x, Maven
  - Dependências: Spring Web, Spring Data JPA, H2, Flyway, Lombok, Validation, Actuator
- [ ] Configurar `pom.xml` com dependências adicionais:
  - `Java-WebSocket 1.5.4` (Engine API)
  - `Springdoc OpenAPI 2.x` (Swagger)
  - `MapStruct` (mapeamento DTO)

### 1.2 Estrutura de pacotes (Hexagonal Architecture)
```
br.gov.ms.saude.ssd/
├── domain/                        → Regras de negócio puras (sem frameworks)
│   ├── model/                     → Entidades e value objects do domínio
│   ├── port/
│   │   ├── in/                    → Portas de entrada (use cases)
│   │   └── out/                   → Portas de saída (DataSourcePort, SyncPort)
│   └── exception/                 → Exceções de domínio
├── application/                   → Orquestra os use cases
│   ├── service/                   → Implementa as portas de entrada
│   └── usecase/                   → Interfaces dos use cases
├── adapter/
│   ├── in/
│   │   ├── rest/                  → Controllers REST (porta de entrada HTTP)
│   │   └── scheduler/             → @Scheduled (porta de entrada temporal)
│   └── out/
│       ├── qlik/
│       │   ├── rest/              → QlikRestAdapter
│       │   └── engine/            → QlikEngineAdapter (WebSocket)
│       ├── mock/                  → MockAdapter
│       └── persistence/           → JPA Repositories, entidades de BD
└── config/                        → Configurações Spring, Beans, Swagger
```

> **SOLID aplicado na estrutura:**
> - **S** — cada pacote tem responsabilidade única
> - **O** — novos adaptadores não alteram o domínio
> - **L** — qualquer adaptador substitui `DataSourcePort` sem alterar serviços
> - **I** — portas granulares (`DataSourcePort`, `SyncPort`, `ReportPort`)
> - **D** — domínio depende de abstrações, nunca de implementações concretas

### 1.3 Profiles da aplicação
```yaml
# application.yml — base
spring:
  profiles:
    active: dev

datasource:
  adapter: mock           # padrão seguro: nunca quebra sem Qlik

# application-dev.yml
datasource:
  adapter: qlik-engine
  qlik:
    host: paineispublicos.saude.ms.gov.br
    app-id: 10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb

# application-test.yml
datasource:
  adapter: mock           # testes nunca dependem do Qlik

# application-prod.yml
datasource:
  adapter: qlik-engine
  sync:
    schedule: "0 0 9 * * *"
```

### 1.4 H2 em modo file + Flyway
- [ ] Configurar H2 no modo file (persiste entre restarts):
  ```yaml
  spring:
    datasource:
      url: jdbc:h2:file:./data/ssd-db
    h2.console.enabled: true
  ```
- [ ] Criar primeira migration `V1__create_schema.sql`

---

## FASE 2 — Domain Model e Contratos (SOLID — SRP + DIP)

> **Agente responsável:** `domain-modeler` — cria as interfaces e value objects

### 2.1 Portas de saída (contratos que os adaptadores devem cumprir)

```java
/**
 * Porta de saída principal — define o contrato que qualquer fonte de dados
 * deve cumprir para ser utilizada pelo domínio.
 *
 * Implementações: QlikRestAdapter, QlikEngineAdapter, MockAdapter
 * Princípio aplicado: DIP — o domínio depende desta abstração, nunca de implementações.
 */
public interface DataSourcePort {
    AppMetadata getAppMetadata();
    DataSchema getDataSchema();
    ObjectData getObjectData(String objectId, QueryOptions options);
    List<ObjectDescriptor> listAvailableObjects();
    HealthStatus checkHealth();
}

/**
 * Porta de saída para extração em lote — usada pelo ETL pipeline.
 * Separada de DataSourcePort para respeitar ISP (Interface Segregation).
 */
public interface DataExtractorPort {
    ExtractResult extractTable(String tableName, List<String> fields, ExtractOptions options);
    ExtractResult extractSince(String tableName, List<String> fields, LocalDateTime watermark);
}

/**
 * Porta de saída para controle de sincronização.
 */
public interface SyncRepositoryPort {
    Optional<LocalDateTime> getLastSyncTime(String tableName);
    void recordSync(SyncLog log);
}
```

### 2.2 Portas de entrada (use cases)

```java
public interface ConsultarAtendimentosUseCase {
    Page<AtendimentoDTO> consultar(AtendimentoFilter filter, Pageable pageable);
}

public interface ExecutarSyncUseCase {
    SyncResult executarFullSync();
    SyncResult executarIncrementalSync();
}

public interface ConsultarSchemaUseCase {
    DataSchema getSchema();
    HealthStatus getHealth();
}
```

### 2.3 Domain objects (imutáveis, sem anotações de framework)

```
AppMetadata       → id, nome, descricao, ultimoReload
DataSchema        → List<TableSchema>
TableSchema       → nome, totalRegistros, List<FieldSchema>
FieldSchema       → nome, tipo, cardinalidade, isPrimaryKey, tags
ObjectDescriptor  → id, tipo (CHART/KPI/FILTER), titulo
ObjectData        → objectId, headers, rows (List<List<Object>>)
QueryOptions      → filters, pagination, sortBy
HealthStatus      → status (UP/DOWN/DEGRADED), latencyMs, message
ExtractResult     → tableName, rows, totalExtraidos, watermark
SyncLog           → tabela, iniciadoEm, concluidoEm, registros, status, erro
AtendimentoFilter → municipio, periodo, especialidade, status, faixaEtaria
```

### 2.4 Exceções de domínio
```java
DataSourceUnavailableException   → Qlik ou qualquer fonte inacessível
DataExtractionException          → Falha durante extração
SyncAlreadyRunningException      → Sync já em andamento
InvalidQueryOptionsException     → Filtros inválidos
```

---

## FASE 3 — MockAdapter (Desenvolvimento Offline)

> **Agente responsável:** `mock-builder`

### 3.1 Implementação
- [ ] `MockDataSourceAdapter implements DataSourcePort, DataExtractorPort`
- [ ] Carregar dados de `src/test/resources/mock-data/`:
  - `atendimentos.json` — 50 registros representativos
  - `profissionais.json` — 10 registros
  - `municipios.json` — lista de municípios
  - `schema.json` — estrutura do schema

### 3.2 Comentários de documentação (obrigatórios)
```java
/**
 * Adaptador de dados simulados para uso em desenvolvimento e testes.
 *
 * <p>Carrega dados estáticos de arquivos JSON em {@code src/test/resources/mock-data/}.
 * Não realiza nenhuma chamada de rede, garantindo execução determinística nos testes.</p>
 *
 * <p>Ativação: {@code datasource.adapter=mock} no application.yml</p>
 *
 * @see DataSourcePort
 * @see DataExtractorPort
 */
@Component("mockAdapter")
@ConditionalOnProperty(name = "datasource.adapter", havingValue = "mock")
public class MockDataSourceAdapter implements DataSourcePort, DataExtractorPort { ... }
```

---

## FASE 4 — Referência de Arquitetura (Contract Tests e ArchUnit)

> **Nota:** Esta fase foi mantida como **documentação viva da arquitetura**.
> Os arquivos já criados em `src/test/java/` descrevem os contratos que qualquer
> adaptador deve cumprir e as regras SOLID do projeto. Não são executados nesta fase.

### 4.1 Arquivos de referência disponíveis

```
DataSourcePortContractTest.java    → contrato abstrato de DataSourcePort
DataExtractorPortContractTest.java → contrato abstrato de DataExtractorPort
MockAdapterDataSourceContractTest  → exemplo de subclasse para MockAdapter
MockAdapterExtractorContractTest   → exemplo de subclasse para DataExtractorPort
ArchitectureTest.java              → regras ArchUnit (domínio ≠ adapters, etc.)
SsdApplicationIntegrationTest.java → exemplo de teste de integração Spring
```

### 4.2 Como usar ao substituir o Qlik

Ao criar um novo adaptador, os arquivos acima servem de guia:
1. Implemente `DataSourcePort` e/ou `DataExtractorPort`
2. Consulte `DataSourcePortContractTest` para ver os comportamentos esperados
3. Adicione `@ConditionalOnProperty(havingValue = "nova-api")`
4. Atualize `application-prod.yml`: `datasource.adapter: nova-api`

---

## FASE 5 — QlikRestAdapter (Metadados Públicos)

> **Agente responsável:** `qlik-rest-builder`

### 5.1 Implementação
- [ ] `QlikRestAdapter implements DataSourcePort`
- [ ] `QlikRestClient` — RestTemplate configurado com timeout e retry
- [ ] Mapeia os 2 endpoints públicos:
  - `GET /api/v1/apps/{appId}` → `AppMetadata`
  - `GET /api/v1/apps/{appId}/data/metadata` → `DataSchema`
- [ ] `QlikRestResponseMapper` — converte JSON Qlik para domain objects (MapStruct)

---

## FASE 6 — QlikEngineAdapter (WebSocket + JSON-RPC)

> **Agente responsável:** `qlik-engine-builder`

### 6.1 Implementação
- [ ] `QlikEngineAdapter implements DataSourcePort, DataExtractorPort`
- [ ] `QlikWebSocketClient` — gerencia ciclo de vida da conexão WS
- [ ] `QlikJsonRpcProtocol` — serializa/deserializa mensagens JSON-RPC 2.0
- [ ] `QlikHyperCubeBuilder` — monta a definição do HyperCube por tabela
- [ ] `QlikPaginationStrategy` — controla paginação (5.000 linhas/página)
- [ ] `QlikSessionManager` — pool de sessões, reconexão automática

### 6.2 Comentários obrigatórios exemplo
```java
/**
 * Executa uma extração paginada de dados do Qlik Sense via Engine API.
 *
 * <p>O protocolo utilizado é JSON-RPC 2.0 sobre WebSocket. O fluxo é:</p>
 * <ol>
 *   <li>Abre conexão WebSocket em {@code wss://{host}/app/{appId}}</li>
 *   <li>Chama {@code OpenDoc} para obter o handle do documento</li>
 *   <li>Chama {@code CreateSessionObject} com a definição do HyperCube</li>
 *   <li>Itera com {@code GetHyperCubeData} até esgotar os registros</li>
 *   <li>Destrói a sessão e fecha a conexão</li>
 * </ol>
 *
 * @param tableName  nome da tabela Qlik a ser extraída
 * @param fields     lista de campos (fieldDefs) a incluir no HyperCube
 * @param options    opções de extração (watermark, tamanho de página, timeout)
 * @return {@link ExtractResult} com todos os registros extraídos
 * @throws DataSourceUnavailableException se não for possível conectar ao servidor
 * @throws DataExtractionException        se ocorrer erro durante a extração
 */
public ExtractResult extractTable(String tableName, List<String> fields, ExtractOptions options) { ... }
```

---

## FASE 7 — ETL Pipeline

> **Agente responsável:** `etl-pipeline-builder`

### 7.1 Componentes

```
QlikExtractorService    → orquestra a extração (usa DataExtractorPort)
FieldTransformerService → transforma campos brutos em tipos Java corretos
  ├── DateParser        → DT_NASC_PACIENTE (string) → LocalDate
  ├── AgeCalculator     → DT_NASC_PACIENTE → faixa etária
  ├── TimeConverter     → HR_AGENDAMENTO (numérico) → LocalTime
  └── NullHandler       → trata campos nulos/vazios do Qlik
LoaderService           → persiste no BD com upsert em batch
SyncScheduler           → @Scheduled dispara sync diário às 09h
SyncLogService          → registra execução, duração, registros, erros
```

### 7.2 Migrations Flyway (schema do BD)
```
V1__create_schema.sql         → tabelas atendimento, profissional, jornada_vagas
V2__create_lookup_tables.sql  → municipio, especialidade, status_consulta
V3__create_sync_log.sql       → tabela de controle de sincronização
V4__create_indexes.sql        → índices de performance nas consultas frequentes
```

---

## FASE 8 — API REST Própria

> **Agente responsável:** `api-rest-builder`

### 8.1 Endpoints

```
GET  /api/v1/atendimentos              → lista paginada com filtros
GET  /api/v1/atendimentos/{id}         → atendimento por ID
GET  /api/v1/atendimentos/resumo       → KPIs: total, por município, por especialidade
GET  /api/v1/profissionais             → lista de profissionais
GET  /api/v1/vagas                     → vagas por período e especialidade
GET  /api/v1/municipios                → municípios atendidos + status piloto
GET  /api/v1/incidentes                → incidentes de suporte
GET  /api/v1/schema                    → schema das tabelas extraídas
GET  /api/v1/health                    → saúde da conexão com a fonte
GET  /api/v1/sync/status               → status da última sincronização
POST /api/v1/sync/trigger              → dispara sync manual (admin)
```

### 8.2 Documentação Swagger/OpenAPI
- [ ] Anotar todos os endpoints com `@Operation`, `@ApiResponse`, `@Parameter`
- [ ] Disponível em `/swagger-ui.html`
- [ ] Exportar `openapi.json` como artefato do build

---

## FASE 9 — Documentação

### 9.1 JavaDoc (obrigatório em todos os métodos públicos)
- [ ] Padrão mínimo: `@param`, `@return`, `@throws`, `@see`
- [ ] Classes de interface: explicar o contrato e quando usar
- [ ] Classes de adaptador: explicar protocolo e como substituir
- [ ] Classes de serviço: explicar regra de negócio aplicada

### 9.2 README.md
```
├── Visão geral do projeto
├── Arquitetura (diagrama Mermaid)
├── Como executar localmente
├── Profiles disponíveis (dev/test/prod)
├── Como trocar o adaptador de fonte de dados
├── Como adicionar um novo adaptador
├── Endpoints da API (link para Swagger)
├── Como executar os testes
├── Estrutura de pacotes explicada
└── Contribuindo
```

### 9.3 Wiki GitHub
```
Home
├── Arquitetura do projeto
├── Guia de desenvolvimento
├── Contract Tests — como funcionam
├── Como substituir o Qlik por outra API
├── Campos disponíveis e mapeamentos
├── Schema do banco de dados
└── Troubleshooting
```

### 9.4 Diagramas (Mermaid, no próprio README/Wiki)
- [ ] Diagrama de arquitetura hexagonal
- [ ] Fluxo de extração WebSocket
- [ ] Fluxo ETL (extract → transform → load)
- [ ] Diagrama de sequência da sincronização

---

## FASE 10 — Interface de Verificação (UI)

> **Agente responsável:** `ui-builder`

### 10.1 Páginas (Thymeleaf + Bootstrap 5)
```
/              → Dashboard: status sync, volumes por tabela, health
/explorer      → Testa endpoints e exibe resposta JSON formatada
/schema        → Visualiza tabelas e campos extraídos
/sync          → Histórico de sincronizações + trigger manual
/incidentes    → Tabela de incidentes de suporte
```

---

## RESUMO: USO DE AGENTES POR FASE

| Fase | Agente | Responsabilidade |
|------|--------|-----------------|
| 0 | `repo-setup` | GitHub, Actions, templates, milestones |
| 1 | `scaffold-builder` | Spring Boot, estrutura de pacotes, profiles |
| 2 | `domain-modeler` | Interfaces, domain objects, exceções |
| 3 | `mock-builder` | MockAdapter + dados mock JSON |
| 4 | `arch-documenter` | Referência de arquitetura (contract tests + ArchUnit) |
| 5 | `qlik-rest-builder` | QlikRestAdapter (metadados HTTP) |
| 6 | `qlik-engine-builder` | WebSocket, JSON-RPC, paginação |
| 7 | `etl-pipeline-builder` | JPA, Extract, Transform, Load, Scheduler |
| 8 | `api-rest-builder` | Controllers, DTOs, Swagger |
| 9 | `doc-writer` | JavaDoc, README, Wiki, diagramas |
| 10 | `ui-builder` | Thymeleaf dashboard + explorer |

---

## DEPENDÊNCIAS ENTRE FASES

```
0 ──> 1 ──> 2 ──> 3 ──> 4 ──> 5 ──> 6
                   │                  │
                   └──────────────────┤
                                      ▼
                                      7 ──> 8 ──> 9 ──> 10
```

- **Fase 3 (mock) viabiliza o desenvolvimento de 7 e 8 sem depender do Qlik**
- **Fase 4 (referência de arquitetura) orienta a criação dos adaptadores 5 e 6**
- **Fase 9 (documentação) acontece em paralelo com 7, 8 e 10**

---

## CHECKLIST SOLID — Revisão por fase

| Princípio | Como garantir | Fase |
|-----------|--------------|------|
| **S** — Single Responsibility | Uma classe, uma razão para mudar | 1, 2 |
| **O** — Open/Closed | Novos adaptadores sem alterar domínio | 2, 3, 4 |
| **L** — Liskov Substitution | `DataSourcePort` garante substituibilidade via interface | 2, 4 |
| **I** — Interface Segregation | `DataSourcePort` ≠ `DataExtractorPort` ≠ `SyncPort` | 2 |
| **D** — Dependency Inversion | Domínio depende de interfaces, injeção via Spring | 1, 2 |

---

## COMO SUBSTITUIR O QLIK (guia rápido)

Quando o Qlik for desacoplado, o processo é:

```
1. Criar NovaApiAdapter implements DataSourcePort, DataExtractorPort
2. Consultar DataSourcePortContractTest como guia dos comportamentos esperados
3. Adicionar @ConditionalOnProperty(havingValue = "nova-api")
4. Atualizar application-prod.yml: datasource.adapter: nova-api
5. Zero alterações no domínio, serviços, ETL ou API REST
```

---

## REFERÊNCIAS DO PROJETO

- `docs/log-progresso.md` — histórico de decisões e descobertas
- `docs/arquitetura-datasource.md` — Ports & Adapters, Engine API
- `docs/campos-disponiveis-telessaude.md` — campos mapeados
- `../saude-digital-analise.md` — análise da página original
