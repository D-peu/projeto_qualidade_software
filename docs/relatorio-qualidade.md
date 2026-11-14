# Relatorio de qualidade

Este resumo acompanha o [relatorio final](relatorio-final-qualidade.md) e a
[decisao de qualidade](decisao-qualidade.md). A avaliacao vale para a branch
`release/rc1-blocker-fixes`, SHA
`d7ccbeeb37347ead150d911cb3b1723351568d2a`, com verificacoes locais executadas
em 2026-11-14 UTC. Nao representa merge ou CI da `main`, analise SonarCloud ou
prontidao para producao.

## Evidencia local

| Verificacao | Resultado |
|---|---|
| `./mvnw -B clean verify` | BUILD SUCCESS; 222 testes Java; 0 falhas, 0 erros, 0 ignorados; testes PostgreSQL/Testcontainers, WireMock e inicializacao em banco limpo executados. |
| JaCoCo | 89,60% de linhas (991/1106) e 75,16% de branches (242/322); metas do build: 80% e 70%. |
| `node --test tests/*.test.js` | 19 testes aprovados; 0 falhas. |
| OpenAPI | YAML valido; 21 paths e 23 operacoes. Nao ha validacao automatizada de conformidade de todos os endpoints. |
| JMeter nesta sessao | Nao executado; `target/jmeter/` ausente. Resultado versionado e historico. |
| SonarCloud / vulnerabilidades | Nao comprovado para o SHA avaliado; nao declarar zero bugs ou vulnerabilidades. |

## Estado e limitacoes

A RTM registra 3 requisitos Implementados, 8 Parciais e 3 Pendentes. Todos os
6 criterios criticos estao rastreados com risco, teste e evidencia (6/6), mas
isso nao significa implementacao completa. As pendencias funcionais incluem
CRUD/pesquisa completos, manutencao por periodo, aprovacao de alteracoes de
horario, pedidos pendentes concorrentes conforme RN03, retirada/devolucao,
auditoria completa, notificacao interna, relatorios e validacao de interface
em navegador. Detalhes e testes estao na [RTM](RTM.md).

O Surefire emitiu o aviso “going to kill self fork JVM” depois de 30 segundos;
nao foi encontrado `System.exit` no codigo e a causa nao foi confirmada. O log
da verificacao nao apresentou `Connection refused` apos os stubs nos testes
nao relacionados a notificacao.

**Decisao: BLOQUEAR.** Nao ha comprovacao de merge e workflow verde na `main`
para o SHA avaliado; o Quality Gate SonarCloud desta revisao nao foi comprovado.
Reavaliar somente apos resolver/delimitar as pendencias, revisao independente,
merge e CI verde na `main`. Veja a [decisao](decisao-qualidade.md) e o
[relatorio final](relatorio-final-qualidade.md).
