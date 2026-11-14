# Auditoria da Estratégia de Testes e Integração Contínua

**Data da auditoria:** 2026-11-11
**Escopo:** configuração Maven, workflow GitHub Actions, testes Java e Node, Testcontainers/PostgreSQL, JaCoCo, SonarCloud, evidências e documentação.
**Restrição respeitada:** nenhum teste foi executado para esta revisão documental; foram consultadas configurações e evidências de execuções já existentes.

> **Adendo da branch de correção — 2026-11-14:** a auditoria abaixo é um
> retrato histórico da configuração e dos resultados conhecidos em 11/11. Nesta
> data foi executado `./mvnw -B clean verify` na branch
> `release/rc1-blocker-fixes`: `BUILD SUCCESS`, 222 testes Java, zero falhas,
> erros ou ignorados; JaCoCo 991/1106 linhas (89,60%) e 242/322 branches
> (75,16%). `node --test tests/*.test.js` passou com 19 testes. Os comandos não
> são CI nem resultados da `main`. WireMock está presente no POM e
> `ReservaNotificacaoWireMockIntegrationTest` cobre HTTP 200/corpo, 500 e
> timeout; a integração externa continua divergente do requisito de notificação
> interna. `target/jmeter/` não existe nesta sessão; a medição disponível é
> histórica e versionada em `docs/performance/resultado-01.md`. A causa do
> aviso de encerramento do fork Surefire não foi confirmada. Estes fatos
> atualizam o estado atual sem reatribuir ao snapshot de 11/11 seus números ou
> conclusões.

## 1. Critérios de classificação

- **Implementado e comprovado:** configuração/código existe e há evidência identificável de execução ou resultado.
- **Implementado, mas sem evidência atual:** mecanismo existe, mas não há resultado atual verificável para o aspecto analisado.
- **Parcial:** existe apenas parte da cobertura, integração ou comprovação necessária.
- **Ausente:** não foi localizada implementação/configuração correspondente.

A aprovação de um job comprova somente os comandos executados naquele job e revisão. Ela não comprova cobertura de requisitos que não são exercitados nem substitui relatórios específicos.

## 2. Resultado por área

