# Campos Disponíveis — Núcleo de Telessaúde MS
**Fonte:** API Qlik Sense — App `10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb`
**Data da análise:** 2026-04-13

---

## RESUMO EXECUTIVO

| Categoria solicitada | Situação | Observação |
|---------------------|----------|------------|
| Cidades atendidas | ✅ Disponível | NOME_MUNICIPIO, IBGE_ATEND |
| Especialidades | ✅ Disponível | NOME_ESPECIALIDADE (16 valores) |
| Médicos | ✅ Disponível | NOME_MEDICO, CBO_MEDICO |
| Datas de atendimento | ✅ Disponível | DT_AGENDAMENTO, data_agendamento |
| Horários | ✅ Disponível | HR_AGENDAMENTO (81 valores) |
| Faixa etária do paciente | ⚠️ Parcial | Apenas DT_NASC_PACIENTE — faixa deve ser calculada |
| Sexo/Gênero | ❌ Ausente | Nenhum campo encontrado no dataset |
| Raça/Cor | ✅ Disponível | RACA_PACIENTE (5 valores) |
| Etnia | ✅ Disponível | ETNIA (9 valores) |
| CPF | ❌ Ausente | Nenhum campo encontrado no dataset |
| CNS | ✅ Disponível | CNS_PACIENTE (8.518 valores) |

---

## CAMPOS DISPONÍVEIS POR CATEGORIA

---

### 1. CIDADES ATENDIDAS

**Tabela: DB_DIGSAUDE**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `NOME_MUNICIPIO` | texto | 49 | Nome do município do atendimento |
| `IBGE_ATEND` | numérico | — | Código IBGE do município do atendimento |
| `TIPO_ZONA` | texto | — | Zona (urbana/rural) |

**Tabela: TEMPDB_USER (municípios dos profissionais)**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `MUNICIPIO_USER` | texto | 60 | Município do profissional |
| `ID_MUNICIPIO` | numérico | 60 | ID do município do profissional |
| `COD_IBGE` | numérico | — | Código IBGE do município do profissional |

**Tabela: MUN_PILOTO**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `MUNICIPIOCHECK` | texto | — | Municípios no programa piloto |
| `PILOTO_TF` | texto | — | Flag de participação no piloto |
| `DEMANDANTE` | texto | — | Demandante do município piloto |
| `DT_TREINAMENTO` | texto | 37 | Data de treinamento do município |

**Tabela: MAPS_OFF**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `co_MUNICIPIO` | texto | 79 | Código do município (para mapa) |
| `co_IBGE` | numérico | — | Código IBGE para mapa |

---

### 2. ESPECIALIDADES DISPONÍVEIS

**Tabela: TEMPDB_USER**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `NOME_ESPECIALIDADE` | texto | 16 | Nome da especialidade médica |
| `ESPECIALIDADE` | numérico | 16 | Código da especialidade |

**Tabela: DB_DIGSAUDE**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `CBO_MEDICO` | texto | 14 | Código Brasileiro de Ocupações do médico |
| `DESCRICAO_CONSULTA` | texto | 12.165 | Descrição/especialidade da consulta |
| `TIPO_SERV_ID` | numérico | — | Tipo de serviço prestado |

---

### 3. MÉDICOS DISPONÍVEIS

**Tabela: DB_DIGSAUDE**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `NOME_MEDICO` | texto | 28 | Nome do médico |
| `ID_MEDICO` | numérico | 28 | ID do médico |
| `CBO_MEDICO` | texto | 14 | CBO do médico |

**Tabela: TEMPDB_USER**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `ID_USER` | numérico | 9.213 | ID do profissional |
| `NOME_USER` | texto | — | Nome do profissional |
| `CRM_USER` | texto | — | CRM do profissional |
| `EMAIL` | texto | — | E-mail do profissional |
| `FONE` | texto | — | Telefone do profissional |
| `TP_USER` | texto | 7 | Tipo de usuário/profissional |
| `NOME_ESPECIALIDADE` | texto | 16 | Especialidade do profissional |
| `IDADE` | numérico | 108 | Idade do profissional |
| `RANGE_IDADE` | texto | 5 | Faixa etária do profissional |
| `RACA_COR_USER` | texto | 5 | Raça/cor do profissional |
| `estrangeiro` | texto | — | Flag de profissional estrangeiro |
| `DT_INGRESSO_ESTRANG_USER` | texto | — | Data de ingresso (estrangeiro) |

