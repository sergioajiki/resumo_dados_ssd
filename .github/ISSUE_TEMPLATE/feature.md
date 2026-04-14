---
name: "Nova funcionalidade"
about: "Proposta de nova funcionalidade ou melhoria no projeto"
title: "feat: "
labels: ["enhancement"]
assignees: []
---

## Resumo da funcionalidade

<!-- Descrição clara e objetiva do que você quer que o sistema faça.
     Uma ou duas frases no formato: "Como [papel], quero [ação] para [benefício]." -->

**Como** [gestor de saúde / desenvolvedor / sistema externo],  
**quero** [descreva a ação],  
**para que** [descreva o benefício ou problema resolvido].

---

## Contexto e justificativa

<!-- Por que esta funcionalidade é necessária?
     Relate o problema ou oportunidade que motivou esta proposta.
     Se aplicável, cite dados do Telessaúde MS ou da Superintendência de Saúde Digital. -->

---

## Comportamento esperado

<!-- Descreva como o sistema deve se comportar após a implementação.
     Use exemplos concretos quando possível. -->

### Antes

<!-- Como o sistema se comporta hoje (se aplicável) -->

### Depois

<!-- Como o sistema deve se comportar com a funcionalidade implementada -->

---

## Critérios de aceite

<!-- Lista objetiva de condições que devem ser verdadeiras para a funcionalidade ser considerada pronta.
     Use uma checkbox por critério. -->

- [ ] 
- [ ] 
- [ ] 

---

## Impacto arquitetural

<!-- Esta funcionalidade afeta algum dos pontos abaixo? Marque todos que se aplicam. -->

- [ ] Interface `DataSourcePort` ou outros ports do domínio
- [ ] Adição ou modificação de adaptador (Qlik REST / Qlik Engine / Mock / novo)
- [ ] Pipeline ETL (extração, transformação, carga)
- [ ] Schema do banco de dados (nova migration Flyway necessária)
- [ ] Endpoints da API REST (`/api/v1/...`)
- [ ] Configuração YAML / profiles (`dev`, `test`, `prod`)
- [ ] Pipeline CI/CD (`.github/workflows/`)
- [ ] Sem impacto arquitetural significativo

---

## Referências e materiais de apoio

<!-- Links úteis: documentação do Qlik Sense, APIs externas, issues relacionadas, ADRs, etc. -->

- [ ] Documentação relacionada em `docs/`
- Issue relacionada: #
- Referência externa: 

---

## Fase do projeto

<!-- Em qual fase do cronograma esta funcionalidade se encaixa? -->

- [ ] Fase 1 — Estrutura base
- [ ] Fase 2 — Domain Model e contratos
- [ ] Fase 3 — MockAdapter
- [ ] Fase 4 — Contract Tests
- [ ] Fase 5 — QlikRestAdapter
- [ ] Fase 6 — QlikEngineAdapter (WebSocket)
- [ ] Fase 7 — ETL Pipeline
- [ ] Fase 8 — API REST própria
- [ ] Fase 9 — Documentação
- [ ] Fase 10 — Interface de verificação
- [ ] Fora do cronograma atual — requer discussão
