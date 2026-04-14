---
name: "Relatório de bug"
about: "Relate um comportamento incorreto ou inesperado no sistema"
title: "fix: "
labels: ["bug"]
assignees: []
---

## Descrição do problema

<!-- Descreva o que está acontecendo de forma clara e objetiva.
     O que você esperava que acontecesse? O que aconteceu de fato? -->

**Comportamento esperado:**
<!-- O que deveria acontecer -->

**Comportamento observado:**
<!-- O que está acontecendo de fato -->

---

## Como reproduzir

<!-- Passo a passo para reproduzir o problema. Seja o mais específico possível. -->

1. 
2. 
3. 
4. O erro ocorre

---

## Ambiente

| Campo | Valor |
|-------|-------|
| Profile ativo | `dev` / `test` / `prod` |
| Adaptador configurado | `qlik-rest` / `qlik-engine` / `mock` |
| Versão Java | ex.: `21.0.3` |
| Sistema operacional | ex.: `Windows 11` / `Ubuntu 22.04` |
| Branch / commit | ex.: `develop` / `abc1234` |

---

## Logs e evidências

<!-- Cole aqui logs relevantes, stacktraces ou a resposta de erro do endpoint.
     Use blocos de código para formatar corretamente. -->

```
Cole o log / stacktrace aqui
```

**Endpoint (se aplicável):**
```
GET /api/v1/...
Resposta HTTP: 
```

---

## Componente afetado

<!-- Marque todos que se aplicam -->

- [ ] Conexão com Qlik Sense (REST ou WebSocket)
- [ ] Pipeline ETL (extração, transformação ou carga)
- [ ] Banco de dados H2 / Flyway migrations
- [ ] Endpoints da API REST
- [ ] Agendamento / sync automático
- [ ] Testes (unitários, contract tests, ArchUnit)
- [ ] Pipeline CI/CD
- [ ] Documentação
- [ ] Outro: ___________

---

## Impacto

<!-- Qual é o impacto deste bug no funcionamento do sistema? -->

- [ ] **Crítico** — impede completamente o uso do sistema ou perde dados
- [ ] **Alto** — funcionalidade principal comprometida, sem contorno
- [ ] **Médio** — funcionalidade comprometida, mas existe contorno
- [ ] **Baixo** — problema estético ou de usabilidade, não afeta dados

---

## Possível causa (se souber)

<!-- Se você tiver hipótese sobre a causa do bug, descreva aqui.
     Ajuda o desenvolvedor a investigar mais rapidamente. -->

---

## Contexto adicional

<!-- Qualquer outra informação que possa ajudar: frequência do problema,
     se é intermitente, se ocorre apenas com certos dados, etc. -->

- Frequência: sempre / intermitente / apenas uma vez
- Observações: 

---

## Referências

<!-- Links para documentação, código relacionado ou issues relacionadas -->

- Issue relacionada: #
- Arquivo suspeito: `src/...`
