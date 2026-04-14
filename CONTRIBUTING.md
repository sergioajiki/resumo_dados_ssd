# Guia de Contribuição — resumo-dados-ssd

Obrigado por contribuir com o projeto **resumo-dados-ssd**!  
Este guia define os padrões que todos os colaboradores devem seguir para manter
a qualidade, rastreabilidade e consistência do código.

---

## Índice

1. [Pré-requisitos](#pré-requisitos)
2. [Fluxo de branches](#fluxo-de-branches)
3. [Padrão de commits (Conventional Commits)](#padrão-de-commits)
4. [Antes de abrir um Pull Request](#antes-de-abrir-um-pull-request)
5. [Checklist do Pull Request](#checklist-do-pull-request)
6. [Padrão de JavaDoc](#padrão-de-javadoc)
7. [Padrão de testes](#padrão-de-testes)
8. [Princípios arquiteturais obrigatórios](#princípios-arquiteturais-obrigatórios)
9. [Dúvidas e suporte](#dúvidas-e-suporte)

---

## Pré-requisitos

Antes de começar, certifique-se de ter:

- **Java 21** instalado (OpenJDK ou Eclipse Temurin)
- **Git** configurado com seu nome e e-mail institucional
- Acesso de leitura ao repositório e, se necessário, acesso à rede SES/MS

Não é necessário instalar Maven — use o wrapper incluso:
```bash
./mvnw        # Linux / macOS
mvnw.cmd      # Windows
```

---

## Fluxo de Branches

O projeto usa o fluxo **Git Flow simplificado**:

```
main
 └── develop
      ├── feature/nome-da-funcionalidade
      ├── hotfix/descricao-do-bug
      └── release/1.2.0
```

| Branch | Propósito | Merge para |
|--------|-----------|-----------|
| `main` | Código estável, pronto para produção | — (apenas via PR aprovado) |
| `develop` | Integração contínua das features | `main` (via release) |
| `feature/*` | Nova funcionalidade | `develop` |
| `hotfix/*` | Correção urgente de bug em produção | `main` e `develop` |
| `release/*` | Preparação de nova versão (bump de versão, changelogs) | `main` e `develop` |

### Regras da branch `main`

- Push direto **proibido** — todo código chega via Pull Request
- Requer pelo menos **1 aprovação** de revisor
- Requer **CI passando** (build, testes unitários, contract tests, ArchUnit)
- Sem merge se houver conflitos não resolvidos

### Criando uma branch de feature

```bash
# Sempre a partir de develop atualizado
git checkout develop
git pull origin develop
git checkout -b feature/nome-descritivo-em-kebab-case

# Exemplos corretos:
# feature/qlik-engine-adapter
# feature/sync-incremental-watermark
# feature/endpoint-resumo-atendimentos
```

---

## Padrão de Commits

O projeto adota o padrão **[Conventional Commits](https://www.conventionalcommits.org/pt-br/v1.0.0/)**.

### Formato

```
<tipo>(<escopo>): <descrição curta em imperativo>

[corpo opcional — explica o "por quê", não o "o quê"]

[rodapé opcional — referência a issues, breaking changes]
```

### Tipos permitidos

| Tipo | Quando usar |
|------|------------|
| `feat` | Nova funcionalidade para o usuário final |
| `fix` | Correção de bug |
| `docs` | Alteração apenas em documentação (README, JavaDoc, Wiki) |
| `test` | Adição ou correção de testes (sem alteração de código de produção) |
| `refactor` | Refatoração de código sem mudança de comportamento externo |
| `chore` | Tarefas de manutenção: atualização de dependências, CI, configurações |
| `perf` | Melhoria de performance |
| `style` | Formatação, espaços em branco — sem mudança de lógica |
| `build` | Alterações no sistema de build (`pom.xml`, Dockerfile, etc.) |
| `ci` | Alterações nos pipelines de CI/CD (`.github/workflows/`) |

### Escopos recomendados

| Escopo | Área do código |
|--------|---------------|
| `domain` | `br.gov.ms.saude.ssd.domain` |
| `qlik-rest` | `adapter.out.qlik.rest` |
| `qlik-engine` | `adapter.out.qlik.engine` |
| `mock` | `adapter.out.mock` |
| `etl` | Pipeline de extração, transformação e carga |
| `api` | Controllers REST e DTOs |
| `persistence` | JPA, Flyway migrations |
| `contract-test` | Testes de contrato (`DataSourcePortContractTest`) |
| `arch-test` | Testes de arquitetura (ArchUnit) |
| `ci` | Pipelines GitHub Actions |
| `config` | Beans Spring, profiles, YAML |

### Exemplos corretos

```bash
# Nova funcionalidade
git commit -m "feat(qlik-engine): implementar extração paginada via GetHyperCubeData"

# Correção de bug
git commit -m "fix(etl): corrigir conversão de HR_AGENDAMENTO para LocalTime quando valor é nulo"

# Com corpo explicativo
git commit -m "refactor(domain): extrair cálculo de faixa etária para AgeCalculator

O cálculo estava duplicado em AtendimentoService e FieldTransformerService.
Centraliza a lógica em AgeCalculator (domain/model/), seguindo SRP."

# Breaking change
git commit -m "feat(api)!: alterar contrato do endpoint /atendimentos para paginação obrigatória

BREAKING CHANGE: o parâmetro 'page' passa a ser obrigatório.
Clientes sem paginação receberão HTTP 400.

Refs: #42"

# Referência a issue
git commit -m "fix(persistence): corrigir upsert batch quando lote excede 1000 registros

Closes #38"
```

### O que NÃO fazer

```bash
# ERRADO — sem tipo
git commit -m "adiciona endpoint de atendimentos"

# ERRADO — vago
git commit -m "fix: correção"

# ERRADO — no passado
git commit -m "feat: adicionou QlikEngineAdapter"

# ERRADO — múltiplas responsabilidades em um commit
git commit -m "feat: adapter + fix: bug + refactor: service"
```

---

## Antes de Abrir um Pull Request

Execute **todos** os passos abaixo antes de abrir o PR. O CI fará as mesmas
verificações automaticamente — economize tempo fazendo localmente primeiro.

### 1. Atualizar a branch com `develop`

```bash
git fetch origin
git rebase origin/develop
# ou, se preferir merge:
git merge origin/develop
```

### 2. Executar a suite completa de testes

```bash
# Compila + testes unitários + integração + JaCoCo
./mvnw clean verify

# A saída deve terminar com:
# [INFO] BUILD SUCCESS
```

### 3. Executar os contract tests

```bash
./mvnw test -Pcontract
```

Se você adicionou um novo adaptador, **deve** criar a subclasse do contract test
(`NovoAdaptadorContractTest extends DataSourcePortContractTest`).

### 4. Verificar violações arquiteturais (ArchUnit)

```bash
./mvnw test -Dtest="*ArchitectureTest"
```

### 5. Verificar cobertura mínima

```bash
./mvnw verify
# Abrir: target/site/jacoco/index.html
# Cobertura mínima exigida: 80% nas classes de domínio e serviços
```

### 6. Revisar o JavaDoc

Todos os métodos públicos **novos ou modificados** devem ter JavaDoc completo
(ver [Padrão de JavaDoc](#padrão-de-javadoc) abaixo).

```bash
# Gera o JavaDoc e verifica erros
./mvnw javadoc:javadoc
```

---

## Checklist do Pull Request

O template de PR (`.github/PULL_REQUEST_TEMPLATE.md`) já inclui este checklist.
Certifique-se de marcar cada item antes de solicitar revisão.

- [ ] Testes adicionados ou atualizados para a mudança realizada
- [ ] Contract tests passando (`./mvnw test -Pcontract`)
- [ ] Sem violações SOLID detectadas pelo ArchUnit
- [ ] JavaDoc completo em todos os métodos públicos novos ou modificados
- [ ] `./mvnw clean verify` finaliza com `BUILD SUCCESS`
- [ ] Branch atualizada com `develop` (sem conflitos)
- [ ] Commits seguem o padrão Conventional Commits
- [ ] Documentação atualizada (README, JavaDoc, Wiki) se necessário
- [ ] Sem segredos, senhas ou URLs de produção no código
- [ ] `application-prod.yml` e `application-secrets.yml` não foram criados/modificados

---

## Padrão de JavaDoc

JavaDoc é **obrigatório** em todos os métodos públicos. O padrão mínimo é:

```java
/**
 * Descrição concisa do que o método faz (uma linha, verbo no indicativo).
 *
 * <p>Parágrafo de detalhes quando necessário: explica o "por quê" da implementação,
 * casos especiais, comportamento em situações de erro ou side-effects relevantes.</p>
 *
 * <p>Para adaptadores, descreva o protocolo de comunicação utilizado.</p>
 *
 * @param nomeParam  descrição do parâmetro (o que representa, restrições)
 * @param outroParam descrição do segundo parâmetro
 * @return descrição do valor retornado (nunca retorna nulo? diga isso)
 * @throws TipoDeExcecao quando/por que essa exceção é lançada
 * @see ClasseOuInterfaceRelacionada
 * @since 1.0
 */
public ReturnType nomeDoMetodo(TipoParam nomeParam, TipoParam2 outroParam) { ... }
```

### Exemplos por tipo de classe

#### Interface (Port)

```java
/**
 * Porta de saída principal — define o contrato que qualquer fonte de dados
 * deve cumprir para ser utilizada pelo domínio.
 *
 * <p>Implementações disponíveis:</p>
 * <ul>
 *   <li>{@link QlikRestAdapter} — metadados via HTTP REST público</li>
 *   <li>{@link QlikEngineAdapter} — dados completos via WebSocket + JSON-RPC</li>
 *   <li>{@link MockDataSourceAdapter} — dados estáticos para desenvolvimento offline</li>
 * </ul>
 *
 * <p><strong>Princípio aplicado:</strong> DIP (Dependency Inversion) — o domínio
 * depende desta abstração, nunca das implementações concretas.</p>
 *
 * @see DataSourcePortContractTest
 */
public interface DataSourcePort { ... }
```

#### Adaptador

```java
/**
 * Executa uma extração paginada de dados do Qlik Sense via Engine API (WebSocket).
 *
 * <p>Protocolo: JSON-RPC 2.0 sobre WebSocket. Fluxo de execução:</p>
 * <ol>
 *   <li>Abre conexão WebSocket em {@code wss://{host}/app/{appId}}</li>
 *   <li>Chama {@code OpenDoc} para obter o handle do documento</li>
 *   <li>Chama {@code CreateSessionObject} com a definição do HyperCube</li>
 *   <li>Itera {@code GetHyperCubeData} até esgotar os registros (páginas de 5.000 linhas)</li>
 *   <li>Destrói a sessão e fecha a conexão WebSocket</li>
 * </ol>
 *
 * @param tableName nome da tabela Qlik a extrair (ex.: {@code DB_DIGSAUDE})
 * @param fields    lista de campos a incluir no HyperCube
 * @param options   opções de extração: watermark, tamanho de página, timeout
 * @return {@link ExtractResult} com todos os registros extraídos e metadados da extração
 * @throws DataSourceUnavailableException se não for possível conectar ao servidor Qlik
 * @throws DataExtractionException        se ocorrer erro durante a extração dos dados
 * @see QlikJsonRpcProtocol
 * @see QlikPaginationStrategy
 */
public ExtractResult extractTable(String tableName, List<String> fields, ExtractOptions options) { ... }
```

#### Serviço de aplicação

```java
/**
 * Executa a sincronização incremental dos dados desde o último sync bem-sucedido.
 *
 * <p>Utiliza o campo {@code DT_NEW} como watermark — identifica registros novos
 * ou modificados desde a última execução. É o campo de maior granularidade
 * disponível no dataset Qlik (16.403 valores distintos em mar/2026).</p>
 *
 * <p>Caso não exista registro de sync anterior, delega para
 * {@link #executarFullSync()} automaticamente.</p>
 *
 * @return {@link SyncResult} com contagem de registros inseridos, atualizados e erros
 * @throws SyncAlreadyRunningException se já houver um sync em execução
 * @see ExecutarSyncUseCase
 * @see SyncLog
 */
public SyncResult executarIncrementalSync() { ... }
```

### Regras adicionais de JavaDoc

- Nunca documente o óbvio: `@param id O ID` é inútil
- Documente invariantes: se um método nunca retorna `null`, diga `@return nunca {@code null}`
- Use `{@code valor}` para referenciar código inline
- Use `{@link Classe#metodo}` para referenciar outras classes/métodos
- Enums: documente cada constante com seu significado no contexto de negócio
- Exceções não checadas relevantes: documente com `@throws` mesmo não sendo checadas

---

## Padrão de Testes

### Nomenclatura de testes

```java
// Padrão: nomeDoMetodo_condicao_comportamentoEsperado
@Test
void getObjectData_idInexistente_deveLancarDataExtractionException() { ... }

@Test
void extractTable_comWatermark_deveRetornarApenasRegistrosAposData() { ... }

@Test
void checkHealth_servidorIndisponivel_deveRetornarStatusDown() { ... }
```

### Estrutura interna: Arrange / Act / Assert

```java
@Test
void consultar_comFiltroMunicipio_deveRetornarApenasAtendimentosDaquelesMunicipio() {
    // Arrange
    var filter = AtendimentoFilter.builder().municipio("Campo Grande").build();
    var pageable = PageRequest.of(0, 10);

    // Act
    var resultado = service.consultar(filter, pageable);

    // Assert
    assertThat(resultado.getContent())
        .isNotEmpty()
        .allMatch(a -> a.getMunicipio().equals("Campo Grande"));
}
```

### Contract Tests — obrigatório para novos adaptadores

Todo adaptador que implementa `DataSourcePort` **deve** ter uma subclasse de
`DataSourcePortContractTest`. Isso garante o Princípio de Liskov em tempo de build.

```java
class MeuNovoAdapterContractTest extends DataSourcePortContractTest {

    @Override
    protected DataSourcePort createAdapter() {
        return new MeuNovoAdapter(configuracaoDeTeste());
    }
}
```

---

## Princípios Arquiteturais Obrigatórios

O ArchUnit (`ArchitectureTest`) valida automaticamente as seguintes regras no CI:

| Regra | Descrição |
|-------|-----------|
| Domain sem frameworks | Classes em `domain.*` não podem importar `org.springframework.*` ou `javax.*` |
| Domínio sem adaptadores | `domain.*` não pode depender de `adapter.*` |
| Controller sem repositório | `adapter.in.rest.*` não pode acessar `adapter.out.persistence.*` diretamente |
| Adaptadores isolados | Um adaptador não pode depender de outro adaptador |
| Serviços via interface | `application.service.*` deve depender de interfaces de `domain.port.*`, nunca de implementações |

Se o CI falhar nos testes de arquitetura, **o PR não pode ser mergeado**.

---

## Dúvidas e Suporte

- **Issues:** use os templates em `.github/ISSUE_TEMPLATE/` para reportar bugs ou propor funcionalidades
- **Dúvidas técnicas:** abra uma issue com o label `pergunta`
- **Urgências:** entre em contato com a equipe da Superintendência de Saúde Digital — SES/MS

---

*Documento mantido pela equipe de desenvolvimento — Superintendência de Saúde Digital, SES/MS.*