---

### 4. DATAS E HORÁRIOS DE ATENDIMENTO

**Tabela: DB_DIGSAUDE**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `DT_AGENDAMENTO` | timestamp | 306 | Data do agendamento |
| `data_agendamento` | timestamp | 306 | Data do agendamento (duplicado) |
| `HR_AGENDAMENTO` | numérico | 81 | Horário do agendamento |
| `DT_SOLICITACAO` | texto | 341 | Data da solicitação do atendimento |
| `DT_SOLICITACAO_MES_ANO` | texto | 16 | Mês/Ano da solicitação |
| `DT_NEW` | timestamp | 16.403 | Data de criação/atualização do registro |
| `ANO_AGENDA` | numérico | 2 | Ano do agendamento |
| `MES_AGENDA` | numérico | 12 | Mês do agendamento (numérico) |
| `MES_NUM_AGENDA` | numérico | — | Número do mês do agendamento |

**Tabela: USERJORNADA (jornada de atendimentos dos profissionais)**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `DT_ATENDIMENTO` | timestamp | 332 | Data do atendimento na jornada |
| `ANO_VAGAS` | numérico | 2 | Ano das vagas disponíveis |
| `MES_VAGAS` | numérico | 12 | Mês das vagas disponíveis |
| `MES_NUM_VAGAS` | numérico | 12 | Número do mês das vagas |
| `VAGAS` | numérico | 10 | Quantidade de vagas |
| `ID_JORNADA` | numérico | 5.310 | ID da jornada |

---

### 5. DADOS DO PACIENTE

**Tabela: DB_DIGSAUDE**
| Campo | Tipo | Cardinalidade | Disponível |
|-------|------|--------------|------------|
| `ID_ATENDIMENTO` | numérico | 16.426 | ✅ ID único do atendimento |
| `CNS_PACIENTE` | texto | 8.518 | ✅ Cartão Nacional de Saúde |
| `DT_NASC_PACIENTE` | texto | 7.274 | ✅ Data de nascimento (calcular faixa etária) |
| `RACA_PACIENTE` | texto | 5 | ✅ Raça/cor do paciente |
| `ETNIA` | texto | 9 | ✅ Etnia do paciente |
| `TELEFONE` | texto | 7.370 | ✅ Telefone do paciente |
| `CEP_PACIENTE` | texto | 1.202 | ✅ CEP do paciente |
| `RUA_PACIENTE` | texto | 2.958 | ✅ Logradouro do paciente |
| `NUM_PACIENTE` | texto | 1.920 | ✅ Número do endereço |
| `BAIRRO_PACIENTE` | texto | 1.016 | ✅ Bairro do paciente |
| `COMPLEMENTO_END_PACIENTE` | texto | 219 | ✅ Complemento do endereço |
| `ENDERECO_COMPLETO` | texto | 5.453 | ✅ Endereço completo formatado |
| **CPF** | — | — | ❌ **NÃO ENCONTRADO** |
| **SEXO / GÊNERO** | — | — | ❌ **NÃO ENCONTRADO** |

---

### 6. STATUS E DESFECHO DOS ATENDIMENTOS

**Tabela: DB_DIGSAUDE**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `STATUS_CONSULTA` | texto | 7 | Status atual da consulta |
| `DESFECHO_ATEND` | texto | 8 | Desfecho do atendimento |
| `CLASSFIC_COR` | texto | — | Classificação por cor/prioridade |
| `CLASSIF_CONCLUSAO` | texto | 2 | Classificação da conclusão |
| `TP_NW_CONCLUSAO` | texto | — | Tipo de conclusão |
| `CID_CONSULTA` | texto | 1.233 | CID da consulta |
| `ID_DIGSAUDE_REF` | numérico | — | Referência interna |

