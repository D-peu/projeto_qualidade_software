# Relatorio final de qualidade

**Data da verificacao local:** 2026-11-14 UTC
**Branch:** `release/rc1-blocker-fixes`
**SHA testado e enviado:** `9cad46750b65d4de3a5d56823c270de6fa5c2fdb`
**Decisao:** **BLOQUEAR** — ver [decisao de qualidade](decisao-qualidade.md).
**Escopo:** verificacao local do estado desta branch; nao e aceite de producao,
CI da `main`, analise SonarCloud ou revisao independente.

## Resultados objetivos

| Meta/criterio | Alvo | Resultado observado | Avaliacao |
|---|---:|---:|---|
| Cobertura de linhas JaCoCo | >= 80% | 89,60% (991/1106) | Atingida localmente |
| Cobertura de branches JaCoCo | >= 70% | 75,16% (242/322) | Atingida localmente |
| Requisitos criticos rastreados na RTM | 100% | 6/6 (100%) com criticidade, risco, teste e evidencia | Atingida como rastreabilidade; nao significa implementacao completa |
| Bugs/vulnerabilidades criticas | 0 | Nao comprovado: sem analise SonarCloud/Quality Gate nova para este SHA | Nao avaliavel |
| Testes Java | — | `./mvnw -B clean verify`: BUILD SUCCESS, 222 testes, 0 falhas, 0 erros, 0 ignorados | Passou localmente |
| Testes Node | — | `node --test tests/*.test.js`: 19 aprovados, 0 falhas | Passou localmente |
| Sintaxe do contrato OpenAPI | — | YAML carregado; 21 paths e 23 operacoes | Passou; nao e validacao de conformidade endpoint a endpoint |
| Aplicacao em clone limpo | — | PostgreSQL pelo README; `GET /` e `/api/auth/csrf` responderam 200 | Passou; ambiente local usou JDK 25.0.5 |
| CI do PR #20 | — | [Run 43534327707](https://github.com/WesMarquess/projeto_qualidade_software/actions/runs/43534327707), SHA `9cad467`; Java 21, Verify, Node e upload do artifact passaram | Passou no PR, nao na `main`; SonarCloud ignorado |
| Carga JMeter nesta sessao | — | Nao reproduzida; `target/jmeter/` ausente | Sem evidencia atual |

O XML `target/site/jacoco/jacoco.xml` foi calculado pelos counters de nivel
raiz: `LINE` 991 cobertos de 1106 e `BRANCH` 242 cobertos de 322. A validacao
local do clone usou JDK 25.0.5, disponivel no ambiente; o workflow remoto
configurou Java 21 e passou para o mesmo SHA do PR. Nenhum resultado e da
`main`.

## Requisitos

A [RTM canonica](RTM.md) mapeia a especificacao academica aos requisitos RF/RN
do projeto, riscos, testes e evidencias. Estado atual:

- **Implementado (3):** #5 sobreposicao; #7 aprovacao por Responsavel no fluxo
  coberto; #14 documentacao da API.
- **Parcial (8):** #1 autenticacao/perfis; #2 cadastro/consulta; #3 pesquisa;
  #4 operacoes/ciclo de estados; #6 protecao concorrente conforme limite RN03;
  #8 manutencao; #10 auditoria; #13 interface/mensagens.
- **Pendente (3):** #9 retirada/devolucao; #11 notificacao simulada interna;
  #12 relatorios de utilizacao.

Status “Implementado” e limitado ao recorte/teste descrito na RTM. A
implementacao de notificacao HTTP com WireMock nao satisfaz RF19/RN11, que pede
notificacao simulada dentro da aplicacao.

## Execucoes e observacoes

| Data (UTC) | Comando | Resultado |
|---|---|---|
| 2026-11-14 | `./mvnw -B clean verify` em clone limpo de `9cad467` | BUILD SUCCESS; 222 Java; 0 falhas/erros/ignorados; PostgreSQL/Testcontainers, WireMock e JaCoCo executados |
| 2026-11-14 | `node --test tests/*.test.js` em clone limpo | 19 testes; 19 aprovados; 0 falhas |
| 2026-11-14 | Validacao local de `docs/openapi.yaml` com PyYAML | YAML valido; 21 rotas (`paths`), 23 operacoes |
| 2026-11-14 | `git shortlog -sne --all` | Historico consultado para autoria; alias de identidade correlacionados; confirmar roster na secao de autoria da decisao |
| 2026-11-14 | CI do PR #20, run 43534327707 | SHA `9cad467`; Java 21; Verify, Node e upload de artifact passaram; SonarCloud ignorado |

