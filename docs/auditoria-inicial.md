# Auditoria inicial do projeto

**Data:** 2026-11-12
**Escopo:** requisitos RF01–RF19, segurança, testes, CI e entregáveis.
**Restrição:** auditoria somente documental; nenhum código de produção foi alterado.

> **Adendo da revisão da branch — 2026-11-14:** os resultados abaixo são
> históricos, medidos em 12/11 e não descrevem o estado corrente. Nesta branch,
> `./mvnw -B clean verify` foi executado com sucesso: 222 testes Java, zero
> falhas/erros/ignorados; JaCoCo 89,60% de linhas (991/1106) e 75,16% de
> branches (242/322). `node --test tests/*.test.js` passou com 19 testes.
> `GET /api/reservas/pendentes` e as demais rotas da API foram incluídos em
> testes sem sessão; somente o fluxo CSRF/login está liberado para início sem
> identidade. Existem três testes WireMock (200/corpo, 500 e timeout), mas essa
> integração HTTP externa não satisfaz RF19/RN11, que exige notificação interna.
> A RTM canônica agora é [docs/RTM.md](./RTM.md); os caminhos anteriores são
> redirecionamentos Markdown. O diagrama C4 de contexto e a revisão de
> componentes/sequências ficam em [docs/arquitetura](./arquitetura/).
> A execução não comprova CI na `main`, SonarCloud nem a conclusão das
> funcionalidades ausentes. Verificação local realizada em 2026-11-14 UTC.

## 1. Fontes e execução

Foram consultados [requisitos-funcionais.md](./prd/requisitos-funcionais.md), [openapi.yaml](./openapi.yaml), [pom.xml](../pom.xml), [ci.yml](../.github/workflows/ci.yml), a RTM em [docs/rtm.md](./rtm.md) e a RTM auxiliar [docs/prd/RTM.md](./prd/RTM.md). O caminho literal `docs/RTM.md` não existe; o arquivo raiz usa `rtm.md` em minúsculas.

Verificações executadas nesta auditoria:

| Comando | Resultado |
|---|---|
| `./mvnw -B verify` | Sucesso; 175 testes Java, 0 falhas, 0 erros, 0 ignorados. O JaCoCo gerou [jacoco.xml](../target/site/jacoco/jacoco.xml). |
| `node --test tests/*.test.js` | 16 testes aprovados, 0 falhas, 0 ignorados. |

Cobertura agregada no JaCoCo: **linhas 696/786 = 88,55%** e **branches 163/204 = 79,90%**. Ambas superam os limites configurados no [pom.xml](../pom.xml) (80% linhas, 70% branches). Esses percentuais medem execução estrutural e não comprovam, por si sós, os requisitos funcionais.

## 2. Situação por requisito

“Implementado” é usado somente quando o recorte descrito tem código e teste correspondente. “Parcial” indica que ambos cobrem apenas parte do requisito. Quando não há comportamento e teste específicos, a situação é “Faltando”.

