# Decisao de qualidade

**Data:** 2026-11-14 UTC
**Repositorio:** [PabloQAntj/projeto_qualidade_software](https://github.com/WesMarquess/projeto_qualidade_software)
**Branch avaliada:** `release/rc1-blocker-fixes`
**SHA submetido a verificacao local:** `26f678f7e1b6b58a2984e75c841f13e0e3243ef0`
**PR:** [#20](https://github.com/WesMarquess/projeto_qualidade_software/pull/20), base `main`, ainda sem merge nesta verificacao.
**Tag avaliada:** nenhuma.

## Decisao

**BLOQUEAR.** A verificacao local desta branch passou, mas nao comprova merge
nem CI verde na `main`. Alem disso, requisitos funcionais obrigatorios seguem
parciais ou pendentes, e a analise SonarCloud/Quality Gate desta revisao nao foi
comprovada. Esta decisao nao declara o sistema pronto para producao.

## Criterios e evidencias

| Criterio | Resultado | Evidencia |
|---|---|---|
| 14 requisitos da especificacao rastreados | **Parcialmente atendido** | [RTM canonica](RTM.md): 3 Implementados, 8 Parciais e 3 Pendentes. Todos os 6 criterios criticos (autenticacao/perfis, estados, sobreposicao, concorrencia, aprovacao e auditoria) estao registrados com risco, teste e evidencia: 6/6 (100%). Isso mede rastreabilidade, nao conclusao funcional. |
| Autorizacao | **Parcial** | `./mvnw -B clean verify` executou `AutenticacaoRotasControllerTest.getApiSemSessaoRetorna401`, `AutenticacaoRotasControllerTest.mutacaoComCsrfMasSemLoginRetorna401` e os casos de perfil/CSRF; inclui `GET /api/reservas/pendentes`. 401 sem sessao passou localmente. |
| Sobreposicao e dupla reserva | **Parcial** | E1 executou testes Testcontainers/PostgreSQL para criacao concorrente, aprovacao concorrente e alteracao concorrente. A implementacao recusa conflito com outra solicitacao `SOLICITADA`, divergindo da RN03; a RTM marca #6 Parcial. |
| Aprovacao restrita | **Implementado no recorte atual** | E1 executou testes de servico, PostgreSQL e HTTP: somente Responsavel aprova; Administrador recebe 403. A aprovacao de alteracoes de horario continua pendente. |
| Auditoria e estados | **Parcial** | E1 executou persistencia/consulta de auditoria e transicoes cobertas. Auditoria de alteracoes de horario e alguns fluxos/expiracoes ainda estao ausentes; ver #4 e #10 na RTM. |
| Cobertura de linhas >= 80% | **Atendido localmente** | `./mvnw -B clean verify`, executado nesta branch em 2026-11-14: 991/1106 linhas = **89,60%**. |
| Cobertura de branches >= 70% | **Atendido localmente** | Mesmo comando: 242/322 branches = **75,16%**. |
| Testes automatizados | **Passou localmente** | `./mvnw -B clean verify`: BUILD SUCCESS, 222 testes Java, 0 falhas, 0 erros, 0 ignorados. `node --test tests/*.test.js`: 19 aprovados, 0 falhas. |
| 0 bugs/vulnerabilidades criticas | **Nao comprovado** | Nao houve analise SonarCloud nova nem Quality Gate verificavel para o SHA avaliado. Nao declarar zero bugs ou vulnerabilidades. |
| Desempenho | **Nao reproduzido nesta sessao** | `target/jmeter/` nao existe. [Resultado JMeter](performance/resultado-01.md) e historico; nao prova capacidade atual ou de producao. |
| Inicializacao limpa | **Passou nesta branch** | E1 executou `AplicacaoBancoLimpoPostgresTest` com Testcontainers; a inicializacao e migrations em PostgreSQL vazio passaram. A CI na `main` nao foi comprovada para essa revisao. |
| CI e integracao na `main` | **Nao atendido / nao verificado para este SHA** | PR #20 continua aberto e sem merge observado; os resultados locais nao sao CI da `main`. |

### Registros operacionais da verificacao

- E1: `./mvnw -B clean verify` — BUILD SUCCESS; 222 testes Java, zero falhas,
  erros ou ignorados; JaCoCo 89,60% linhas e 75,16% branches. Data: 2026-11-14
  UTC; branch `release/rc1-blocker-fixes`.
- E2: `node --test tests/*.test.js` — 19 testes aprovados, zero falhas. Data:
  2026-11-14 UTC; mesma branch.
- O log do Surefire emitiu o aviso “going to kill self fork JVM” 30 segundos
  apos `System.exit(0)`. Nao foi localizado `System.exit` no codigo; uma thread
  ou recurso ativo no encerramento e apenas hipotese, sem causa confirmada.
- Nao apareceu `Connection refused` no log da execucao E1 apos configurar stubs
  de notificacao nos testes que nao exercitam a integracao.
- JMeter nao foi executado nesta sessao. O resumo historico permanece
  identificado como tal em `performance/resultado-01.md`.

## Riscos

**Riscos formalmente aceitos: nenhum.** A decisao e bloquear; portanto nao ha
aceite de risco de release.

| Risco aberto | Impacto | Justificativa | Responsavel pelo tratamento |
|---|---|---|---|
| Requisitos funcionais parciais/pendentes (#1–#4, #6, #8–#13) | Alto: fluxos e controles do PRD nao estao disponiveis ou divergem das regras. | Especificados individualmente na RTM; testes/cobertura nao substituem funcionalidade ausente. | Equipe e responsavel de produto, a designar nominalmente. |
| Branch nao integrada nem validada na `main` | Alto: nao ha evidencias da versao candidata no ramo de entrega. | Nao foi verificado merge e CI no SHA final da `main`. | Mantenedor/autor do PR e revisor independente, a confirmar. |
| Seguranca sem Quality Gate atual | Alto e desconhecido: vulnerabilidades podem permanecer. | SonarCloud nao executado/comprovado para esta revisao; zero criticos nao pode ser afirmado. | Responsavel pelas configuracoes SonarCloud/CI, a designar. |
| Desempenho sem nova medicao | Medio/alto e desconhecido: latencia/capacidade sob carga atual nao foram medidas. | Baseline existente e historico, local e sem equivalencia com producao. | Responsavel de desempenho/operações, a designar. |
| Autoria/autodeclaracoes incompletas | Medio: a equipe/docente nao tem confirmacao individual de nomes e contribuicoes. | O historico Git identifica autores, nao substitui roster oficial ou declaracoes assinadas. | Coordenacao da equipe e cada integrante. |

## Autoria a confirmar pelos integrantes

Consulta executada: `git shortlog -sne --all`. Os aliases foram correlacionados
por identidade Git; emails foram omitidos neste relatorio. Os numeros sao
commits no historico local, nao uma medida de esforco. Nao ha roster oficial no
repositorio; cada pessoa deve confirmar nome completo e contribuicao, sem tratar
as linhas abaixo como declaracoes ja assinadas.

| Identidade Git (commits) | Nome completo confirmado pelo integrante | Contribuicao declarada pelo integrante |
|---|---|---|
| PabloQAntj (38) | A confirmar | A preencher e confirmar em primeira pessoa. |
| Weslley Marques / WesMarquess (22) | A confirmar | A preencher e confirmar em primeira pessoa. |
| PabloFerds / Pablo (17) | A confirmar | A preencher e confirmar em primeira pessoa. |
| Pedro de Paula (11) | A confirmar | A preencher e confirmar em primeira pessoa. |
| Daniel (3) | A confirmar | A preencher e confirmar em primeira pessoa. |
| Copilot (1) | Nao se aplica: automacao, nao integrante humano | Nao substitui contribuicao/autoria de estudante. |

## Condicao para reavaliar

Manter **BLOQUEAR** ate que (1) a equipe resolva ou delimite formalmente as
pendencias funcionais, (2) o PR seja revisado independentemente e integrado na
`main`, (3) seja observado um workflow verde no SHA resultante da `main`, e
(4) a equipe confirme as declaracoes de autoria. Somente depois de verificar
esses pontos e revisar o Quality Gate pode-se considerar **LIBERAR COM
RESSALVAS**, com riscos remanescentes, impacto e responsaveis nomeados. Sem
evidencia de merge e CI verde na `main`, a decisao nao muda.

## Texto para o formulario do Hub

Repositorio publico: https://github.com/WesMarquess/projeto_qualidade_software
— branch avaliada: `release/rc1-blocker-fixes`, SHA
`26f678f7e1b6b58a2984e75c841f13e0e3243ef0`; **tag avaliada: nenhuma**.
Decisao: **BLOQUEAR**, ate resolver as condicoes acima.
