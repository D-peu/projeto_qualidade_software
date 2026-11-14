# Matriz única de rastreabilidade — entrega da disciplina

**Revisão funcional verificada:** `release/rc1-blocker-fixes`, código no commit
`2c83627` (14/11/2026). Esta RTM é a fonte canônica; não representa merge,
execução de CI na `main`, Quality Gate ou aceite de produção.

## Evidências desta revisão

| ID | Comando executado | Resultado | Data |
|---|---|---|---|
| E1 | `./mvnw -B clean verify` | `BUILD SUCCESS`; 222 testes Java, 0 falhas, 0 erros, 0 ignorados. `target/site/jacoco/jacoco.xml`: linhas 991/1106 = 89,60%; branches 242/322 = 75,16%. PostgreSQL/Testcontainers e WireMock foram executados. | 2026-11-14 UTC |
| E2 | `node --test tests/*.test.js` | 19 testes aprovados, 0 falhas. | 2026-11-14 UTC |

E1 e E2 foram executados localmente nesta branch. Não atribuir esses resultados
à `main` ou à CI. “Pendente” significa que a funcionalidade descrita não está
implementada; a aprovação da suíte completa não comprova funcionalidade
ausente. Casos Gherkin em `docs/testes/reservas.feature` não têm runner BDD e
não são contados como execução automatizada independente.

## Matriz — 14 requisitos da especificação acadêmica

