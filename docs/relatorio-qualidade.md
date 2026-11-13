# Relatorio de qualidade

**Data:** 2026-11-13  
**Escopo:** verificacao local e CI do PR #20 na branch `release/rc1-blocker-fixes`. A CI citada validou o commit `d7039d5c622ed4d072530f7456f5427f107b8710`; o documento nao representa revisao por pares, CI na `main` ou liberacao de producao.

## Evidencias executadas

| Verificacao | Resultado |
|---|---|
| `./mvnw -B clean verify` (Java 21.0.13+2) | BUILD SUCCESS; 200 testes Java, 0 falhas, 0 erros e 0 ignorados. Os testes de integracao PostgreSQL/Testcontainers foram executados. |
| JaCoCo | 87,79% de cobertura de linhas (971/1106) e 74,84% de branches (241/322). Os limites do build, 80% e 70%, foram atendidos. |
| `node --test tests/*.test.js` | 19 testes aprovados, 0 falhas. |
| Instalacao limpa | `AplicacaoBancoLimpoPostgresTest` confirmou inicializacao/migrations em PostgreSQL sem schema preexistente e HTTP 200 em `/api/auth/csrf`. O smoke manual com o banco descartavel tambem respondeu HTTP 200. |
| JMeter | Baseline local de 13/11 em [resultado-01.md](performance/resultado-01.md): consultas 35.225 (p95 26 ms), criacao 7.702 (p95 112 ms), contencao 1 `201` e 49 `409` esperados. JTL compactados versionados. |
| Autorizacao de aprovacao | A API recusa Administrador ao aprovar, inclusive reserva sem material restrito; teste HTTP autenticado com CSRF e PostgreSQL confirma HTTP 403 e ausencia de transicao. |
| WireMock | Os testes de integracao de notificacao executaram dentro de `verify`; isso comprova o cliente HTTP atual, nao conformidade com o requisito de simulacao interna descrito abaixo. |
| CI do PR #20 | [Execucao 43527466987](https://github.com/WesMarquess/projeto_qualidade_software/actions/runs/43527466987), no commit `d7039d5c622ed4d072530f7456f5427f107b8710`: sucesso; artifact `test-reports-43527466987-1` publicado. A etapa SonarCloud foi ignorada. |
| `git diff --check` | Sem erros de whitespace. |

## Resultado por risco critico

- **Dupla reserva:** os testes PostgreSQL concorrentes da suite passaram; o limite de aprovacao exclusiva do Responsavel tambem tem regressao automatizada.
- **Autorizacao:** sessoes, perfis, CSRF e a recusa de aprovacao por Administrador foram exercitados. A cobertura de jornadas completas para todos os endpoints e perfis continua parcial.
- **Manutencao:** Administrador pode alterar o estado operacional da sala; pedidos e aprovacoes verificam o estado. Agenda de manutencao por periodo ainda nao existe.
- **Auditoria:** criacao, transicoes e expiracao de solicitacoes geram eventos; a equipe consulta eventos e alteracoes aplicadas de horario. Alteracoes diretas de horario em solicitacoes ainda nao formam um pedido auditavel de aprovacao.

## Pendencias frente ao PRD

O resultado dos testes nao significa que todos os requisitos estejam completos:

- **RF03-RF05:** CRUD de professores/materiais e filtros de localizacao/competencia ainda faltam; filtros de sala cobrem somente estado e capacidade minima.
- **RF04/RF11:** faltam bloqueios e manutencoes por periodo e alerta ao Responsavel.
- **RF06/RF10:** alterar horario de uma solicitacao ainda aplica a mudanca diretamente; falta pedido pendente, aprovacao do Responsavel, revalidacao e expiracao sem perder o horario original.
- **RF12-RF13:** faltam fluxo de validacao docente e registro de retirada/devolucao de materiais.
- **RF14-RF15:** trilha e estados estao parciais; expiracao de solicitacoes novas ocorre na consulta, e alteracoes pendentes nao existem.
- **RF16:** relatorios de utilizacao, carga horaria e conflitos evitados nao estao implementados.
- **RF17-RF18:** validacao visual/acessibilidade em navegador e verificacao automatizada de conformidade OpenAPI permanecem pendentes.
- **RF19/RN11:** o codigo envia notificacoes por HTTP e tem testes WireMock, mas o PRD vigente exige notificacoes simuladas dentro do sistema, sem integracao externa. A implementacao esta divergente e nao deve ser declarada conforme ate a equipe decidir ou substituir o fluxo.
- **Desempenho:** a nova medicao e local, exploratoria, usa 10 usuarios/60 segundos e nao representa producao. `APP_AUTH_ENABLED=false` foi usado apenas no ambiente descartavel; o resultado nao e evidencia de autorizacao. Metas de desempenho oficiais nao foram aprovadas.
- **CI/SonarCloud:** a CI do PR #20 passou no commit `d7039d5`; a etapa SonarCloud foi ignorada, sem Quality Gate. A CI do commit integrado na `main` ainda esta pendente.

## Decisao

**Nao declarar release candidate final nem conformidade integral.** A falha de inicializacao em banco limpo foi corrigida nesta branch e a verificacao local esta verde, incluindo o requisito de instalacao limpa; a CI do PR #20 passou no commit indicado. Ainda permanecem lacunas funcionais do PRD, a divergencia em RF19/RN11, validacao visual pendente, falta de Quality Gate e ausencia de revisao independente/merge na `main`. Nenhuma tag foi criada.