| Área | Classificação | Evidência e conclusão |
|---|---|---|
| JUnit 5 — testes unitários | **Implementado e comprovado** | `spring-boot-starter-test` fornece JUnit 5. Há testes de entidade (`ReservaTest`), serviço (`ReservaServiceTest`, `SalaServiceTest`), autenticação e inicialização de dados. O CI #24 completou `./mvnw -B verify` com sucesso no commit `d555bae`. |
| JUnit 5 — integração com banco e API | **Implementado e comprovado** | `ReservaPostgresIntegrationTest` cobre persistência, conflitos, intervalos e concorrência usando PostgreSQL real via Testcontainers. `ReservaApiPostgresE2ETest` inicia a aplicação em porta aleatória e chama endpoints HTTP. O CI #24 inicializou Docker e concluiu Verify com sucesso. |
| Cobertura dos principais fluxos | **Parcial** | Há cobertura para criação, consulta, alteração, cancelamento, conflitos, estados e aspectos de sessão/perfil/posse. Não há jornada E2E autenticada completa; RF12, estados operacionais, auditoria, relatórios e integração de notificação permanecem ausentes ou pendentes conforme a RTM e a estratégia. Testes de listagem de catálogo não comprovam CRUD administrativo nem pesquisa filtrada. |
| JaCoCo — instrumentação/relatório | **Implementado e comprovado** | `jacoco-maven-plugin` 0.8.12 configura `prepare-agent`, relatórios HTML/XML e `check`; `sonar.coverage.jacoco.xmlReportPaths` aponta para `target/site/jacoco/jacoco.xml`. O workflow atual publica Surefire/Failsafe, JaCoCo HTML/XML e `jacoco.exec` como artefato de CI. O run #24 do `main` publicou `test-reports-43054180565-1`. |
| JaCoCo — metas exigidas pelo build | **Implementado e comprovado** | O POM executa `jacoco:check` na fase `verify`, exigindo no bundle pelo menos 80% de cobertura de linhas e 70% de branches. O CI #24 do commit `d555bae` concluiu com sucesso; os limites são aplicados pelo build, não apenas recomendados pela estratégia. |
| SonarCloud — análise | **Implementado e comprovado** | O CI #21, anterior ao PR #13, executou “Analyze with SonarCloud” e os logs registram `ANALYSIS SUCCESSFUL`/upload do relatório. A consulta anterior ao painel registrou Quality Gate `OK`, cobertura global 86,8%, cobertura de branches 79,9%, 0 bugs, 0 vulnerabilidades e 2 code smells; esses números não são atribuídos ao CI #24 nem provam cada requisito funcional. |
| SonarCloud — integração no build/Quality Gate | **Parcial** | A etapa só roda quando `SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY` estão configuradas; sem elas, é ignorada. Se as variáveis existem e `SONAR_TOKEN` falta, o workflow falha explicitamente. O workflow não configura espera explícita (`sonar.qualitygate.wait`). O painel consultado anteriormente informava `new_coverage` de 84,1% contra 80%; isso é uma condição do Sonar, distinta dos limites totais de 80% linhas/70% branches impostos por JaCoCo no Maven. |
| GitHub Actions — execução Java | **Implementado e comprovado** | [Workflow `ci.yml`](../.github/workflows/ci.yml) roda em `push` e `pull_request`, configura Java 21, verifica Docker e executa `./mvnw -B verify`. O PR #13 foi incorporado ao `main` no commit `d555bae`. A execução mais recente consultada é [CI #24](https://github.com/WesMarquess/projeto_qualidade_software/actions/runs/43054180565), nesse commit, concluída em 2026-11-11 com sucesso. |
| GitHub Actions — artefatos de teste | **Implementado e comprovado** | O workflow usa `actions/upload-artifact@v4` para publicar relatórios Surefire/Failsafe, relatórios JaCoCo e `jacoco.exec`, com retenção de 14 dias. O CI #24 publicou o artefato `test-reports-43054180565-1` (715.206 bytes; expira em 2026-11-25). Os planos/relatórios JMeter locais não fazem parte desse artefato. |
| Testcontainers/PostgreSQL | **Implementado e comprovado** | `pom.xml` inclui `org.testcontainers:junit-jupiter` e `org.testcontainers:postgresql` (versão 1.21.4). As duas classes de integração declaram `@Testcontainers`, um `@Container` estático `PostgreSQLContainer<>("postgres:16")`, readiness com `pg_isready` e propriedades datasource via `@DynamicPropertySource`. CI inicializa Docker antes de Verify. |
| Isolamento dos testes de banco | **Parcial** | Cada classe declara seu container estático PostgreSQL separado; os testes de integração configuram `create-drop` e `@Transactional`, e o E2E usa banco `create-drop` com limpeza por callbacks. É isolamento por classe/ambiente de teste, não evidência de testes paralelos isolados entre si ou de equivalência com produção. Ambas as classes desabilitam autenticação. |
| WireMock / integração HTTP externa | **Ausente** | Não há dependência WireMock no POM nem cliente HTTP externo encontrado no código de produção/testes. A estratégia cita WireMock como condicional a uma decisão futura para RF19, que permanece pendente; não existe teste de integração HTTP externo. |
| Testes Node | **Implementado e comprovado na CI; validação visual permanece parcial** | Existem `tests/frontend-contract.test.js` e `tests/reservation-api.test.js`, baseados em `node:test`. Após o PR #13, o workflow configura Node.js 24 e executa `node --test tests/*.test.js`; o CI #24 do `main` concluiu com sucesso. Isso não substitui testes visuais em navegador nem comprova responsividade/acessibilidade. |
| Segurança: sessão, CSRF, perfil e posse | **Parcial** | `AutenticacaoControllerTest` verifica sessão/login, acesso sem sessão (401) e token CSRF inválido (403); `ContextoAutenticacaoTest` verifica perfil, identidade e IDs/tipo informados pelo solicitante; `ReservaServiceTest` cobre verificações de posse. O E2E `ReservaApiPostgresE2ETest` configura `app.auth.enabled=false`, portanto não comprova autorização integrada com banco. Não há evidência de uma jornada completa autenticada com diferentes perfis e posse nas rotas sensíveis. |
| Respostas HTTP e contrato | **Parcial** | MockMvc testa respostas de autenticação e a classe E2E envia requisições HTTP e verifica respostas/estado persistido em cenários selecionados. A estratégia prevê REST-assured, mas essa dependência não está no POM; a suíte não demonstra conformidade completa de todos os endpoints e respostas com OpenAPI. |
| Surefire/Failsafe | **Parcial** | `mvn verify` invoca testes Maven. As classes `*IntegrationTest` e `*E2ETest` terminam em `Test` e entram no padrão usual do Surefire, não por uma fase Failsafe configurada. O workflow publica os caminhos de relatórios Surefire/Failsafe como artefato; a configuração do upload não significa que todos os tipos de relatório existam em cada execução. |
| Cenários Gherkin/Cucumber | **Parcial** | [reservas.feature](./testes/reservas.feature) especifica cenários e os relaciona a métodos JUnit. Conforme a estratégia, não há dependência/runner Cucumber; os cenários Gherkin são documentação, não execução automatizada independente. |
| JMeter/carga na CI | **Parcial** | O workflow não executa JMeter. Os JTL e relatórios HTML das execuções de 11/11/2026 estão presentes localmente em `target/jmeter/`, mas `target/` é ignorado pelo Git e esses arquivos não são publicados pelo workflow. Os resultados são de ambiente local, não prova de desempenho de produção. |