---

### 7. SUPORTE / INCIDENTES

**Tabela: INCID_SUPORTE**
| Campo | Tipo | Cardinalidade | Descrição |
|-------|------|--------------|-----------|
| `ID_INCDSUPORTE` | numérico | 314 | ID do incidente |
| `ANO_INCDSUPORTE` | numérico | — | Ano do incidente |
| `MES_INCDSUPORTE` | texto | — | Mês do incidente |
| `MES_NUM_INCDSUPORTE` | numérico | — | Número do mês do incidente |
| `DT_INCIDENTE` | texto | 129 | Data do incidente |
| `DT_SOL_INCIDENTE` | texto | 129 | Data de solicitação do incidente |
| `TECNICO` | texto | 6 | Técnico responsável |
| `TEC_RESP_SOLUCAO` | texto | 6 | Técnico responsável pela solução |
| `ESPEC_ERRO` | texto | — | Especificação do erro |
| `INCIDENTE` | texto | — | Descrição do incidente |
| `STATUS` | texto | — | Status do incidente |
| `CLASSIFICACAO` | texto | — | Classificação do incidente |

---

## CAMPOS AUSENTES — AÇÃO NECESSÁRIA

### ❌ CPF do Paciente
- **Situação:** Nenhum campo de CPF encontrado em nenhuma tabela
- **Impacto:** Impossível identificar paciente por CPF via esta API
- **Alternativas:**
  - Usar `CNS_PACIENTE` como identificador único (disponível)
  - Verificar se CPF existe em outra fonte de dados (banco transacional SES)
  - Solicitar inclusão do campo no próximo reload do app Qlik

### ❌ Sexo / Gênero do Paciente
- **Situação:** Nenhum campo de sexo/gênero encontrado em nenhuma tabela
- **Impacto:** Impossível segmentar atendimentos por sexo via esta API
- **Alternativas:**
  - Verificar se o campo existe na fonte original (sistema DigiSaúde/FIOCRUZ)
  - Solicitar inclusão no script de carga do app Qlik
  - Derivar de outro sistema via CNS (integração futura)

---

## CAMPOS QUE REQUEREM TRATAMENTO

| Campo | Problema | Solução |
|-------|----------|---------|
| `DT_NASC_PACIENTE` | Data de nascimento bruta — sem faixa etária | Calcular `RANGE_IDADE` no backend |
| `HR_AGENDAMENTO` | Formato numérico (não timestamp) | Converter para HH:mm |
| `DT_AGENDAMENTO` e `data_agendamento` | Campos duplicados | Usar `DT_AGENDAMENTO` como canônico |
| `ESPECIALIDADE` | Código numérico sem label | JOIN com `NOME_ESPECIALIDADE` |
| `CBO_MEDICO` | Código CBO sem descrição | Lookup na tabela CBO do DATASUS |
| `CNES_NESTABELECIMENTO` | Código CNES sem nome | Lookup no CNES/DATASUS |

---

## TABELAS E VOLUMES

| Tabela | Registros | Função |
|--------|-----------|--------|
| DB_DIGSAUDE | 16.426 | Atendimentos realizados |
| TEMPDB_USER | 9.213 | Profissionais/usuários cadastrados |
| LINK | 22.325 | Ligação entre atendimentos e usuários |
| USERJORNADA | 5.310 | Jornadas de atendimento dos profissionais |
| MUN_PILOTO | 68 | Municípios no programa piloto |
| MUNAPROV_PILOTO | 207 | Municípios aprovados no piloto |
| MUN_SEMATIVIDADE | 68 | Municípios sem atividade |
| INCID_SUPORTE | 314 | Incidentes de suporte técnico |
| MAPS_OFF | 79 | Municípios para mapa (offline) |

---

## REFERÊNCIAS

- Análise completa da página: `../saude-digital-analise.md`
- Arquitetura DataSource: `./arquitetura-datasource.md`
- API metadata: `https://paineispublicos.saude.ms.gov.br/api/v1/apps/10f9b380-d7a4-426c-ae4e-8f6b7d3bd3fb/data/metadata`
