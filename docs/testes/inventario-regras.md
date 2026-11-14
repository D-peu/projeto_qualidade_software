# Inventário de Regras e Riscos

## 1. Como ler este documento

Este inventário liga cada regra de negócio (RN) e requisito funcional (RF) a um comportamento esperado, ao risco de falha e à técnica de teste adequada. Ele serve de base para `caixa-preta.md`, `caixa-branca.md`, `caixa-cinza.md` e `rtm.md`.

- **Fontes:** `docs/prd/requisitos-funcionais.md`, `docs/openapi.yaml`, `docs/api-reservas.md` e o código em `src/main/java`.
- **Estado atual:** Implementado, Parcial ou Pendente, sempre com a classe e o método que comprovam. Pendente não é tratado como se funcionasse.
- **Impacto:** Alto, Médio ou Baixo, pelo dano se a regra falhar.
- **Técnica:** P = caixa preta, B = caixa branca, C = caixa cinza.
- **Crítico:** os requisitos RF07 a RF11 são críticos para a avaliação e estão marcados.
- **Decisão pendente:** pontos que dependem de decisão do grupo registrada em `estrategia.md`.

## 2. Inventário

Ordenado por impacto.

| ID | Regra e requisito | Comportamento esperado | Risco se falhar | Impacto | Técnica | Estado atual e evidência |
|---|---|---|---|---|---|---|
| INV-01 | RN01 / RF07 (crítico). Término posterior ao início. | Início ou fim ausente, ou fim igual ou anterior ao início, é recusado com 400. Vale ao solicitar e ao alterar o horário. | Reserva inválida gravada; conflito de horário calculado sobre intervalo sem sentido. | Alto | P, B | Implementado. `ReservaService.validarIntervalo` (IllegalArgumentException, 400). Os DTOs também exigem `@NotNull`, e `Reserva.isIntervaloValido` repete a regra. |
| INV-02 | RN02 / RF08 (crítico). Sem sobreposição de recurso. | Reserva que se sobrepõe para a mesma sala, docente ou material é recusada com 409. Intervalos adjacentes são aceitos; ao alterar horário, a própria reserva não conta como conflito. | Dupla alocação de sala, professor ou material, ou recusa indevida de reserva legítima. | Alto | P, B, C | Implementado. `ReservaRepository` consulta conflitos por sala, docente e material; `ReservaService` considera SOLICITADA, APROVADA, EM_USO, CONCLUIDA e NAO_COMPARECEU ocupantes do intervalo original `[inicio, fim)`. |
| INV-03 | RN03 / RF09 (crítico). Simultaneidade. | Duas solicitações simultâneas que compartilham sala, docente ou material e intervalo resultam em no máximo uma reserva aceita; a outra recebe 409. | Dupla reserva (falha que limita a nota). | Alto | C | Coberto por testes de integração concorrente em `ReservaPostgresIntegrationTest`: PostgreSQL 16 via Testcontainers verifica sala compartilhada, docente em salas diferentes e material em salas diferentes; intervalos não conflitantes na mesma sala são aceitos. |
| INV-04 | RN05 / RF10 (crítico). Aprovação pelo perfil permitido. | Toda reserva nova exige aprovação do Responsável, independentemente da classificação do recurso; Administrador não pode aprovar. | Aprovação por perfil indevido. | Alto | P, B, I | Implementado para novas reservas. `ReservaServiceTest.approvalRequiresResponsibleRegardlessOfMaterialRestriction` cobre recursos restritos e não restritos; `ReservaAutenticacaoPostgresE2ETest.administradorNaoPodeAprovarReservaSemMaterialRestrito` verifica 403 e ausência de transição com sessão e PostgreSQL. Aprovação de alterações de horário continua pendente. |
| INV-05 | RN04 / RF11 (crítico). Sala indisponível. | Sala cujo estado não é DISPONIVEL não aceita nova reserva, alteração de horário nem aprovação (409). | Reserva em sala em manutenção. | Alto | P, B | Implementado para o estado atual da sala. `ReservaService.validarDisponibilidadeDaSala` é executado ao solicitar, alterar horário e aprovar; aprovação bloqueia a linha da sala. CP-124 e `ReservaServiceTest`. |
| INV-06 | RN02 / RF08 (crítico). Sobreposição de professor e material. | Conflito de horário também é detectado para professor e material envolvidos. | Professor ou material alocado em dois lugares ao mesmo tempo. | Alto | P, B, C | Implementado. Docentes são contas `Solicitante` do tipo `PROFESSOR`; `Material` tem entidade e relação com reserva. Testes validam conflito individual e concorrência. |
| INV-07 | RN04 / RF11 (crítico). Bloqueios e manutenção por período. | Um período de bloqueio ou manutenção impede reservas só naquele intervalo. **Decisão pendente:** o que fazer com reservas já existentes dentro de um bloqueio novo. | Reservas aceitas em período bloqueado. | Alto | P, C | Pendente. Não há agenda de bloqueios nem endpoint administrativo. |
| INV-08 | RF01 e RF02. Autenticação e autorização. | Login identifica a conta por e-mail/senha; sessão HTTP, perfil e posse restringem operações. Mutação sem CSRF é recusada. | Qualquer pessoa age em nome de outra (falha de autorização, que limita a nota). | Alto | P, C | Parcial. Login/sessão, senha PBKDF2, CSRF e contexto por perfil são verificados também por `ReservaAutenticacaoPostgresE2ETest` com MockMvc e PostgreSQL nas três transições. Gestão/recuperação de contas e jornada integrada HTTP de outros fluxos ainda pendentes. |
| INV-09 | RN06 / RF15. Transições de estado. | Aprovar e rejeitar só a partir de SOLICITADA. Alterar horário só em SOLICITADA. Cancelar só em SOLICITADA ou APROVADA. Iniciar em `[inicio, fim)`, registrar ausência a partir de `fim` e concluir somente após início. Transições inválidas dão 409. | Reserva em estado incoerente. | Alto | P, B | Parcial. `Reserva`/`ReservaService` e as rotas iniciam, concluem e registram ausência com autorização e limites temporais. RF15 permanece parcial: `EXPIRADA` e as regras de expiração/alterações pendentes do PRD não estão implementadas. |
| INV-10 | RF06. Posse da reserva e perfil. | Só o solicitante autenticado dono altera/cancela a reserva; o tipo deve corresponder à conta. Solicitante, sala e reserva inexistentes dão 404. | Usuário altera reserva de outro. | Médio | P, B | Parcial. Além das validações do serviço, o contexto autenticado verifica ID/perfil e impede que o cliente substitua a própria identidade. |
| INV-11 | Contrato de erros HTTP. | 400 (argumento ou corpo inválido), 403 (acesso negado), 404 (não encontrado), 409 (conflito ou estado inválido), com corpo de mensagem de erro. | Cliente recebe código errado e trata mal o erro. | Médio | P, B | Implementado. `ApiExceptionHandler`. `IllegalStateException` cobre conflito de horário, sala indisponível e estado inválido, todos como 409. |
| INV-12 | RF06. Consulta de reservas. | Consulta por ID existente devolve a reserva; ID inexistente, 404. Solicitante só lista as próprias reservas; equipe pode consultar a fila. | Dado errado ou vazado ao consultar. | Médio | P, C | Parcial. `ReservaService.buscarReserva` e `listarReservasDoSolicitante` aplicam a identidade autenticada; campos pessoais seguem fora da resposta. |
| INV-13 | RN07 / RF14. Auditoria e não exclusão. | Criação e mudanças de estado geram evento transacional com estado anterior/novo, ator, perfil e data/hora. Reservas iniciadas não podem ser apagadas. | Sem rastreabilidade das mudanças. | Médio | C | Parcial. `AuditoriaReserva` é gravada na mesma transação da criação e das transições; testes unitários e de integração verificam os eventos. Não há endpoint de exclusão nem consulta pública do histórico; alterações de horário não são auditadas. |
| INV-14 | RF18. Contrato OpenAPI. | As respostas reais seguem o `openapi.yaml`, incluindo autenticação, catálogos, reservas, campos e erros documentados. | Documentação diverge do comportamento. | Médio | P | Parcial. Contrato documenta autenticação, salas, catálogos e reservas; ainda requer validação automatizada de conformidade e refinamento de exemplos de erro. |
| INV-15 | RF13 / RN08. Retirada e devolução de materiais. | Retirada e devolução ficam registradas. | Perda de rastreio de equipamento. | Baixo | P, C | Pendente. |
| INV-16 | RF19. Notificação. | Notificação simulada ou integração externa ao aprovar, rejeitar ou cancelar. **Decisão pendente:** evento, payload e resultado observável. | Usuário não é avisado. | Baixo | P | Pendente. Não há cliente de notificação. |
| INV-17 | RF16 e RF17. Relatórios e interface responsiva. | Relatórios de utilização e interface responsiva com mensagens claras. | Usuário sem visibilidade nem usabilidade. | Baixo | P | Parcial. A interface responsiva consome a API de salas e reservas; relatórios e E2E automatizado de navegador não existem. |
| INV-18 | RF03, RF04, RF05 e RF12. Cadastros, pesquisa e validação de alocação docente. | Administrador gerencia recursos e usuários; usuários pesquisam recursos; Responsável valida alocação docente. | Dados mestres só criáveis direto no banco. | Baixo | P | Pendente. Não há endpoints. |