## 3. Detalhes de execução e isolamento

### Testes Java

O workflow invoca `./mvnw -B verify`, não comandos seletivos por pacote. O POM herda a configuração de testes do Spring Boot e adiciona as dependências de teste. O CI #24 permite afirmar que Verify terminou com sucesso para `d555bae`; não permite afirmar que todos os requisitos possuem teste dedicado. CI #21 é uma execução histórica do commit `4cb9799`.

A documentação contém registros de diferentes revisões: [evidências TDD/BDD](./testes/evidencias-tdd-bdd.md) relata 130 testes Java em 2026-10-26, enquanto a história de integração do frontend registra 156 testes Java e 15 Node. São evidências históricas de revisões distintas; não devem ser apresentadas como contagem do CI #24 sem relatório/log específico que a confirme.

### PostgreSQL e Testcontainers

`ReservaPostgresIntegrationTest` executa testes do serviço/repositórios com transações, criação do schema e PostgreSQL 16 em container. Entre os cenários estão conflitos, recursos, sobreposição e concorrência. `ReservaApiPostgresE2ETest` usa `RANDOM_PORT` e `TestRestTemplate`, exercitando endpoints HTTP e verificando persistência.

Ambas as classes definem `app.auth.enabled=false`. Assim, os testes de API com banco real não atestam autenticação, perfil, CSRF ou posse na mesma jornada. Os testes MockMvc de autenticação usam serviços mockados e comprovam apenas os cenários específicos implementados.

### JMeter — execuções de 11/11/2026

Os resultados abaixo foram lidos dos JTL/`statistics.json` e metadados já existentes em `target/jmeter/`; nenhum teste foi executado para esta revisão. Os diretórios permanecem locais e ignorados pelo Git.

| Cenário | Execução e resultado |
|---|---|
| Consulta | JMeter 5.6.3, 50 usuários, ramp-up de 30 s e duração de 300 s. 365.500 amostras HTTP, 100% de sucesso (zero erros), média de 35,46 ms, p95 de 61 ms e throughput de 1.218,7 req/s. |
| Contenção | Rajada sincronizada de 50 requisições HTTP; 1 resposta `201` e 49 `409` esperados, zero falhas, média de 822,24 ms e código de saída 0. A validação auxiliar do plano também terminou sem erro; as métricas de latência referem-se às 50 requisições HTTP. |

O teste JMeter de criação não foi executado nesta série e não é declarado concluído. Resultados de criação descritos anteriormente em `relatorio-desempenho.md` permanecem históricos e separados. A parada anterior da aplicação segue com causa inconclusiva; os registros consultados não estabelecem uma causa. Também permanecem sem teste integrado completo de autorização as rotas com autenticação habilitada, e RF12/RF15 continuam pendentes conforme a RTM.

## 4. Evidências localizadas e não localizadas

