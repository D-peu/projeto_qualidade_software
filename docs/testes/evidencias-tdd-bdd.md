# Evidencias de TDD e BDD

## Escopo

Este documento preserva o registro historico do ciclo TDD para a classificacao
demonstrativa de materiais, vinculada a RF10 do PRD da equipe, INV-04 e CP-54/CP-55.
O cenario Gherkin correspondente esta em [reservas.feature](./reservas.feature).
O projeto nao configura Cucumber; os cenarios sao especificacoes rastreaveis e
os testes JUnit relacionados rodam separadamente.

**Retificação:** o ciclo historico abaixo registrou a regra de aprovação por
classificação do material, que divergia da RN05 vigente. Nesta revisão, a
aprovação de reservas novas foi alinhada ao PRD: somente Responsável aprova,
independentemente do material; os testes atuais e a evidência de execução estão
na tabela de rastreabilidade ao final.

## Ciclo TDD executado em 2026-11-13

### RED

- Commit: `7c23368 test(red): aprovação de recurso restrito`
- Comando: `./mvnw -B -Dtest=ReservaServiceTest,ReservaPostgresIntegrationTest test`
- Resultado: BUILD FAILURE durante `testCompile`; os dois testes nao chegaram a
  executar. O compilador apontou duas chamadas a `Material.setRestrito(boolean)`
  inexistente, uma no teste parametrizado e outra no teste PostgreSQL. A falha
  evidencia a ausencia da classificacao de materiais no modelo; nao e registrada
  como falha de assertiva nem como teste executado.

### GREEN

- Commit: `1eeb465 feat(green): classifica materiais restritos na aprovação`
- Comando: `./mvnw -B -Dtest=ReservaServiceTest,ReservaPostgresIntegrationTest,AlteracaoHorarioMigrationPostgresTest test`
- Resultado: BUILD SUCCESS; 52 testes, 0 falhas, 0 erros e 0 ignorados. Foram
  executados os casos parametrizados para Responsavel, Administrador com material
  restrito e Administrador com material nao restrito, o teste de integracao
  PostgreSQL/Testcontainers e os testes da migration. A migration cria a
  classificacao como nao restrita por padrao e impede valor nulo.

### REFACTOR

- Commit: `0a97b92 refactor: extrai verificação de material restrito`
- Comando: `./mvnw -B verify`
- Resultado: BUILD SUCCESS; 186 testes Java, 0 falhas, 0 erros e 0 ignorados.
  JaCoCo: 85,89% de linhas (901/1049) e 73,57% de branches (231/314); os limites
  configurados foram atendidos.
- Comando complementar: `node --test tests/*.test.js`
- Resultado: 19 testes Node aprovados, 0 falhas.

Os comandos do ciclo acima são evidência histórica registrada em 2026-11-13. O RED parou na
compilacao porque o campo ainda nao existia; os estagios GREEN e REFACTOR
executaram os testes descritos. O teste Gherkin nao foi executado por runner BDD.

## Regra atual de aprovação e verificação nesta branch

O ciclo histórico acima implementou classificação de material, não a regra de
negócio vigente de aprovação. A correção da regra atual (Responsável aprova
reserva restrita e Administrador recebe 403, sem exceção para material comum)
foi registrada no commit `d7039d5` (`Fix release blockers and refresh quality
evidence`), que altera conjuntamente serviço e testes. O histórico TDD
anteriormente relacionado à classificação tem os hashes:

1. `7c23368` — RED registrado; a compilação falhou porque `setRestrito` não
   existia. Não foi uma falha de assertiva.
2. `1eeb465` — GREEN registrado; adicionou a classificação demonstrativa.
3. `0a97b92` — REFACTOR registrado; extraiu a verificação de classificação.

Esses três commits comprovam o ciclo da classificação, não uma sequência
RED/GREEN/REFACTOR da regra final “somente Responsável aprova”. A regra final é
verificada por `ReservaServiceTest.approvalRequiresResponsibleRegardlessOfMaterialRestriction`,
`ReservaPostgresIntegrationTest.deveRecusarAprovacaoDeMaterialRestritoPorAdministrador`
e `ReservaAutenticacaoPostgresE2ETest.administradorNaoPodeAprovarReservaSemMaterialRestrito`.
No commit avaliado nesta branch, `./mvnw -B clean verify` passou em
2026-11-14 UTC: 222 testes Java, zero falhas/erros/ignorados. O JaCoCo
`target/site/jacoco/jacoco.xml` registrou 991/1106 linhas (89,60%) e 242/322
branches (75,16%). `node --test tests/*.test.js` também passou na mesma data:
19 testes, zero falhas. Esta é uma verificação de estado atual, não uma
alegação de TDD histórico adicional.

A execução final ocorreu depois de substituir `NotificacaoClient` por mock nas
classes de integração cujo objeto não é a notificação. Não houve `Connection
refused` no log final. O aviso do Surefire “going to kill self fork JVM” ainda
apareceu após o limite de 30 segundos, embora o processo tenha terminado com
`BUILD SUCCESS`; não foi localizado `System.exit` no código do projeto. A causa
provável é thread não daemon/recurso de contexto de teste ou container que
permanece ativo durante o encerramento. Isso é uma hipótese, não uma causa
confirmada, e não invalida a contagem de testes.

## Rastreabilidade

| Comportamento | Requisito/regra e caso | Teste JUnit |
|---|---|---|
| Material restrito aprovado por Responsavel | RF10, INV-04, CP-54 | `ReservaServiceTest.approvalRequiresResponsibleRegardlessOfMaterialRestriction` |
| Material restrito recusado para Administrador | RF10, INV-04, CP-156 | `ReservaServiceTest.approvalRequiresResponsibleRegardlessOfMaterialRestriction`; `ReservaPostgresIntegrationTest.deveRecusarAprovacaoDeMaterialRestritoPorAdministrador` |
| Material nao restrito aprovado por Responsavel | RF10, INV-04, CP-54 | `ReservaServiceTest.approvalRequiresResponsibleRegardlessOfMaterialRestriction` |
| Material nao restrito recusado para Administrador | RF02, RF10, INV-04, CP-55/CP-157 | `ReservaServiceTest.approvalRequiresResponsibleRegardlessOfMaterialRestriction`; `ReservaAutenticacaoPostgresE2ETest.administradorNaoPodeAprovarReservaSemMaterialRestrito` |
