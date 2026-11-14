# Checklist de entrega e bloqueadores

## Identificacao da avaliacao

- Repositorio publico: [WesMarquess/projeto_qualidade_software](https://github.com/WesMarquess/projeto_qualidade_software).
- `main` consultada em 2026-11-14: `3d9b5fdd7468bf8d07c784965d790e897279c093`.
- Branch avaliada: `release/rc1-blocker-fixes`, SHA local testado
  `26f678f7e1b6b58a2984e75c841f13e0e3243ef0`.
- PR #20 aberto para a `main`, ainda Draft e sem merge observado; o SHA remoto
  consultado antes do push desta revisao era `b1c40b7d0a4a4e1fb13c6e288904d6cd7a798afb`.
- A auditoria anterior em clone limpo reproduziu a falha de inicializacao em
  banco sem schema. DF-04 registra a causa e a migration/teste que corrigem o
  problema nesta branch. Esta revisao local executou `./mvnw -B clean verify`
  em 2026-11-14: 222 testes Java, zero falhas/erros/ignorados; JaCoCo 89,60%
  linhas e 75,16% branches. `node --test tests/*.test.js`: 19 aprovados.
- Esses resultados sao locais e da branch; nao sao CI da `main`.

“Existe” indica artefato no repositorio ou resultado registrado. “Atualizado”
indica que o conteudo reflete a revisao avaliada; nao significa que o requisito
funcional esteja completo.

| Item | Existe | Atualizado | Caminho/evidencia |
|---|---|---|---|
| Repositorio e revisao por pares | Sim | Revisao independente desta branch pendente; PR #20 aberto em Draft | [PR #20](https://github.com/WesMarquess/projeto_qualidade_software/pull/20), historico Git |
| Aplicacao executavel | Sim | Inicializacao limpa corrigida e testada nesta branch; merge/main nao verificados | [README](../../README.md), migration `V0_1`, [AplicacaoBancoLimpoPostgresTest](../../src/test/java/br/com/projeto_qualidade_software/projeto/integration/AplicacaoBancoLimpoPostgresTest.java), [DF-04](../defeitos/DF-04-inicializacao-banco-limpo.md) |
| README e documentacao da API | Sim | Atualizados para rotas, erros e limitacoes atuais | [README](../../README.md), [guia API](../api-reservas.md), [OpenAPI](../openapi.yaml) |
| RTM requisito–risco–teste–evidencia | Sim | Matriz unica atualizada; requisitos parciais/pendentes declarados | [docs/RTM.md](../RTM.md); caminhos antigos sao aliases |
| Diagramas de contexto, componentes e sequencias | Sim | Contexto e fluxos revisados para refletir o codigo e as lacunas | [C4 contexto](../arquitetura/c4-contexto.md), [componentes](../prd/modelagem/estatica/componentes.md), [sequencias](../prd/fluxos-sequencia.md) |
| Plano e relatorio de testes | Sim | Atualizados com 222 Java/19 Node e limitacoes de execucao | [estrategia](../testes/estrategia.md), [caixa-preta](../testes/caixa-preta.md), [caixa-branca](../testes/caixa-branca.md), [relatorio final](../relatorio-final-qualidade.md) |
| Pipeline de CI | Sim | Existe; run verde conhecido anterior a esta revisao foi no SHA `b1c40b7`; CI do SHA atual deve ser verificada apos push | [workflow](../../.github/workflows/ci.yml), [run #43528184691](https://github.com/WesMarquess/projeto_qualidade_software/actions/runs/43528184691) |
| JaCoCo | Sim | Relatorio local desta branch: 89,60% linhas/75,16% branches; artifact remoto citado e de SHA anterior | `target/site/jacoco/`; artifact do [run #43528184691](https://github.com/WesMarquess/projeto_qualidade_software/actions/runs/43528184691) |
| SonarCloud / Quality Gate | Configurado condicionalmente | Analise e Quality Gate do SHA avaliado nao comprovados | [Dashboard do projeto](https://sonarcloud.io/dashboard?id=WesMarquess_projeto_qualidade_software&branch=main); variaveis/secrets e resultado dependem da configuracao da equipe |
| Plano e resultado de carga | Sim | Plano existe; resultado e historico, nao reproduzido nesta sessao | [Plano JMeter](../testes/jmeter/README.md), [resultado historico](../performance/resultado-01.md); `target/jmeter/` ausente |
| Registro de defeitos | Sim | Corrigidos historicos separados de pendencias declaradas | [gestao](../gestao-defeitos.md), [defeitos](../defeitos/README.md) |
| Relatorio final de qualidade | Sim | Atualizado com os resultados e riscos da branch | [relatorio final](../relatorio-final-qualidade.md), [resumo](../relatorio-qualidade.md) |
| Changelog | Sim | Secao `Unreleased`; sem release/tag | [CHANGELOG.md](../../CHANGELOG.md) |
| Indice de evidencias associado a commit de tag | Nao | Sem tag/commit candidato, CI e artifact que possam ser vinculados | Nao criar placeholders como se fossem evidencias |
| Tag anotada `v1.0.0-rc1` | Nao | Corretamente nao criada | Aguardar criterios abaixo; tag publicada nunca deve ser movida |

## Bloqueadores, em ordem

1. **Requisitos funcionais parciais ou pendentes.** RTM #1–#4, #6, #8–#13
   registra os limites e evidencias; permanecem, entre outros, aprovacao de
   alteracoes de horario, pedidos pendentes conflitantes conforme RN03,
   manutencao por periodo, retirada/devolucao, auditoria completa, notificacao
   interna, relatorios e validacao de interface em navegador.
2. **Revisao independente pendente.** O PR #20 segue Draft; obter revisao de
   outra pessoa antes do merge.
3. **Merge e CI da `main` pendentes.** `main` foi consultada no SHA
   `3d9b5fdd7468bf8d07c784965d790e897279c093`; a CI verde conhecida de
   `b1c40b7` nao e CI do SHA atual nem da `main` apos integracao.
4. **SonarCloud/Quality Gate nao comprovado.** A configuracao pode ser
   condicional; executar analise no commit integrado e confirmar o gate.
5. **Pacote de evidencias de release nao pode ser fechado.** Sem commit/tag
   validos e workflow/artifact no SHA exato, nao criar indice final nem tag.

## Decisao atual

**BLOQUEAR.** Nao criar nem enviar `v1.0.0-rc1`. Reavaliar somente apos
resolver ou delimitar formalmente as pendencias, obter revisao independente,
integrar na `main`, verificar CI verde no SHA integrado e avaliar o Quality
Gate. A decisao de qualidade detalha criterios, riscos e responsabilidades em
[docs/decisao-qualidade.md](../decisao-qualidade.md).