| Evidência | Estado na auditoria |
|---|---|
| Workflow ativo e definição dos comandos | PR #13 incorporado ao `main` em `d555bae`; workflow atual contém Verify, Node e upload de relatórios. |
| Execução remota mais recente | [CI #24](https://github.com/WesMarquess/projeto_qualidade_software/actions/runs/43054180565), no `main` em `d555bae`, concluída com sucesso em 2026-11-11. CI #21 é evidência histórica do commit `4cb9799` (2026-11-08). |
| JaCoCo | `pom.xml` configura `check` em `verify`, com limites de 80% linhas e 70% branches. Relatórios locais (`target/surefire-reports`, `target/jacoco.exec` e `target/site/jacoco/jacoco.xml`) estão presentes no checkout, mas não foram gerados nesta revisão. |
| Artefato do CI #24 | `test-reports-43054180565-1` publicado, com expiração em 2026-11-25. |
| SonarCloud | O CI #21 executou a análise SonarCloud; o CI #24 foi confirmado como sucesso geral, mas esta revisão não atribui a ele um resultado de análise Sonar específica. |
| Resultados JMeter de 11/11/2026 | JTL e relatórios HTML presentes em `target/jmeter/consulta-20261111-151033658/` e `target/jmeter/contencao-20261111-1617/`. Estão ignorados pelo Git e não são publicados como artefatos do CI. |
| Execução Node | Workflow atual configura Node 24 e executa `node --test tests/*.test.js`; CI #24 concluiu com sucesso. A execução histórica de 10 testes de 2026-10-15 continua sendo evidência de outra revisão. |
| Criação JMeter | Não executada na série de 11/11/2026; permanece sem resultado atual e não é marcada como concluída. |

## 5. Lacunas prioritárias

1. **P1 — Cobertura de segurança integrada:** adicionar jornada E2E com autenticação habilitada para sessão, CSRF, perfil e posse; o E2E PostgreSQL atual desabilita o interceptor.
2. **P1 — Retenção e rastreabilidade das evidências:** o CI já publica relatórios Java/JaCoCo por 14 dias; preservar os resultados necessários após a expiração e identificar cada evidência com revisão/ambiente. JMeter continua fora do CI.
3. **P2 — Validação visual do frontend:** os testes Node agora rodam na CI; permanecem pendentes a validação em navegador, responsividade e acessibilidade.
4. **P2 — Contrato HTTP:** avaliar REST-assured ou ferramenta equivalente e automatizar validação de respostas de API contra OpenAPI; a estratégia planeja REST-assured, ausente do POM.
5. **P2 — Qualidade Sonar:** o workflow não espera explicitamente o resultado do Quality Gate antes de encerrar o job; decidir se o gate deve bloquear merge e alinhar as condições Sonar ao critério de cobertura total adotado pelo build.
6. **P3 — Integrações futuras:** se RF19 ganhar integração HTTP externa, adicionar cliente, WireMock e testes de sucesso, timeout e falha; não implementar WireMock antes da decisão de escopo.
7. **P3 — Teste de carga:** decidir se JMeter será rotina de CI ou validação manual periódica; preservar os JTL/relatórios locais junto à revisão e ambiente, pois os resultados atuais estão sob `target/` ignorado e não são publicados no CI.

## 6. Referências

- [POM](../pom.xml), [workflow CI](../.github/workflows/ci.yml) e [README](../README.md).
- Testes: [integração PostgreSQL](../src/test/java/br/com/projeto_qualidade_software/projeto/integration/ReservaPostgresIntegrationTest.java), [E2E PostgreSQL/API](../src/test/java/br/com/projeto_qualidade_software/projeto/integration/ReservaApiPostgresE2ETest.java), [autenticação](../src/test/java/br/com/projeto_qualidade_software/projeto/api/AutenticacaoControllerTest.java), [contexto de autenticação](../src/test/java/br/com/projeto_qualidade_software/projeto/security/ContextoAutenticacaoTest.java), [testes Node](../tests).
- Documentação: [estratégia](./testes/estrategia.md), [evidências TDD/BDD](./testes/evidencias-tdd-bdd.md), [cenários Gherkin](./testes/reservas.feature), [relatório JMeter](./testes/jmeter/relatorio-desempenho.md) e [RTM](./rtm.md).
- [Painel SonarCloud do projeto](https://sonarcloud.io/dashboard?id=WesMarquess_projeto_qualidade_software&branch=main) e [Quality Gate](https://sonarcloud.io/api/qualitygates/project_status?projectKey=WesMarquess_projeto_qualidade_software&branch=main).