## 3. Observações do código que viram casos de teste

1. **Ordem das validações em `solicitarReserva`:** intervalo (400), sala (404), disponibilidade da sala (409), solicitante (404), tipo do solicitante (403) e conflito (409). Com duas falhas ao mesmo tempo, vale a primeira. Os testes devem fixar essa ordem.
2. **Mesmo 409 para causas diferentes:** conflito de horário, sala indisponível e estado inválido usam `IllegalStateException`. Os testes precisam diferenciar pela mensagem de erro.
3. **A aprovação revalida o estado atual da sala e os conflitos confirmados** para sala, professor e material. Reservas ainda SOLICITADAS são excluídas dessa contagem para permitir aprovar somente uma entre pedidos pendentes sobrepostos; os bloqueios de linhas de sala, professor e material serializam aprovações concorrentes. Isso não implementa agenda de bloqueios/manutenção por período, ainda pendente em INV-07.
4. **Transições operacionais (RF15):** Administrador pode atuar em qualquer reserva; Responsável somente se seu ID autenticado for o de `Reserva.responsavel`; Solicitante não pode executar as transições. O Responsável que aprova passa a assumir a responsabilidade operacional. Conclusão antecipada e ausência não liberam o intervalo original.
5. **Redundância na validação do intervalo.** `validarIntervalo` e `Reserva.isIntervaloValido` repetem a regra. Isso afeta a cobertura de ramos no JaCoCo e vale ser anotado.
6. **Unicidade de e-mail em `Solicitante`:** o defeito anterior com `@UniqueElements` foi corrigido; o campo atual usa `@Email` e `@Column(unique = true)`.
7. **A interface chama a API.** O cliente em `src/main/resources/static/js/reservation-api.js` usa `fetch` para consumir as rotas de salas e reservas; `app.js` atualiza a tela com os dados do servidor. Os testes Node validam o contrato do cliente, mas não há teste E2E automatizado em navegador.
8. **Identidade no corpo da requisição:** `solicitanteId` e `usuarioId` ainda vêm no JSON por compatibilidade contratual, mas o serviço compara esses valores ao contexto da sessão; eles não definem a identidade. As rotas operacionais não aceitam ID no corpo.

## 4. Decisões pendentes que afetam o inventário

| Decisão | Linhas afetadas |
|---|---|
| Quem aprova recurso restrito e como se classifica o recurso | INV-04 |
| Regra para reservas já existentes dentro de um bloqueio | INV-05, INV-07 |
| Evento, payload e resultado observável da notificação | INV-16 |

Enquanto estas decisões não forem registradas em `estrategia.md`, as linhas afetadas permanecem como pendentes e não podem ser marcadas como aprovadas. A abrangência do conflito de docentes e materiais (INV-06) está decidida e implementada, não sendo mais uma decisão pendente.