| # | Requisito da especificação | Mapeamento PRD do projeto | Criticidade | Risco se não atendido | Teste(s) automatizado(s) / limite | Evidência | Status |
|---:|---|---|---|---|---|---|---|
| 1 | Autenticação e autorização por perfil | RF01, RF02 | **Crítico** | Acesso não autorizado a dados ou ações; limitador de nota. | `AutenticacaoControllerTest.loginCriaSessaoEPermiteRestaurarUsuarioAutenticado`; `AutenticacaoRotasControllerTest.getApiSemSessaoRetorna401`; `AutenticacaoRotasControllerTest.mutacaoComCsrfMasSemLoginRetorna401`; `ReservaAutenticacaoPostgresE2ETest.administradorNaoPodeAprovarReservaSemMaterialRestrito`. Inclui `GET /api/reservas/pendentes`; cadastro/gestão de contas e E2E de navegador não existem. | E1; os testes de sessão, perfis, CSRF e rotas anônimas passaram. | Parcial |
| 2 | Cadastro e consulta de salas, professores e materiais | RF03 | Não | Recursos podem estar incompletos ou indisponíveis para gestão. | `SalaPostgresIntegrationTest.administradorCriaSalaPersistidaEVisivelNoCatalogo`; `CatalogoRecursosControllerTest.listarProfessoresRetornaSomenteIdENome`; `CatalogoRecursosControllerTest.listarMateriaisRetornaCatalogoOrdenadoPeloRepositorio`. Edição/desativação e cadastro administrativo de professores/materiais não existem. | E1; os testes citados passaram; operações ausentes não foram executadas. | Parcial |
| 3 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade | RF05 | Não | Busca pode omitir ou incluir recursos inadequados. | `SalaControllerTest.listarSalasAplicaFiltrosRecebidos`; `SalaPostgresIntegrationTest.administradorAlteraEstadoEFiltraCatalogoDeSalas`. Apenas estado e capacidade mínima são filtráveis; tipo, localização e competência não têm implementação. | E1; os testes citados passaram; filtros ausentes não foram executados. | Parcial |
| 4 | Criar, alterar e cancelar reservas; ciclo de estados | RF06, RF07, RF10, RF15; RN01, RN06, RN09, RN10, RN12 | **Crítico** — estados | Transições ou prazos incorretos podem manter recurso ocupado, liberar horário indevidamente ou apagar rastreabilidade. | `ReservaControllerTest.solicitarReservaRetornaCreatedLocationEReserva`; `ReservaControllerTest.alterarHorarioRetornaReservaComHorarioAtualizado`; `ReservaApiPostgresE2ETest.deveCancelarReservaPorHttpEConsultarEstadoPersistido`; `ReservaServiceTest.expiresSolicitationsBeforeAtAndAfterStartWhenListing`; `ReservaPostgresIntegrationTest.devePersistirAuditoriaAoConcluirUtilizacao`. Aprovação separada de alteração de horário, prazo de cancelamento de duas horas e expiração de alteração pendente não estão implementados. | E1 e E2; testes citados passaram. Regras pendentes não foram exercitadas. | Parcial |
| 5 | Detectar sobreposição para sala e agenda do professor (e material) | RF08; RN02 | **Crítico** — conflito | Dupla alocação de sala, professor ou material. | `ReservaPostgresIntegrationTest.deveDetectarConflitoEntreIntervalosSobrepostos`; `deveImpedirSobreposicaoDoMesmoProfessorEmSalasDiferentes`; `deveImpedirSobreposicaoDoMesmoMaterialEmSalasDiferentes`; `naoDeveDetectarConflitoEntreIntervalosAdjacentes`. | E1; testes de integração PostgreSQL citados passaram. | Implementado |
| 6 | Proteger contra dupla reserva em solicitações simultâneas | RF09; RN03 | **Crítico** — concorrência | Duas reservas conflitantes aceitas simultaneamente; limitador de nota. | `ReservaPostgresIntegrationTest.deveAceitarSomenteUmaReservaEmSolicitacoesConcorrentesComConflito`; `deveAprovarNoMaximoUmaReservaConflitanteEmOperacoesConcorrentes`; `deveAceitarNoMaximoUmaAlteracaoConcorrenteParaMesmoHorario`; também `deveImpedirSolicitacoesConcorrentesParaMesmoProfessorEmSalasDiferentes` e `deveImpedirSolicitacoesConcorrentesParaMesmoMaterialEmSalasDiferentes`. Os três cenários principais usam PostgreSQL/Testcontainers, `ExecutorService` e `CountDownLatch`; métodos concorrentes suspendem a transação de classe. A regra atual também recusa conflito com outra `SOLICITADA` na criação; RN03 permite pedidos pendentes coincidentes e revalida na aprovação. | E1; os cinco testes de concorrência citados passaram. | Parcial |
| 7 | Exigir Responsável para aprovar recurso restrito | RF03, RF10; RN05 | **Crítico** — autorização | Administrador ou perfil indevido confirma recurso restrito. | `ReservaPostgresIntegrationTest.deveRecusarAprovacaoDeMaterialRestritoPorAdministrador`; `ReservaAutenticacaoPostgresE2ETest.administradorNaoPodeAprovarReservaSemMaterialRestrito`; `ReservaServiceTest.approvalRequiresResponsibleRegardlessOfMaterialRestriction`. A regra atual também recusa Administrador para recursos não restritos. | E1; testes de serviço, integração e HTTP autenticado passaram. | Implementado |
| 8 | Bloquear recursos em manutenção | RF04, RF11; RN04 | Não | Reserva pode ser aceita em recurso indisponível; bloqueios por intervalo podem não ser respeitados. | `ReservaPostgresIntegrationTest.deveRecusarAprovacaoQuandoSalaEntraEmManutencao`; `ReservaServiceTest.blocksApprovalWhenRoomIsInMaintenance`; `SalaPostgresIntegrationTest.administradorAlteraEstadoEFiltraCatalogoDeSalas`. Bloqueio/manutenção por período e alerta ao Responsável não existem. | E1; testes de estado atual da sala passaram; manutenção por período não foi executada. | Parcial |
| 9 | Registrar retirada e devolução de materiais/equipamentos | RF13; RN08 | Não | Perda ou impossibilidade de comprovar a movimentação de material. | Nenhum: endpoints, serviço e persistência de retirada/devolução não estão implementados. | Não executado; E1 não comprova requisito ausente. | Pendente |
| 10 | Manter histórico auditável das mudanças | RF14, RF15; RN07 | **Crítico** — auditoria | Mudança sem ator/data/hora impede responsabilização e investigação. | `ReservaPostgresIntegrationTest.devePersistirAuditoriaDaCriacaoNaMesmaTransacao`; `ReservaPostgresIntegrationTest.devePersistirAuditoriaAoConcluirUtilizacao`; `ReservaAutenticacaoPostgresE2ETest.consultaAuditoriaProtegidaPorPerfil`; `ReservaControllerTest.consultarAuditoriaRetornaAtorEstadoEData`. Auditoria cobre criação e transições; alterações diretas de horário não têm trilha completa e não há operação de exclusão a testar. | E1; testes de persistência e consulta protegida passaram. | Parcial |
| 11 | Emitir notificação simulada dentro do sistema, sem serviço externo | RF19; RN11 | Não | Solicitantes podem não receber decisão; dependência externa diverge do requisito vigente. | `ReservaNotificacaoWireMockIntegrationTest.approvalSendsExpectedNotificationBody`; `serverErrorDoesNotUndoRejection`; `notificationTimeoutDoesNotUndoCancellation`. Cobrem o cliente HTTP externo, inclusive 200/corpo, 500 e timeout, mas **não** comprovam notificação interna exigida pelo PRD. | E1; três testes WireMock passaram. Conformidade interna: não executada porque a solução não existe. | Pendente |
| 12 | Relatórios de utilização, carga horária e conflitos evitados | RF16 | Não | Responsável/Administração sem dados para planejar capacidade ou detectar conflitos. | Nenhum endpoint ou relatório correspondente foi localizado. | Não executado; E1 não comprova requisito ausente. | Pendente |
| 13 | Interface responsiva e mensagens de erro compreensíveis | RF17 | Não | Uso inacessível em dispositivos/telas; erros podem impedir recuperação pelo usuário. | `tests/frontend-contract.test.js` verifica mensagens e regras do cliente; `tests/reservation-api.test.js` verifica erros da API e rede. Não há teste E2E em navegador, viewport responsiva ou acessibilidade. | E2; 19 testes Node passaram. Verificação visual não executada nesta revisão. | Parcial |
| 14 | Documentar API ou fluxos públicos | RF18 | Não | Consumidores podem integrar rotas ou interpretar erros incorretamente. | Contrato `docs/openapi.yaml`, guia `docs/api-reservas.md` e proteção de rotas testada em `AutenticacaoRotasControllerTest`. A suíte executa testes do comportamento documentado, mas não há validador automático OpenAPI contra todos os endpoints. | E1 e E2; documentos e testes existem; validação completa do contrato não executada automaticamente. | Implementado |

