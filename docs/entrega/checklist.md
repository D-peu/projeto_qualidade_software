# Checklist de entrega e release

## Identificação da avaliação

- Repositório: `WesMarquess/projeto_qualidade_software`.
- Revisão da `main` auditada em clone limpo: `3d9b5fdd7468bf8d07c784965d790e897279c093`.
- Correções em avaliação: branch `release/rc1-blocker-fixes`, derivada do commit auditado. O resultado local não é CI nem revisão da `main`.
- O checklist externo original reproduziu `docker compose up -d`, `./mvnw spring-boot:run`, `./mvnw -B verify` e `node --test tests/*.test.js` no clone limpo. A falha de inicialização constatada está registrada como DF-04.
- Validação local da branch: Java 21.0.13+2, `./mvnw -B clean verify` passou (200 testes; zero falhas/erros/ignorados); JaCoCo 87,79% de linhas e 74,84% de branches; Node, 19 testes aprovados.

“Existe” indica que o artefato está no repositório ou foi produzido nesta branch. “Atualizado” avalia se ele representa a revisão em avaliação e se a evidência está acessível; não significa que o requisito funcional esteja completo.

| Item | Existe | Atualizado | Caminho/evidência |
|---|---|---|---|
| Repositório e histórico de revisão por pares | Sim | Parcial; não há review desta correção | Histórico Git e PRs existentes; PR #19 foi mergeado sem reviews registradas. A revisão independente da branch de correção ainda precisa ocorrer. |
| Aplicação executável | Sim | Corrigida localmente; não integrada na `main` | [README](../../README.md), migration [V0_1](../../src/main/resources/db/migration/V0_1__bootstrap_migration_dependencies.sql), teste [AplicacaoBancoLimpoPostgresTest](../../src/test/java/br/com/projeto_qualidade_software/projeto/integration/AplicacaoBancoLimpoPostgresTest.java) e [DF-04](../defeitos/DF-04-inicializacao-banco-limpo.md). |
| README e documentação da API | Sim | Parcial; mudanças desta branch ainda sem revisão/CI | [README](../../README.md), [guia da API](../api-reservas.md) e [OpenAPI](../openapi.yaml). |
| RTM requisito–risco–teste–evidência | Sim | Parcial; há pendências funcionais e evidências históricas | [rtm.md](../rtm.md). |
| Diagramas de contexto, componentes e sequências | Sim | Parcial; presentes, mas não atualizados/validados integralmente nesta correção | [Arquitetura/ATAM](../arquitetura-atam.md), [componentes](../prd/modelagem/estatica/componentes.md) e [fluxos de sequência](../prd/fluxos-sequencia.md). |
| Plano e relatório de testes | Sim | Atualizado localmente; sem CI remota desta revisão | [Estratégia](../testes/estrategia.md), [caixa-preta](../testes/caixa-preta.md), [caixa-branca](../testes/caixa-branca.md), [relatório final de qualidade](../relatorio-qualidade.md). |
| Pipeline de CI | Sim | Não para esta branch | [Workflow](../../.github/workflows/ci.yml); a execução verde consultada é de revisão anterior da `main`. |
| SonarCloud / Quality Gate | Sim para revisão anterior | Não para o commit de correção | Dashboard do projeto: https://sonarcloud.io/dashboard?id=WesMarquess_projeto_qualidade_software&branch=main. Revalidar após integração. |
| JaCoCo | Sim | Localmente atualizado; artefato de CI do commit final ainda não existe | `target/site/jacoco/` local; os números desta execução constam em [relatorio-qualidade.md](../relatorio-qualidade.md). |
| Plano e resultado de carga | Sim | Baseline local atual; exploratório, não produção | [Guia JMeter](../testes/jmeter/README.md), [resultado-01.md](../performance/resultado-01.md) e JTL compactados em [evidências brutas](../performance/evidencias/resultado-01/). |
| Registro de defeitos | Sim | Atualizado nesta branch; aguardando revisão/integração | [Gestão de defeitos](../gestao-defeitos.md) e [diretório de defeitos](../defeitos/README.md). |
| Relatório final de qualidade | Sim | Atualizado com a verificação local; não é avaliação de CI | [relatorio-qualidade.md](../relatorio-qualidade.md). |
| Changelog | Sim | Rascunho `Unreleased`; ainda não há versão publicada | [CHANGELOG.md](../../CHANGELOG.md). |
| Índice de evidências ligado ao commit da tag | Não | Não pode ser criado com links reais antes da tag/CI | Aguardar SHA final, run do CI, artifact JaCoCo e Quality Gate desse commit. Não inserir placeholders como se fossem evidências. |
| Tag anotada `v1.0.0-rc1` | Não | Corretamente não criada | A `main` precisa receber as correções, ter CI verde e cumprir os bloqueadores abaixo. Nunca mover tag publicada; correção pós-tag exigiria `rc2`. |

## Bloqueadores, em ordem

1. **Requisitos funcionais ainda pendentes ou parciais.** O PRD ainda aponta lacunas em gestão/CRUD de recursos e usuários, pesquisa/filtros completos, manutenção por período, aprovação de alterações de horário, validação docente, retirada/devolução de materiais, relatórios e notificações simuladas internas. Consulte as situações RF em [requisitos-funcionais.md](../prd/requisitos-funcionais.md) e a análise em [relatorio-qualidade.md](../relatorio-qualidade.md). Não declarar conformidade integral nem RC enquanto esses itens obrigatórios não forem resolvidos ou formalmente delimitados pela equipe.
2. **Revisão por pares da correção não realizada.** A correção precisa de um commit/PR revisado por outra pessoa; não foi feito commit direto na `main`.
3. **CI e SonarCloud da revisão corrigida pendentes.** A verificação local passou, mas não substitui pipeline, Quality Gate nem artifact JaCoCo do commit candidato.
4. **Inicialização limpa corrigida somente nesta branch.** DF-04 passou em Testcontainers e no smoke local, mas o desbloqueio só vale para release depois da revisão, integração à `main` e CI verde no commit que contém a migration/teste.
5. **Pacote de evidências de release ainda não pode ser fechado.** Sem commit de tag e run de CI associado, não há URL/artifact JaCoCo ou Quality Gate que satisfaça o requisito de apontar para o commit da tag. Criar o índice apenas após existir essa evidência.

## Decisão atual

**Não criar nem enviar `v1.0.0-rc1` agora.** O bloqueio técnico de banco vazio foi corrigido e os testes locais passaram, mas a `main` auditada permanece sem essas correções e os bloqueadores funcionais, de revisão e de CI continuam. A decisão de release deve ser reavaliada após revisão independente, merge e CI verde na `main`.