| RF | Situação | Evidência de código e teste; limites |
|---|---|---|
| RF01 | Parcial | Login/sessão em [`AutenticacaoController.login`, `currentUser` e `AutenticacaoInterceptor.preHandle`](../src/main/java/br/com/projeto_qualidade_software/projeto/api/AutenticacaoController.java); [`AutenticacaoControllerTest.loginCriaSessaoEPermiteRestaurarUsuarioAutenticado`](../src/test/java/br/com/projeto_qualidade_software/projeto/api/AutenticacaoControllerTest.java). Há autenticação com perfis cadastrados, mas não gestão/recuperação de contas nem teste HTTP integrado para cada perfil. |
| RF02 | Parcial | As restrições de identidade, perfil, posse e equipe estão em [`ContextoAutenticacao.exigirSolicitante`, `exigirAprovador`, `exigirEquipe` e verificações de acesso`](../src/main/java/br/com/projeto_qualidade_software/projeto/security/ContextoAutenticacao.java); testes em [`ContextoAutenticacaoTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/security/ContextoAutenticacaoTest.java) e posse em [`ReservaServiceTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/services/ReservaServiceTest.java). Não existe cobertura integrada de rotas por todos os perfis; há também a permissão indevida de aprovação por Administrador (ver seção 3). |
| RF03 | Parcial | Existem listagens em [`SalaController.listar`](../src/main/java/br/com/projeto_qualidade_software/projeto/api/SalaController.java) e [`CatalogoRecursosController.listarProfessores/listarMateriais`](../src/main/java/br/com/projeto_qualidade_software/projeto/api/CatalogoRecursosController.java), cobertas por [`SalaControllerTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/api/SalaControllerTest.java) e [`CatalogoRecursosControllerTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/api/CatalogoRecursosControllerTest.java). Não há CRUD administrativo, desativação ou classificação de recurso restrito. |
| RF04 | Faltando | Não foram encontrados endpoints/serviços para administrar usuários, bloqueios ou períodos de manutenção; nenhum teste específico. A recusa de sala pelo estado atual não implementa a gestão exigida. |
| RF05 | Parcial | Há listagens em [`SalaService.listarSalas`](../src/main/java/br/com/projeto_qualidade_software/projeto/services/SalaService.java) e nos endpoints de catálogo, com testes de listagem em [`SalaServiceTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/services/SalaServiceTest.java), [`SalaControllerTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/api/SalaControllerTest.java) e [`CatalogoRecursosControllerTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/api/CatalogoRecursosControllerTest.java). Não há pesquisa comprovada pelos filtros de tipo, capacidade, localização, competência e disponibilidade. |
| RF06 | Parcial | Criar, alterar e cancelar aparecem em [`ReservaController`](../src/main/java/br/com/projeto_qualidade_software/projeto/api/ReservaController.java) e [`ReservaService.solicitarReserva/alterarHorarioReserva/cancelarReserva`](../src/main/java/br/com/projeto_qualidade_software/projeto/services/ReservaService.java). Há testes de fluxo/posse em [`ReservaServiceTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/services/ReservaServiceTest.java) e HTTP/persistência em [`ReservaApiPostgresE2ETest`](../src/test/java/br/com/projeto_qualidade_software/projeto/integration/ReservaApiPostgresE2ETest.java). Faltam os limites temporais RN09/RN10; a alteração atual muda diretamente o horário, sem aprovação pendente. |
| RF07 | Parcial | [`ReservaService.validarIntervalo`](../src/main/java/br/com/projeto_qualidade_software/projeto/services/ReservaService.java) rejeita início/fim nulos e fim não posterior; testes em [`ReservaServiceTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/services/ReservaServiceTest.java) e [`ReservaControllerTest.solicitarReservaComIntervaloInvalidoRetornaBadRequest`](../src/test/java/br/com/projeto_qualidade_software/projeto/api/ReservaControllerTest.java). Não valida data futura nem incrementos de 30 minutos, exigidos por RF07/RN01. |
| RF08 | Implementado (sobreposição) | [`ReservaService.validarAusenciaDeConflito`](../src/main/java/br/com/projeto_qualidade_software/projeto/services/ReservaService.java) consulta conflitos de sala, professor e material; [`ReservaPostgresIntegrationTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/integration/ReservaPostgresIntegrationTest.java) testa sobreposição por recurso, adjacência e estados que ocupam a sala. Isso comprova o recorte implementado, não outros fluxos de alocação ainda ausentes. |
| RF09 | Parcial | [`ReservaService.solicitarReserva`, `alterarHorarioReserva` e `aprovarReserva`](../src/main/java/br/com/projeto_qualidade_software/projeto/services/ReservaService.java) usa locks e valida conflitos; [`ReservaPostgresIntegrationTest.deveAceitarSomenteUmaReservaEmSolicitacoesConcorrentesComConflito`](../src/test/java/br/com/projeto_qualidade_software/projeto/integration/ReservaPostgresIntegrationTest.java) comprova uma única solicitação concorrente aceita. Porém, pedidos pendentes sobrepostos são recusados já na solicitação, e a aprovação não revalida conflitos. Não há teste de concorrência na aprovação nem de alterações simultâneas conflitantes. |
| RF10 | Parcial, com divergência crítica | Nova reserva inicia como `SOLICITADA` em [`ReservaService.solicitarReserva`](../src/main/java/br/com/projeto_qualidade_software/projeto/services/ReservaService.java), mas [`ReservaService.aprovarReserva`](../src/main/java/br/com/projeto_qualidade_software/projeto/services/ReservaService.java) aceita Responsável **ou Administrador**. [`ReservaServiceTest.aprovarReservaPorResponsavelAtualizaReservaESalva` e `aprovarReservaPorAdministradorAtualizaReservaESalva`](../src/test/java/br/com/projeto_qualidade_software/projeto/services/ReservaServiceTest.java) confirmam ambos os caminhos atuais; não validam a regra “somente Responsável aprova”. Alteração de horário tampouco aguarda decisão. |
| RF11 | Parcial | [`ReservaService.validarDisponibilidadeDaSala`](../src/main/java/br/com/projeto_qualidade_software/projeto/services/ReservaService.java) impede reserva quando sala não está `DISPONIVEL`; [`ReservaServiceTest.solicitarReservaComSalaIndisponivelLancaIllegalStateException`](../src/test/java/br/com/projeto_qualidade_software/projeto/services/ReservaServiceTest.java) cobre esse caso. Não existe gestão de bloqueios/manutenção por período, preservação/avaliação de reservas existentes nem alerta ao Responsável. |
| RF12 | Faltando | O serviço associa professores e verifica conflito (RF08), mas isso não é um fluxo de validação da alocação pelo Responsável. Não foi encontrado método/endpoint de validação docente nem teste que o comprove. |
| RF13 | Faltando | `Reserva` associa materiais, mas não registra retirada/devolução; não há endpoint, serviço ou teste de movimentação de material. |
| RF14 | Parcial | [`Reserva`](../src/main/java/br/com/projeto_qualidade_software/projeto/entities/Reserva.java) guarda solicitante, decisor e horários de solicitação/aprovação; [`ReservaServiceTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/services/ReservaServiceTest.java) cobre transições e ator de aprovação. Não há trilha histórica de cada alteração/cancelamento, ator e data/hora; testes de estado não provam auditoria. |
| RF15 | Parcial | [`StatusReserva`](../src/main/java/br/com/projeto_qualidade_software/projeto/enums/StatusReserva.java) declara os sete estados do RF, e [`Reserva`](../src/main/java/br/com/projeto_qualidade_software/projeto/entities/Reserva.java) implementa decisão; testes de estado em [`ReservaTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/entities/ReservaTest.java) e [`ReservaServiceTest`](../src/test/java/br/com/projeto_qualidade_software/projeto/services/ReservaServiceTest.java). Não há fluxo para `EM_USO`, `CONCLUIDA` ou `NAO_COMPARECEU`, nem estado `EXPIRADA`/expiração automática prevista pela RN12. |
| RF16 | Faltando | Não foram encontrados relatórios/consultas de utilização, carga horária ou conflitos evitados; nenhum teste específico. |
| RF17 | Parcial | A interface existe em [`static/`](../src/main/resources/static), com estilos responsivos e controles ARIA em `css/app.css`, `index.html` e `js/app.js`. Os testes [`frontend-contract.test.js`](../tests/frontend-contract.test.js) e [`reservation-api.test.js`](../tests/reservation-api.test.js) exercitam contrato/regras do cliente, não responsividade visual, navegação por teclado, foco ou contraste em navegador. |
| RF18 | Parcial | Há contrato em [`openapi.yaml`](./openapi.yaml), guia em [`api-reservas.md`](./api-reservas.md) e rotas em [`ReservaController`](../src/main/java/br/com/projeto_qualidade_software/projeto/api/ReservaController.java) e controllers de autenticação/catálogo. Os testes Node validam o cliente e a API consumida, mas não há teste de conformidade OpenAPI contra as respostas reais nem documentação dos fluxos ainda não implementados. |
| RF19 | Faltando | Não foi encontrado serviço de notificação simulada nem teste de notificação. A exigência de notificação interna sem serviço externo já está definida no RF19/RN11; não depende de decisão sobre integração externa. |

## 3. Autorização — RF01/RF02

### O que os testes demonstram

| Perfil/caso | Evidência automatizada | Limite |
|---|---|---|
| Professor (Solicitante) | [`ContextoAutenticacaoTest.solicitanteNaoPodeInformarOutroIdOuTipo`](../src/test/java/br/com/projeto_qualidade_software/projeto/security/ContextoAutenticacaoTest.java) verifica identidade/tipo do Professor e nega outro ID, outro tipo e acesso à fila da equipe. [`AutenticacaoControllerTest.loginCriaSessaoEPermiteRestaurarUsuarioAutenticado`](../src/test/java/br/com/projeto_qualidade_software/projeto/api/AutenticacaoControllerTest.java) valida login de Professor. | Não cobre rotas sensíveis por sessão autenticada em jornada completa. |
| Coordenador (Solicitante) | Há verificação de divergência de tipo em `solicitanteNaoPodeInformarOutroIdOuTipo`; [`ReservaApiPostgresE2ETest.deveRetornarForbiddenQuandoTipoInformadoDivergirDoCadastro`](../src/test/java/br/com/projeto_qualidade_software/projeto/integration/ReservaApiPostgresE2ETest.java) recusa tipo divergente do cadastro. | Não autentica uma sessão de Coordenador nem prova suas permissões de ponta a ponta; o E2E configura `app.auth.enabled=false`. |
| Responsável | [`ContextoAutenticacaoTest.responsavelNaoPodeSePassarPorAdministrador`](../src/test/java/br/com/projeto_qualidade_software/projeto/security/ContextoAutenticacaoTest.java) permite o tipo Responsável e nega que a mesma conta se identifique como Administrador. Testes do serviço aprovam por Responsável. | Teste de perfil isolado, sem execução do endpoint com sessão/CSRF. |
| Administrador | [`ReservaServiceTest.aprovarReservaPorAdministradorAtualizaReservaESalva`](../src/test/java/br/com/projeto_qualidade_software/projeto/services/ReservaServiceTest.java) demonstra que o serviço aprova pelo caminho de Administrador. | Não prova que Administrador só executa operações permitidas: não há teste de sessão/rota de Administrador. Além disso, a permissão de aprovar conflita com RN05. |
| Sem sessão | [`AutenticacaoControllerTest.bloqueiaApiSemSessaoEOperacaoComTokenCsrfInvalido`](../src/test/java/br/com/projeto_qualidade_software/projeto/api/AutenticacaoControllerTest.java) espera 401 em `GET /api/auth/me` e `GET /api/salas`; também testa CSRF inválido. | Não testa todas as rotas protegidas. O interceptor abrange `/api/**`, exceto o início do fluxo CSRF e login; em mutação sem CSRF, a validação de CSRF pode responder 403 antes da checagem de sessão. |

### Lacunas e regra de aprovação

O controle HTTP está configurado globalmente em [`AutenticacaoWebConfig.addInterceptors`](../src/main/java/br/com/projeto_qualidade_software/projeto/security/AutenticacaoWebConfig.java), mas não existe matriz automatizada por rota × perfil. Os testes de API com banco real desabilitam autenticação; os testes de contexto são unitários. Também não há teste demonstrando um Coordenador autenticado, nem uma sessão de Administrador submetida às permissões administrativas.

**Sim, o Administrador consegue aprovar reservas.** O serviço aceita `TipoAprovadorReserva.ADMINISTRADOR`, a entidade registra Administrador como decisor e há teste positivo do caminho em `ReservaServiceTest`. Isso **não está de acordo** com RF10/RN05: toda reserva exige aprovação do Responsável, e somente Responsáveis aprovam. A classificação demonstrativa de recurso restrito não cria exceção para Administradores nem dispensa aprovação para outros recursos. A RTM atual deve tratar esse comportamento como divergência/defeito de autorização, não como requisito satisfeito.

## 4. Divergências entre matrizes, código e evidência

1. A seção “Situação da implementação atual” em [requisitos-funcionais.md](./prd/requisitos-funcionais.md) marca RF07 como implementado, mas o código não aplica data futura ou incrementos de 30 minutos; o estado real é parcial.
2. A mesma matriz marca RF09 como implementado. Os testes comprovam concorrência na criação de pedidos, porém a implementação não permite pendências sobrepostas e não revalida conflitos na aprovação, como exigem RN03/RF09; falta também teste de aprovação/alteração concorrente.
3. RF10 é descrito corretamente no PRD como aprovação do Responsável para toda reserva nova e alteração. A formulação de RF10 em [docs/rtm.md](./rtm.md) e [docs/prd/RTM.md](./prd/RTM.md) restringe indevidamente o requisito a recurso classificado como restrito. O código e testes atuais aceitam Administrador; isso contradiz RN05.
4. RF12–RF14 aparecem agrupados como pendentes na matriz de implementação. RF12 segue sem fluxo de validação docente; RF14 tem campos de decisor/horários para aprovação, mas não histórico auditável. Logo, RF12 falta e RF14 é parcial, sem evidência suficiente para considerar nenhum deles concluído.
5. A matriz chama RF19 de não definido, mas RF19/RN11 definem notificação simulada dentro da aplicação. O comportamento está faltando; a definição funcional já existe.
6. [docs/rtm.md](./rtm.md) identifica a revisão-base `4cb9799` como referência funcional e `d555bae` como revisão corrente à época da consolidação. Suas evidências de execução são históricas. Esta auditoria executou a árvore de trabalho atual e encontrou 175 testes Java/16 Node; não se deve atribuir esses números a CI remoto ou commit específico.
7. O DF-03 antigo alegava que o OpenAPI omitia 403 em alteração/cancelamento; o arquivo já declarava esses códigos. Foi removido do registro de defeitos e documentado como alegação não confirmada em [gestao-defeitos.md](./gestao-defeitos.md).

## 5. Entregáveis

| Entregável | Estado e localização |
|---|---|
| RTM | Existe em [docs/rtm.md](./rtm.md) (minúsculas) e [docs/prd/RTM.md](./prd/RTM.md). O caminho literal `docs/RTM.md` não existe. |
| Diagrama C4 de contexto | Não localizado. |
| Diagrama de componentes | Existe [componentes.md](./prd/modelagem/estatica/componentes.md), com Mermaid e componentes; não está identificado como C4 nem substitui um diagrama de contexto. |
| Diagramas de sequência | Existem em [fluxos-sequencia.md](./prd/fluxos-sequencia.md); são fluxos conceituais, inclusive fluxos ainda não implementados. |
| Plano JMeter | Existe [README.md](./testes/jmeter/README.md) e planos `.jmx` para consulta, criação e contenção em [docs/testes/jmeter](./testes/jmeter/). |
| Resultado JMeter | Existe [relatorio-desempenho.md](./testes/jmeter/relatorio-desempenho.md), com consultas, criação e contenção. Os artefatos brutos `target/jmeter/` citados no documento não estão presentes neste checkout, portanto os números do relatório não foram revalidados aqui. |
| Teste de concorrência | Existe em [ReservaPostgresIntegrationTest](../src/test/java/br/com/projeto_qualidade_software/projeto/integration/ReservaPostgresIntegrationTest.java), com PostgreSQL/Testcontainers; cobre solicitações, não aprovação nem alteração concorrente. |
| Teste end-to-end | Existe E2E **pela API** em [ReservaApiPostgresE2ETest](../src/test/java/br/com/projeto_qualidade_software/projeto/integration/ReservaApiPostgresE2ETest.java), incluindo banco real e persistência. Não é E2E de navegador; autenticação é desabilitada nessa classe. |
| WireMock | Não há dependência no [pom.xml](../pom.xml), cliente externo ou teste WireMock. É coerente com escopo atual sem integração externa; RF19 ainda precisa de notificações internas. |
| Evidência TDD/BDD | Existe [evidencias-tdd-bdd.md](./testes/evidencias-tdd-bdd.md) e cenários em [reservas.feature](./testes/reservas.feature). Gherkin não roda em runner BDD; o documento não comprova ciclo histórico TDD. |
| Registro de defeitos | Existe [gestao-defeitos.md](./gestao-defeitos.md); o registro atual separa defeitos históricos confirmados, divergências e alegações não confirmadas. |
| Relatório final de qualidade | Não localizado como entrega final. Há auditorias parciais em [auditoria-testes-ci.md](./auditoria-testes-ci.md) e [arquitetura-atam.md](./arquitetura-atam.md), além desta auditoria inicial; nenhuma é um relatório final aprovado. |

## 6. Pipeline, SonarCloud e qualidade dos testes

- [ci.yml](../.github/workflows/ci.yml) dispara em `push` **e** `pull_request`, configura Java 21/Node 24, inicia Docker, executa `./mvnw -B verify` e `node --test tests/*.test.js`, e publica relatórios Surefire/Failsafe/JaCoCo. A configuração existe; nesta auditoria foi validada execução local, não uma execução remota de PR.
- JaCoCo está configurado no Maven e os limites de linhas/branches passaram nesta execução. O relatório agregado pode incluir testes de serialização, controller e delegação sem verificar regra de negócio; cobertura não equivale a rastreabilidade funcional.
- SonarCloud está configurado no workflow e no plugin Maven, mas a etapa é condicional: só roda se `SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY` estiverem configurados e exige `SONAR_TOKEN`. Não foi executada pelos comandos locais desta auditoria; não se afirma Quality Gate atual.
- Há testes de baixo valor de regra que contam para cobertura, por exemplo [`SalaServiceTest.listarSalasConsultaTodasAsSalasDoRepository`](../src/test/java/br/com/projeto_qualidade_software/projeto/services/SalaServiceTest.java), que verifica sobretudo a delegação `findAll`, e testes de controller que verificam mapeamento/encaminhamento. Isso não prova intenção de inflar números, mas mostra por que cobertura JaCoCo não deve ser apresentada como prova de requisitos sem os casos de negócio correspondentes.

## 7. Pendências priorizadas

### (a) Limitadores de nota: execução, pipeline, autorização e dupla reserva

1. **P1 — Autorização:** impedir aprovação por Administrador conforme RF10/RN05 e adicionar testes HTTP com sessão/CSRF para Professor, Coordenador, Responsável e Administrador nas rotas permitidas e proibidas. Incluir testes de recusas sem sessão em endpoints protegidos, não apenas `/api/auth/me` e `/api/salas`.
2. **P1 — Dupla reserva na decisão:** revalidar conflitos e disponibilidade ao aprovar, sob proteção atômica; decidir como pedidos pendentes sobrepostos devem coexistir conforme RN03. Adicionar teste concorrente de aprovações e alterações concorrentes para sala, professor e material. Os testes atuais não reproduziram dupla reserva na criação concorrente, mas não cobrem a fase de confirmação.
3. **Execução da aplicação:** não foi identificado bloqueio de build/execução nesta verificação: `verify` passou e os testes E2E inicializam a aplicação. Isso não substitui smoke test de implantação nem a validação de autorização integrada.
4. **Pipeline:** não está ausente; há CI em pull request. Confirmar uma execução remota de PR e decidir se SonarCloud/Quality Gate deve ser obrigatório, pois hoje a análise pode ser omitida quando as variáveis não estão configuradas.

### (b) Funcionalidades faltando ou incompletas

1. **RF07:** validar data futura e intervalos em passos de 30 minutos.
2. **RF06/RF10:** aplicar prazos de alteração/cancelamento; manter alteração pendente, exigir decisão do Responsável e expirar pedidos não decididos.
3. **RF04/RF11:** administração de usuários, bloqueios e manutenção temporal, preservação de reservas existentes e alertas.
4. **RF03/RF05:** CRUD de recursos/usuários e pesquisa com os filtros previstos.
5. **RF12/RF13/RF14:** validação de alocação docente, retirada/devolução de materiais e auditoria persistida de todas as mudanças.
6. **RF15:** transições de ciclo operacional e expiração automática com auditoria.
7. **RF16/RF19:** relatórios e notificações internas simuladas.
8. **RF17/RF18:** validar acessibilidade em navegador e automatizar conformidade do contrato OpenAPI.

### (c) Entregáveis faltando ou a completar

1. Diagrama C4 de contexto; identificar formalmente se o diagrama atual de componentes e as sequências atendem ao formato solicitado.
2. Teste E2E de navegador e jornada E2E autenticada com banco real.
3. Evidência contemporânea de TDD; runner BDD, caso se queira que Gherkin seja executado.
4. Preservar e versionar/arquivar evidências brutas JMeter junto dos resultados; os arquivos `target/jmeter` não estão neste checkout.
5. Relatório final de qualidade com decisão formal, resultados por requisito e pendências aceitas.
6. Corrigir divergências entre PRD, RTMs, estratégia, registro de defeitos e estado atual do OpenAPI/código.