## Cobertura dos requisitos críticos na RTM

Critérios críticos definidos pela especificação e regra de estados: #1 autenticação
e perfis, #4 ciclo de estados, #5 sobreposição, #6 concorrência, #7 aprovação
restrita e #10 auditoria. **6/6 (100%) estão registrados nesta RTM com criticidade,
risco e teste/evidência, conforme conferência documental de 2026-11-14.** Essa
métrica mede presença/rastreabilidade das linhas, não implementação: #1, #4 e
#10 continuam parciais. O resultado local de E1 não substitui CI, revisão ou
Quality Gate na `main`.

## Pendências funcionais declaradas

- #2/#3: CRUD completo de recursos e pesquisa por tipo, localização e competência.
- #4: fluxo de aprovação de alteração de horário, limite de cancelamento de
  duas horas e expiração de alteração pendente que preserve horário original.
- #6: permitir solicitações pendentes coincidentes, conforme RN03, preservando
  a exclusão mútua no momento da aprovação.
- #8: agenda de bloqueios/manutenção por período e alerta ao Responsável.
- #9: retirada e devolução de materiais.
- #10: histórico completo das alterações de horário e cobertura integral de
  auditoria.
- #11: substituir/complementar o cliente HTTP externo por notificação simulada
  interna em conformidade com RF19/RN11.
- #12: relatórios de utilização, carga horária e conflitos evitados.
- #13: E2E em navegador, viewport responsiva, teclado, foco visível e contraste.
- #14: validação automatizada de conformidade entre OpenAPI e todos os endpoints.

## Arquivos de compatibilidade

Os caminhos históricos [docs/rtm.md](./rtm.md) e
[docs/prd/RTM.md](./prd/RTM.md) apontam para esta matriz única.
