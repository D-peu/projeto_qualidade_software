# Checklist de entrega e release

## Identificação da avaliação

- Repositório: `WesMarquess/projeto_qualidade_software`.
- Revisão da `main` auditada em clone limpo: `3d9b5fdd7468bf8d07c784965d790e897279c093`.
- Correções em avaliação: branch `release/rc1-blocker-fixes`, commit `d7039d5c622ed4d072530f7456f5427f107b8710`, derivada do commit auditado. O PR #20 está em Draft; o resultado não é CI nem revisão da `main`.
- O checklist externo original reproduziu `docker compose up -d`, `./mvnw spring-boot:run`, `./mvnw -B verify` e `node --test tests/*.test.js` no clone limpo. A falha de inicialização constatada está registrada como DF-04.
- Validação local: Java 21.0.13+2, `./mvnw -B clean verify` passou (200 testes; zero falhas/erros/ignorados); JaCoCo 87,79% de linhas e 74,84% de branches; Node, 19 testes aprovados. A CI do PR #20 também passou no commit indicado abaixo.

“Existe” indica que o artefato está no repositório ou foi produzido nesta branch. “Atualizado” avalia se ele representa a revisão em avaliação e se a evidência está acessível; não significa que o requisito funcional esteja completo.

| Item | Existe | Atualizado | Caminho/evidência |
|---|---|---|---|
| Repositório e histórico de revisão por pares | Sim | Parcial; não há review desta correção | Histórico Git e PRs existentes; PR #19 foi mergeado sem reviews registradas. A revisão independente da branch de correção ainda precisa ocorrer. |
| Aplicação executável | Sim | Corrigida localmente; não integrada na `main` | [README](../../README.md), migration [V0_1](../../src/main/resources/db/migration/V0_1__bootstrap_migration_dependencies.sql), teste [AplicacaoBancoLimpoPostgresTest](../../src/test/java/br/com/projeto_qualidade_software/projeto/integration/AplicacaoBancoLimpoPostgresTest.java) e [DF-04](../defeitos/DF-04-inicializacao-banco-limpo.md). |
| README e documentação da API | Sim | Parcial; CI do PR passou, revisão humana pendente | [README](../../README.md), [guia da API](../api-reservas.md) e [OpenAPI](../openapi.yaml). |
| RTM requisito–risco–teste–evidência | Sim | Parcial; há pendências funcionais e evidências históricas | [rtm.md](../rtm.md). |
| Diagramas de contexto, componentes e sequências | Sim | Parcial; presentes, mas não atualizados/validados integralmente nesta correção | [Arquitetura/ATAM](../arquitetura-atam.md), [componentes](../prd/modelagem/estatica/componentes.md) e [fluxos de sequência](../prd/fluxos-sequencia.md). |
| Plano e relatório de testes | Sim | Atualizado; CI passou no commit indicado, revisão humana pendente | [Estratégia](../testes/estrategia.md), [caixa-preta](../testes/caixa-preta.md), [caixa-branca](../testes/caixa-branca.md), [relatório final de qualidade](../relatorio-qualidade.md). |
| Pipeline de CI | Sim | Verde no PR #20; ainda não executado em commit integrado na `main` | [Workflow](../../.github/workflows/ci.yml) e [execução 43527466987](https://github.com/WesMarquess/projeto_qualidade_software/actions/runs/43527466987), no commit `d7039d5c622ed4d072530f7456f5427f107b8710`. |
| SonarCloud / Quality Gate | Sim para revisão anterior | Sem análise nesta execução; Quality Gate do commit corrigido indisponível | A etapa Analyze with SonarCloud foi ignorada na [execução do PR #20](https://github.com/WesMarquess/projeto_qualidade_software/actions/runs/43527466987). Dashboard: https://sonarcloud.io/dashboard?id=WesMarquess_projeto_qualidade_software&branch=main. |
| JaCoCo | Sim | Relatório local e artifact da CI do PR #20 disponíveis; não são do commit final em `main` | `target/site/jacoco/`; artifact `test-reports-43527466987-1` na [execução 43527466987](https://github.com/WesMarquess/projeto_qualidade_software/actions/runs/43527466987). |
| Plano e resultado de carga | Sim | Baseline local atual; exploratório, não produção | [Guia JMeter](../testes/jmeter/README.md), [resultado-01.md](../performance/resultado-01.md) e JTL compactados em [evidências brutas](../performance/evidencias/resultado-01/). |
| Registro de defeitos | Sim | Atualizado nesta branch; aguardando revisão/integração | [Gestão de defeitos](../gestao-defeitos.md) e [diretório de defeitos](../defeitos/README.md). |
| Relatório final de qualidade | Sim | Atualizado com a verificação local; não é avaliação de CI | [relatorio-qualidade.md](../relatorio-qualidade.md). |
| Changelog | Sim | Rascunho `Unreleased`; ainda não há versão publicada | [CHANGELOG.md](../../CHANGELOG.md). |
| Índice de evidências ligado ao commit da tag | Não | Não pode ser criado com links reais antes da tag/CI | Aguardar SHA final, run do CI, artifact JaCoCo e Quality Gate desse commit. Não inserir placeholders como se fossem evidências. |
| Tag anotada `v1.0.0-rc1` | Não | Corretamente não criada | A `main` precisa receber as correções, ter CI verde e cumprir os bloqueadores abaixo. Nunca mover tag publicada; correção pós-tag exigiria `rc2`. |

## Bloqueadores, em ordem

1. **Requisitos funcionais ainda pendentes ou parciais.** O PRD ainda aponta lacunas em gestão/CRUD de recursos e usuários, pesquisa/filtros completos, manutenção por período, aprovação de alterações de horário, validação docente, retirada/devolução de materiais, relatórios e notificações simuladas internas. Consulte as situações RF em [requisitos-funcionais.md](../prd/requisitos-funcionais.md) e a análise em [relatorio-qualidade.md](../relatorio-qualidade.md). Não declarar conformidade integral nem RC enquanto esses itens obrigatórios não forem resolvidos ou formalmente delimitados pela equipe.
2. **Revisão por pares da correção não realizada.** A correção precisa de um commit/PR revisado por outra pessoa; não foi feito commit direto na `main`.
3. **CI em `main` e SonarCloud pendentes.** A CI do PR #20 passou no commit `d7039d5`, mas isso não é execução na `main`; a etapa SonarCloud foi ignorada e não há Quality Gate do commit corrigido.
4. **Inicialização limpa corrigida somente nesta branch.** DF-04 passou em Testcontainers e no smoke local, mas o desbloqueio só vale para release depois da revisão, integração à `main` e CI verde no commit que contém a migration/teste.
5. **Pacote de evidências de release ainda não pode ser fechado.** Sem commit de tag e run de CI associado, não há URL/artifact JaCoCo ou Quality Gate que satisfaça o requisito de apontar para o commit da tag. Criar o índice apenas após existir essa evidência.

## Decisão atual

**Não criar nem enviar `v1.0.0-rc1` agora.** O bloqueio técnico de banco vazio foi corrigido e a CI do PR passou no commit indicado, mas o PR continua em Draft, sem revisão humana; a `main` permanece no SHA auditado, e os bloqueadores funcionais e de SonarCloud continuam. Reavaliar somente após resolver os requisitos pendentes, obter revisão independente, integrar e validar o commit final na `main`.