O Surefire emitiu o aviso “going to kill self fork JVM” depois de 30 segundos.
Nao foi encontrado `System.exit` no codigo. Uma thread nao daemon/recurso de
encerramento e somente hipotese; causa nao confirmada. O log da execucao nao
mostrou `Connection refused` depois que testes alheios a notificacao passaram
a usar stub de `NotificacaoClient`.

O JMeter nao foi reproduzido nesta sessao porque `target/jmeter/` nao existe.
Os dados em [resultado-01.md](performance/resultado-01.md) sao historicos,
locais e exploratorios; nao representam capacidade de producao. SonarCloud,
Quality Gate, revisao independente e workflow no SHA final da `main` nao foram
comprovados nesta verificacao.

## Pendencias funcionais e riscos remanescentes

1. **RF03/RF05 (#2/#3):** CRUD completo de professores/materiais e pesquisa por
   tipo, localizacao e competencia; filtros existentes cobrem apenas recorte
   de sala.
2. **RF06/RF10/RF15 (#4):** aprovacao de alteracoes de horario, limites de
   cancelamento e expiracao de alteracoes pendentes.
3. **RF09/RN03 (#6):** permitir solicitacoes pendentes conflitantes e garantir
   decisao exclusiva ao aprovar; o comportamento atual recusa o conflito ja na
   criacao.
4. **RF04/RF11 (#8):** bloqueios/manutencoes por periodo e alertas.
5. **RF13 (#9):** retirada e devolucao de materiais.
6. **RF14 (#10):** historico integral de alteracoes de horario.
7. **RF19/RN11 (#11):** notificacao simulada interna, em vez de depender do
   cliente HTTP externo atual.
8. **RF16 (#12):** relatorios de utilizacao, carga horaria e conflitos evitados.
9. **RF17 (#13):** validacao E2E em navegador, responsividade, acessibilidade
   e inspecao visual; testes Node nao a substituem.
10. **Autorizacao (#1):** falta cobertura integrada de jornadas para cada rota,
    perfil, sessao, CSRF e posse; os casos sem sessao presentes passaram.
11. **Integracao/CI:** sem merge e CI verde comprovados na `main` para o SHA
    avaliado; SonarCloud nao comprovado.
12. **Desempenho:** baseline historico, sem repeticao atual, sem medicao de
    infraestrutura e sem metas oficiais aprovadas.
13. **Autoria:** roster oficial e confirmacoes individuais pendentes; ver
    espacos de confirmacao em [decisao-qualidade.md](decisao-qualidade.md).

## Defeitos e regressao

O [registro de defeitos](gestao-defeitos.md) relaciona defeitos historicos
corrigidos (DF-01, DF-02, DF-04 a DF-07), seus commits e testes de regressao.
As pendencias PD-01 a PD-04 nao sao declaradas corrigidas. A alegacao DF-03
(OpenAPI sem 403) nao foi confirmada e nao e reportada como defeito corrigido.

## Limitacoes da evidencia e decisao

Os numeros acima valem para a branch `release/rc1-blocker-fixes` e o SHA
indicado, medidos em execucao local nesta sessao. A execucao local nao demonstra
merge, revisao independente, CI na `main` nem ausencia de vulnerabilidades. A
analise SonarCloud/Quality Gate no commit avaliado nao foi comprovada. A
evidencia JMeter e historica e nao foi reproduzida.

A decisao permanece **BLOQUEAR** ate que as pendencias sejam tratadas ou
formalmente delimitadas, o PR seja revisado e integrado, e haja evidencia de
workflow verde no commit resultante da `main`. Somente entao pode-se reavaliar
**LIBERAR COM RESSALVAS**, com riscos e responsaveis nomeados. Nenhuma tag de
release e afirmada.
