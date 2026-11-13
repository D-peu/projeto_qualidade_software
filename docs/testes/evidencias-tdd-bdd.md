# Evidencias de TDD e BDD

## Escopo

Este documento registra o ciclo TDD executado nesta sessao para a aprovacao de
materiais restritos, vinculada a RF10 do PRD da equipe, INV-04 e CP-54/CP-55.
O cenario Gherkin correspondente esta em [reservas.feature](./reservas.feature).
O projeto nao configura Cucumber; os cenarios sao especificacoes rastreaveis e
os testes JUnit relacionados rodam separadamente.

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

Os comandos acima foram executados localmente em 2026-11-13. O RED parou na
compilacao porque o campo ainda nao existia; os estagios GREEN e REFACTOR
executaram os testes descritos. O teste Gherkin nao foi executado por runner BDD.

## Rastreabilidade

| Comportamento | Requisito/regra e caso | Teste JUnit |
|---|---|---|
| Material restrito aprovado por Responsavel | RF10, INV-04, CP-54 | `ReservaServiceTest.approvalRespectsMaterialRestriction` |
| Material restrito recusado para Administrador | RF10, INV-04, CP-55 | `ReservaServiceTest.approvalRespectsMaterialRestriction`; `ReservaPostgresIntegrationTest.deveRecusarAprovacaoDeMaterialRestritoPorAdministrador` |
| Material nao restrito mantém o fluxo existente | RF10, INV-04, CP-55 | `ReservaServiceTest.approvalRespectsMaterialRestriction` |
