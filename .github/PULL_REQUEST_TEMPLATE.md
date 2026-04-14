## Descrição

<!-- O que foi feito e por quê? Contextualize a mudança para o revisor.
     Exemplo: "Implementa o QlikEngineAdapter para extração via WebSocket
     porque o QlikRestAdapter só expõe metadados, não os dados reais dos objetos." -->

**Tipo de mudança:**
- [ ] Nova funcionalidade (`feat`)
- [ ] Correção de bug (`fix`)
- [ ] Refatoração sem mudança de comportamento (`refactor`)
- [ ] Documentação (`docs`)
- [ ] Manutenção / infraestrutura (`chore`, `ci`, `build`)

**Issue relacionada:** <!-- Fecha #NNN / Refs #NNN -->

---

## O que foi alterado

<!-- Liste os principais arquivos/classes modificados e o motivo.
     Ajuda o revisor a navegar pelo PR com contexto. -->

- 
- 
- 

---

## Checklist

### Testes
- [ ] Testes unitários adicionados ou atualizados para a mudança
- [ ] Contract tests passando: `./mvnw test -Pcontract`
- [ ] Se novo adaptador: subclasse de `DataSourcePortContractTest` criada
- [ ] `./mvnw clean verify` finaliza com `BUILD SUCCESS` localmente

### Qualidade de código
- [ ] Sem violações SOLID detectadas pelo ArchUnit (`./mvnw test -Dtest="*ArchitectureTest"`)
- [ ] JavaDoc completo em todos os métodos públicos novos ou modificados
- [ ] Cobertura de código não regrediu (verificar relatório JaCoCo em `target/site/jacoco/`)
- [ ] Sem código comentado ou `TODO` não rastreado por issue

### Segurança e configuração
- [ ] Sem segredos, senhas, tokens ou URLs de produção no código versionado
- [ ] `application-prod.yml` e `application-secrets.yml` **não** foram incluídos
- [ ] Variáveis de ambiente sensíveis usam `${NOME_VAR}` no YAML

### Arquitetura
- [ ] Domínio não importa classes de `adapter.*` ou frameworks Spring/JPA
- [ ] Controllers não acessam repositórios JPA diretamente (respeitam a cadeia de use cases)
- [ ] Novo adaptador usa `@ConditionalOnProperty` — não interfere nos outros adaptadores
- [ ] Se novo adaptador: documentação de "Como trocar o adaptador" no README atualizada

### Documentação
- [ ] README atualizado se o comportamento externo mudou (novos endpoints, novo profile, etc.)
- [ ] CONTRIBUTING.md atualizado se novos padrões foram estabelecidos
- [ ] Commits seguem o padrão Conventional Commits

---

## Como testar esta mudança

<!-- Descreva o passo a passo para o revisor reproduzir e validar a funcionalidade.
     Inclua: comandos, profiles necessários, dados de exemplo, endpoints. -->

```bash
# Exemplo:
./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# Verificar o resultado:
curl -s http://localhost:8080/api/v1/health | jq .
```

---

## Impacto e riscos

<!-- Há impacto em outras áreas do sistema? Algum risco de regressão?
     Se não houver, escreva "Nenhum impacto identificado." -->

---

## Screenshots / evidências (se aplicável)

<!-- Para mudanças em endpoints REST: resposta de exemplo.
     Para mudanças de comportamento: logs relevantes.
     Pode usar blocos de código ao invés de imagens quando possível. -->
