# Casos de Teste de Caixa Preta

## 1. Escopo e premissas

Estes casos verificam a API de reservas **somente pelo contrato** (`docs/openapi.yaml` e `docs/api-reservas.md`) e pelas regras do `docs/testes/inventario-regras.md`. Nenhum resultado esperado depende da estrutura interna das classes.

- **Ferramenta prevista:** REST-assured contra a aplicação conectada ao PostgreSQL (Testcontainers). Os casos também podem ser executados manualmente com `curl`.
- **Contas para teste manual:** usar a tabela de contas demonstrativas do [README](../../README.md); os usuários de Professor, Coordenador, Responsável e Administrador só são semeados com `APP_SEED_ENABLED=true`. Casos automatizados devem criar seus próprios usuários sintéticos.
- **Técnicas:** valor limite (VL), partição de equivalência (PE), tabela de decisão (TD) e transição de estado (TR).
- **Identificação:** cada caso tem ID `CP-nn` e liga a uma linha do inventário (INV-nn). As transições têm IDs `TR-nn` e a tabela de decisão `TD-nn`.
- **Códigos HTTP:** 201, 200, 400, 403, 404 e 409, conforme o contrato. O corpo de erro tem o campo `mensagem`.
- **Autenticação:** antes dos casos protegidos, autentique o perfil indicado no caso, mantenha o cookie da sessão e envie o `csrfToken` no cabeçalho `X-CSRF-Token` em operações mutáveis. IDs e tipos do payload devem corresponder à conta autenticada.
- **Resultado esperado** vem do contrato e das regras. Onde o contrato é omisso, o caso é marcado com **[verificar]**.
- **Decisão pendente:** casos que dependem de decisão do grupo registrada em `estrategia.md` levam esta marca.

## 2. Massa de teste

Dados sintéticos, recriados antes de cada caso ou suíte.

| Símbolo | Descrição |
|---|---|
| S1 | Sala com capacidade 30, estado DISPONIVEL |
| S2 | Sala em estado EM_MANUTENCAO |
| S3 | Sala em estado RESERVADA |
| S4 | Outra sala DISPONIVEL |
| P1, P2 | Solicitantes do tipo PROFESSOR |
| M1, M2 | Materiais cadastrados |
| C1 | Solicitante do tipo COORDENADOR |
| R1 | Responsável cadastrado |
| A1 | Administrador cadastrado |
| D | Data futura base; D+1 é o dia seguinte |
| E | Reserva existente em S1, de P1, em D das 10:00 às 11:00, estado SOLICITADA (quando o caso diz outro estado, ele é informado) |

Os IDs de R1 e A1 devem ser **diferentes**, para detectar confusão entre os dois cadastros.

## 2.1 Consulta de salas (GET `/api/salas`, RF05)

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-89 | PE | GET `/api/salas`, com várias salas cadastradas | 200; lista contém ID, capacidade e estado corretos para cada sala, sem campos adicionais da entidade |
| CP-90 | PE | GET `/api/salas`, sem salas cadastradas | 200; corpo `[]` |

## 2.2 Cadastro de sala (RF03, RF02)

Para os casos protegidos, autentique a conta indicada, mantenha o cookie da sessão e envie o `csrfToken` no cabeçalho `X-CSRF-Token`. Os cenários CP-120 a CP-123 foram executados por `SalaPostgresIntegrationTest` com PostgreSQL/Testcontainers em `./mvnw -B verify`; os resultados estão registrados em `docs/rtm.md`.

| ID | Técnica | Rastreabilidade | Entrada | Resultado esperado |
|---|---|---|---|---|
| CP-120 | PE | RF03, RF02; INV-18, INV-08 | Administrador autenticado envia somente `capacidade: 20` para criar sala; em seguida consulta `GET /api/salas` | 201; resposta contém ID gerado, capacidade 20 e estado `DISPONIVEL`; a listagem subsequente contém a mesma identidade e os mesmos dados, além das salas existentes. O cliente não envia ID nem estado. |
| CP-121 | VL | RF03; INV-18 | Administrador autenticado tenta criar sala com capacidade 19 | 400; mensagem de erro conforme contrato, nenhuma sala criada e a listagem mantém a contagem/dados anteriores. |
| CP-122 | PE | RF02; INV-08 | Solicitante e Responsável autenticados tentam chamar diretamente a operação de criação | Para cada perfil: 403; nenhuma sala criada e a listagem mantém a contagem/dados anteriores. |
| CP-123 | PE | RF01, RF02; INV-08 | Chamada direta à operação de criação sem sessão autenticada | 401; nenhuma sala criada e a listagem mantém a contagem/dados anteriores. |

O inventário não identifica uma RN numerada específica para a capacidade mínima; por isso, os casos a rastreiam a RF03 e INV-18, sem criar um identificador de regra novo. CP-89 permanece somente como cobertura de listagem, não de criação.

## 2.3 Consulta de reservas pendentes (GET `/api/reservas/pendentes`, RF12)

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-91 | PE | Reservas com status SOLICITADA, APROVADA, REJEITADA e CANCELADA e datas de solicitação distintas | 200; somente SOLICITADA, ordenadas por `solicitadoEm` crescente |
| CP-92 | PE | Nenhuma reserva SOLICITADA | 200; corpo `[]` |

## 2.4 Catálogo de recursos e alocação (RF08-RF09)

Os endpoints de catálogo exigem sessão autenticada. As solicitações devem usar CSRF e as contas sintéticas devem ser criadas pela suíte.

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-93 | PE | GET `/api/professores`, com P1 e P2 cadastrados | 200; lista contém ID e nome dos professores, sem e-mail ou senha |
| CP-94 | PE | GET `/api/materiais`, com M1 e M2 cadastrados | 200; lista contém ID e nome dos materiais |
| CP-95 | PE | Solicitar reserva com professorIds `[P2]` e sem conflito | 201; resposta contém P2 em professorIds |
| CP-96 | PE | Solicitar reserva com materialIds `[M1]` e sem conflito | 201; resposta contém M1 em materialIds |
| CP-97 | PE | Sobreposição com reserva existente que usa o mesmo professor, em outra sala | 409; mensagem identifica conflito de professor |
| CP-98 | PE | Sobreposição com reserva existente que usa o mesmo material, em outra sala | 409; mensagem identifica conflito de material |
| CP-99 | VL | Solicitação por P1 omite P1 de professorIds | 201; a resposta inclui automaticamente P1 |
| CP-100 | PE | Duas solicitações paralelas compartilham um professor ou material e intervalo, usando salas diferentes | No máximo uma retorna 201; a outra retorna 409 |

## 3. Intervalo da reserva (INV-01)

### 3.1 Valor limite ao solicitar

Requisição: POST `/api/reservas` com S1, P1, PROFESSOR, no dia D, sem reservas prévias.

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-01 | VL | `inicio` ausente | 400 |
| CP-02 | VL | `fim` ausente | 400 |
| CP-03 | VL | `inicio` 11:00 e `fim` 10:00 (fim antes do início) | 400 |
| CP-04 | VL | `inicio` 10:00 e `fim` 10:00 (iguais) | 400 |
| CP-05 | VL | `inicio` 10:00 e `fim` 10:01 (um minuto de duração) | 400; horários devem usar incrementos de 30 minutos |
| CP-06 | PE | `inicio` 08:00 e `fim` 18:00 (intervalo longo) | 201 |
| CP-07 | PE | `inicio` D 23:00 e `fim` D+1 01:00 (atravessa a meia-noite) | 201 |
| CP-08 | PE | `inicio` e `fim` ausentes | 400 |

### 3.2 Alterar horário (PUT `/api/reservas/{id}/horario`)

Pré-condição: reserva SOLICITADA de P1 em S1.

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-09 | PE | Novo intervalo válido | 200, horário atualizado, status continua SOLICITADA |
| CP-10 | VL | `fim` igual ao `inicio` | 400 |
| CP-11 | VL | `fim` antes do `inicio` | 400 |
| CP-12 | PE | `inicio` ausente | 400 |

### 3.3 Campos obrigatórios e formato (POST `/api/reservas`)

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-13 | PE | Corpo ausente | 400 |
| CP-14 | PE | JSON malformado | 400 |
| CP-15 | PE | `salaId` ausente | 400 |
| CP-16 | VL | `salaId` igual a 0 | 400 |
| CP-17 | PE | `salaId` negativo | 400 |
| CP-18 | PE | `solicitanteId` ausente | 400 |
| CP-19 | VL | `solicitanteId` igual a 0 | 400 |
| CP-20 | PE | `tipoSolicitante` ausente | 400 |
| CP-21 | PE | `tipoSolicitante` fora do enum (por exemplo ALUNO) | 400 |
| CP-22 | PE | `inicio` em formato inválido (por exemplo texto livre) | 400 |
| CP-23 | PE | `salaId` não numérico (por exemplo abc) | 400 |

## 4. Sobreposição de horários (INV-02)

### 4.1 Partições de sobreposição na mesma sala

Pré-condição: E existe (S1, 10:00 às 11:00, SOLICITADA). Requisição: POST `/api/reservas` por P2 na sala indicada.

| ID | Técnica | Intervalo pedido | Resultado esperado |
|---|---|---|---|
| CP-24 | PE | S1, 10:00 às 11:00 (idêntico) | 409 |
| CP-25 | PE | S1, 10:15 às 10:45 (contido em E) | 409 |
| CP-26 | PE | S1, 09:00 às 12:00 (contém E) | 409 |
| CP-27 | PE | S1, 09:30 às 10:30 (sobrepõe o início) | 409 |
| CP-28 | PE | S1, 10:30 às 11:30 (sobrepõe o fim) | 409 |
| CP-29 | VL | S1, 11:00 às 12:00 (adjacente depois) | 201 |
| CP-30 | VL | S1, 09:00 às 10:00 (adjacente antes) | 201 |
| CP-31 | PE | S1, 13:00 às 14:00 (separado) | 201 |
| CP-32 | PE | S4, 10:00 às 11:00 (mesmo horário, outra sala) | 201 |
| CP-33 | PE | S1, 10:00 às 11:00 em D+1 (mesmo horário, outro dia) | 201 |
| CP-34 | VL | S1, 10:00 às 10:01 (um minuto dentro de E) | 409 |

### 4.2 Estados da reserva existente que ocupam a sala

Pré-condição: E no estado indicado. Requisição: P2 pede S1, 10:00 às 11:00.

| ID | Técnica | Estado de E | Resultado esperado |
|---|---|---|---|
| CP-35 | PE | APROVADA | 409 |
| CP-36 | PE | CANCELADA | 201 |
| CP-37 | PE | REJEITADA | 201 |
| CP-38 | PE | EM_USO | 409. **Não executável pela API**: não há endpoint que leve a reserva a EM_USO. Só com o estado preparado direto no banco. |

### 4.3 Conflito ao alterar horário

| ID | Técnica | Pré-condição e entrada | Resultado esperado |
|---|---|---|---|
| CP-39 | PE | E existe. P1 altera E para 10:00 às 11:30 (sobrepõe somente ela mesma) | 200 |
| CP-40 | PE | E (10:00 às 11:00) e F (12:00 às 13:00) existem em S1. Altera F para 10:30 às 12:30 | 409 |
| CP-41 | VL | E e F existem. Altera F para 11:00 às 12:00 (adjacente a E) | 200 |

## 5. Disponibilidade da sala (INV-05)

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-42 | PE | POST para S2 (EM_MANUTENCAO) | 409 |
| CP-43 | PE | POST para S3 (RESERVADA) | 409 |
| CP-44 | PE | POST com `salaId` inexistente (por exemplo 9999) | 404 |
| CP-45 | PE | Reserva SOLICITADA em S1; a sala passa a EM_MANUTENCAO (estado preparado); P1 altera o horário | 409 |

## 6. Solicitante, perfil e ordem das validações (INV-10)

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-46 | PE | POST com `solicitanteId` inexistente | 404 |
| CP-47 | PE | P1 (PROFESSOR) informa `tipoSolicitante` COORDENADOR | 403 |
| CP-48 | PE | C1 informa `tipoSolicitante` COORDENADOR | 201 |
| CP-49 | PE | C1 informa `tipoSolicitante` PROFESSOR | 403 |
| CP-50 | PE | Intervalo inválido e sala inexistente ao mesmo tempo | 400 (o intervalo é validado primeiro) |
| CP-51 | PE | Sala inexistente e solicitante inexistente ao mesmo tempo | 404 com mensagem citando a sala |
| CP-52 | PE | S2 (EM_MANUTENCAO) e solicitante inexistente ao mesmo tempo | 409 (a disponibilidade da sala vem antes do solicitante) |
| CP-53 | PE | E existe; P1 informa COORDENADOR no mesmo intervalo de E | 403 (o perfil é checado antes do conflito) |

Os casos CP-50 a CP-53 fixam a ordem das validações. Se a ordem mudar, os casos precisam ser revisados de propósito.

## 7. Decisão sobre a reserva (RF10, RF15, RN05, INV-04 e INV-09)

Pré-condição: reserva SOLICITADA de P1 em S1. Endpoints: POST `/aprovacao` e POST `/rejeicao`.

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-54 | PE | R1 aprova (tipoAprovador RESPONSAVEL) | 200, status APROVADA, `responsavelId` igual a R1, `administradorId` nulo, `aprovadoEm` preenchido |
| CP-55 | PE | A1 aprova reserva sem material restrito | 403; a reserva permanece SOLICITADA e sem decisor |
| CP-156 | PE | A1 aprova reserva com material restrito | 403; a reserva permanece SOLICITADA e sem decisor |
| CP-157 | PE | Administrador autenticado tenta aprovar reserva sem material restrito, com CSRF válido | 403; a reserva permanece SOLICITADA e sem auditoria de aprovação |
| CP-56 | PE | R1 rejeita | 200, status REJEITADA, `responsavelId` igual a R1, `aprovadoEm` nulo |
| CP-57 | PE | A1 rejeita | 200, status REJEITADA, `administradorId` igual a A1 |
| CP-58 | PE | Aprova reserva inexistente | 404 |
| CP-59 | PE | Aprova com `usuarioId` de Responsável inexistente (tipo RESPONSAVEL) | 404 |
| CP-60 | PE | Aprova com `usuarioId` de Administrador inexistente (tipo ADMINISTRADOR) | 404 |
| CP-61 | PE | Aprova com o ID de R1 e tipo ADMINISTRADOR (sem administrador com esse ID) | 404 |
| CP-62 | PE | `tipoAprovador` fora do enum | 400 |
| CP-63 | PE | `tipoAprovador` ausente | 400 |
| CP-64 | PE | `usuarioId` ausente | 400 |
| CP-65 | VL | `usuarioId` igual a 0 | 400 |
| CP-66 | PE | Rejeita reserva inexistente | 404 |

## 8. Cancelamento e alteração por solicitante (INV-09 e INV-10)

| ID | Técnica | Pré-condição e entrada | Resultado esperado |
|---|---|---|---|
| CP-67 | PE | P1 cancela a própria reserva SOLICITADA | 200, status CANCELADA |
| CP-68 | PE | P1 cancela a própria reserva APROVADA | 200, status CANCELADA |
| CP-69 | PE | Após CP-67, P2 pede o mesmo intervalo na mesma sala | 201 (o cancelamento libera o horário) |
| CP-70 | PE | Após a rejeição da reserva, P2 pede o mesmo intervalo | 201 (a rejeição libera o horário) |
| CP-71 | PE | P2 cancela a reserva de P1 | 403, documentado no OpenAPI. |
| CP-72 | PE | Cancela reserva inexistente | 404 |
| CP-73 | PE | Cancelamento com `usuarioId` ausente | 400 |
| CP-74 | VL | Cancelamento com `usuarioId` igual a 0 | 400 |
| CP-75 | PE | P2 altera o horário da reserva de P1 | 403, documentado no OpenAPI. |
| CP-76 | PE | Altera horário de reserva inexistente | 404 |
| CP-77 | PE | Alteração com `solicitanteId` ausente | 400 |

## 9. Consulta (INV-12)

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-78 | PE | GET de reserva existente | 200 com os campos do schema Reserva |
| CP-79 | PE | GET de reserva inexistente | 404 |
| CP-80 | PE | GET com ID não numérico (por exemplo abc) | 400. **[verificar]** o formato do corpo, que pode fugir do schema Erro. |
| CP-81 | PE | Lista de P1 com três reservas em horários diferentes | 200, ordenada por `inicio` do mais recente ao mais antigo |
| CP-82 | PE | Lista de solicitante sem reservas | 200 com lista vazia |
| CP-83 | PE | Lista de solicitante inexistente | 200 com lista vazia **[verificar]**: o contrato não define 404 |
| CP-84 | PE | Seguir o cabeçalho `Location` devolvido no 201 | O GET nessa URI responde 200 com a mesma reserva |

## 10. Contrato de resposta (INV-11 e INV-14)

| ID | Técnica | Verificação | Resultado esperado |
|---|---|---|---|
| CP-85 | PE | Respostas 400, 403, 404 e 409 de qualquer endpoint | O corpo tem o campo `mensagem` com texto não vazio |
| CP-86 | PE | Corpo do 201 de uma solicitação | Contém `id`, `salaId`, `solicitanteId`, `inicio`, `fim`, status SOLICITADA e `solicitadoEm` preenchido; `responsavelId`, `administradorId` e `aprovadoEm` nulos |
| CP-87 | PE | Campos de data nas respostas | Formato date-time conforme o contrato |
| CP-88 | PE | Revisão documental do `openapi.yaml` | O contrato declara 403 em cancelamento e alteração de horário. A conformidade automatizada entre contrato e respostas reais permanece pendente. |

## 11. Matriz de transição de estados (INV-09)

Cada linha parte de uma reserva no estado de origem e aplica uma operação. O resultado é o código HTTP e o estado final.

| ID | Estado de origem | Operação | Resultado esperado |
|---|---|---|---|
| TR-01 | SOLICITADA | Aprovar | 200, APROVADA |
| TR-02 | SOLICITADA | Rejeitar | 200, REJEITADA |
| TR-03 | SOLICITADA | Cancelar | 200, CANCELADA |
| TR-04 | SOLICITADA | Alterar horário | 200, continua SOLICITADA |
| TR-05 | APROVADA | Aprovar | 409 |
| TR-06 | APROVADA | Rejeitar | 409 |
| TR-07 | APROVADA | Cancelar | 200, CANCELADA |
| TR-08 | APROVADA | Alterar horário | 409 |
| TR-09 | REJEITADA | Aprovar | 409 |
| TR-10 | REJEITADA | Rejeitar | 409 |
| TR-11 | REJEITADA | Cancelar | 409 |
| TR-12 | REJEITADA | Alterar horário | 409 |
| TR-13 | CANCELADA | Aprovar | 409 |
| TR-14 | CANCELADA | Rejeitar | 409 |
| TR-15 | CANCELADA | Cancelar | 409 |
| TR-16 | CANCELADA | Alterar horário | 409 |
| TR-17 | APROVADA | Iniciar em `[inicio, fim)` | 200, EM_USO |
| TR-18 | EM_USO | Concluir, inclusive antes de `fim` | 200, CONCLUIDA |
| TR-19 | APROVADA | Registrar não comparecimento em ou após `fim` | 200, NAO_COMPARECEU |

### Transições operacionais (RF15, RN06, RN07)

Relógio controlado. Administrador pode operar qualquer reserva; Responsável, somente quando seu ID autenticado for igual a `Reserva.responsavel`; Solicitante não pode executar as operações. O Responsável que aprova a reserva passa a assumir sua responsabilidade operacional.

| ID | Técnica | Pré-condição e entrada | Resultado esperado |
|---|---|---|---|
| CP-129 | U | `APROVADA`, agora exatamente em `inicio`, ator Responsável vinculado | `EM_USO`, auditoria `APROVADA` → `EM_USO` |
| CP-130 | U | `APROVADA`, agora um nanossegundo antes de `fim` | Início permitido; `EM_USO` |
| CP-131 | U | `APROVADA`, agora antes de `inicio` ou exatamente em `fim` | 409; estado e auditoria inalterados |
| CP-132 | U | `APROVADA`, agora exatamente em `fim` | `NAO_COMPARECEU`, auditoria registrada |
| CP-133 | U | `APROVADA`, agora um nanossegundo antes de `fim`; POST `/nao-comparecimento` | 409; estado e auditoria inalterados |
| CP-134 | U | `EM_USO`, agora antes de `fim` | `CONCLUIDA`, auditoria registrada |
| CP-135 | U | Solicitante tenta qualquer transição operacional | Acesso negado; estado inalterado |
| CP-136 | U | Responsável diferente do armazenado em `Reserva.responsavel` tenta operar | 403; estado inalterado |
| CP-137 | U | Administrador atua em reserva sem Responsável associado | Operação permitida e ator registrado como Administrador |
| CP-138 | I | Criar reserva pelo serviço no PostgreSQL | Um evento com estado anterior nulo e novo `SOLICITADA` persiste com a reserva |
| CP-139 | I | Responsável autorizado conclui uma reserva `EM_USO` no PostgreSQL | `CONCLUIDA` e evento com estados anterior/novo, ator e data/hora persistidos |
| CP-140 | I | Reserva `CONCLUIDA` antecipadamente; pedir intervalo que sobrepõe o original, antes de seu fim | 409; intervalo original permanece ocupado |
| CP-141 | I | Contexto autenticado como Solicitante tenta concluir reserva persistida | Acesso negado; estado permanece `EM_USO` e nenhum evento de transição é gravado |
| CP-142 | PE | Administrador autenticado opera via HTTP reserva sob responsabilidade alheia, em cada uma das três operações | 200; estado e auditoria persistidos registram o Administrador da sessão |
| CP-143 | PE | Responsável autenticado opera via HTTP sua própria reserva, em cada uma das três operações | 200; estado e auditoria persistidos registram o Responsável da sessão |
| CP-144 | PE | Responsável autenticado tenta via HTTP operar reserva de outro Responsável, em cada operação | 403; estado permanece e auditoria não é criada, mesmo enviando ID/perfil forjados no corpo |
| CP-145 | PE | Solicitante autenticado tenta via HTTP cada uma das três operações | 403; estado permanece e auditoria não é criada, mesmo enviando dados de Administrador no corpo |
| CP-146 | PE | Sessão anônima com CSRF válido tenta cada operação | 401; estado permanece e auditoria não é criada |
| CP-147 | PE | Sessão autenticada sem `X-CSRF-Token` tenta cada operação | 403; estado permanece e auditoria não é criada |
| CP-148 | PE | Sessão autenticada com token CSRF inválido tenta cada operação | 403; estado permanece e auditoria não é criada |
| CP-149 | PE | Requisição sem sessão e sem token CSRF tenta cada operação | 403; o interceptor verifica CSRF antes de autenticação; estado e auditoria permanecem inalterados |

O intervalo segue `[inicio, fim)`: concluir antecipadamente ou registrar ausência não o encurta; reserva consecutiva iniciada exatamente em `fim` permanece permitida.

## 12. Intervalos futuros em incrementos de 30 minutos (RF07, RN01)

Os casos inválidos abaixo são aplicados tanto ao POST `/api/reservas` quanto ao PUT `/api/reservas/{id}/horario`. O início e término válidos usam uma data futura, segundos/nanos zerados e término posterior.

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-101 | VL | Início em data passada | 400; mensagem informa que o início deve estar no futuro |
| CP-102 | VL | Início exatamente no momento atual | 400; mensagem informa que o início deve estar no futuro |
| CP-103 | VL | Início um minuto no futuro | 400; mensagem informa incrementos de 30 minutos |
| CP-104 | VL | Início no minuto 15 | 400; mensagem informa incrementos de 30 minutos |
| CP-105 | VL | Início no minuto 45 | 400; mensagem informa incrementos de 30 minutos |
| CP-106 | VL | Término no minuto 15 | 400; mensagem informa incrementos de 30 minutos |
| CP-107 | VL | Término no minuto 45 | 400; mensagem informa incrementos de 30 minutos |
| CP-108 | VL | Segundos do início diferentes de zero | 400; mensagem informa segundos/nanos zerados |
| CP-109 | VL | Segundos do término diferentes de zero | 400; mensagem informa segundos/nanos zerados |
| CP-110 | VL | Nanos do início diferentes de zero | 400; mensagem informa segundos/nanos zerados |
| CP-111 | VL | Nanos do término diferentes de zero | 400; mensagem informa segundos/nanos zerados |
| CP-112 | VL | Término igual ao início | 400; mensagem informa que o término deve ser posterior |
| CP-113 | VL | Término anterior ao início | 400; mensagem informa que o término deve ser posterior |
| CP-114 | PE | Intervalo futuro 10:00–11:00 | 201, status SOLICITADA |
| CP-115 | PE | Intervalo futuro 10:30–12:00 | 201, status SOLICITADA |
| CP-116 | VL | Novo intervalo começa exatamente quando termina a reserva anterior | 201; intervalos adjacentes não se sobrepõem |
| CP-117 | VL | Relógio de São Paulo em 2026-11-12 23:50; solicitar 23:30–00:00 | 400; o início já passou no mesmo dia |
| CP-118 | VL | Relógio de São Paulo em 2026-11-12 23:50; solicitar 2026-11-13 00:00–01:00 | 201; o início do dia seguinte é futuro |
| CP-119 | VL | Relógio de São Paulo em 2026-11-12 01:00; solicitar 00:30–01:00 | 400; o início já passou no mesmo dia |

## 13. Tabela de decisão da aprovação (INV-04, RN05)

A classificação de recurso restrito é demonstrativa; não altera quem pode aprovar. Toda reserva nova só pode ser aprovada pelo Responsável.

| ID | Perfil do aprovador | Recurso restrito | Resultado esperado | Situação |
|---|---|---|---|---|
| TD-01 | Responsável | Não | Aprova (200) | Executável (CP-54) |
| TD-02 | Administrador | Não | Recusa (403), reserva permanece SOLICITADA | Executável (CP-55/CP-157) |
| TD-03 | Responsável | Sim | Aprova (200) | Executável; classificação não altera a regra |
| TD-04 | Administrador | Sim | Recusa (403), reserva permanece SOLICITADA | Executável (CP-156) |

## 14. Gestão de estado de sala, filtros e auditoria (RF02, RF04, RF05, RF11, RF14, RF15, RN07, INV-05, INV-08, INV-13 e INV-18)

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-150 | PE | GET `/api/salas?status=EM_MANUTENCAO&capacidadeMinima=30` | 200; somente salas em manutenção com capacidade mínima de 30 |
| CP-151 | PE | Administrador envia `PUT /api/salas/{id}/status` com `EM_MANUTENCAO` | 200; estado persistido; nova solicitação e aprovação são recusadas; reservas existentes são preservadas |
| CP-152 | PE | Solicitante, Responsável e sessão anônima tentam alterar estado da sala | 403 para os perfis autenticados sem permissão e 401 sem sessão; nenhuma alteração persistida |
| CP-153 | PE | Responsável consulta `GET /api/reservas/{id}/auditoria`; Solicitante e sessão anônima tentam a mesma rota | Responsável recebe eventos; Solicitante recebe 403; sessão anônima recebe 401 |
| CP-154 | PE | Equipe consulta `GET /api/reservas/{id}/historico-horario` após alteração aprovada | 200; horários anterior/novo, responsável e instante são retornados em ordem cronológica |
| CP-155 | PE | Aplicar migration com evento de ação automática sem usuário associado | Migration permite ator `SISTEMA` com `usuarioId` nulo, preservando integridade do evento |
| CP-159 | VL | Administrador envia alteração de estado sem informar `statusSala` | 400; serviço não altera a sala |
| CP-124 | PE | Reserva SOLICITADA em S1; S1 passa a EM_MANUTENCAO antes da aprovação | 409; a reserva continua SOLICITADA e não é salva como APROVADA |
| CP-125 | PE | E SOLICITADA sobrepõe reserva APROVADA da mesma sala | 409; E continua SOLICITADA |
| CP-126 | PE | E SOLICITADA sobrepõe reserva APROVADA do mesmo professor em outra sala | 409; E continua SOLICITADA |
| CP-127 | PE | E SOLICITADA sobrepõe reserva APROVADA do mesmo material em outra sala | 409; E continua SOLICITADA |
| CP-128 | C | Duas reservas SOLICITADAS preexistentes e conflitantes são aprovadas simultaneamente no PostgreSQL | Exatamente uma fica APROVADA; a outra recebe 409 e permanece SOLICITADA |

## 15. Casos parcialmente cobertos ou que não podem ser executados hoje

| Assunto | Motivo | Linha do inventário |
|---|---|---|
| Bloqueios e manutenção por período | Não há agenda nem endpoint | INV-07 |
| Autenticação e autorização por perfil | Login, sessão e regras de perfil têm testes Java; falta jornada E2E de navegador | INV-08 |
| Auditoria e não exclusão | Há trilha de estados e endpoint protegido; falta evidência completa de imutabilidade e de todos os tipos de alteração | INV-13 |
| Alterações de horário | Falta solicitação pendente, aprovação do Responsável e revalidação/expiração do fluxo | INV-04, INV-09 |
| Expiração | A expiração de solicitações novas ocorre sob demanda em consultas; não há agendador independente | INV-09 |
| Validação de docentes | Não há fluxo de validação pelo Responsável | INV-18 |
| Retirada e devolução de materiais | Não há fluxo implementado | INV-14 |
| Relatórios de utilização e conflitos | Não há endpoints/relatórios implementados | INV-15 |
| Notificação simulada interna | A integração HTTP existente diverge do RF19/RN11 do PRD | INV-16 |
| Validação visual e jornada E2E | Testes de API/Node existem; faltam execução de navegador e validação visual/acessibilidade | INV-17 |

Estes itens permanecem registrados como **bloqueados ou pendentes**. Eles não podem ser reportados como aprovados.

## 16. Resumo

| Grupo | Quantidade |
|---|---|
| Casos de execução (CP-01 a CP-159) | 159 |
| Transições de estado (TR-01 a TR-19) | 19 |
| Tabela de decisão (TD-01 a TD-04) | 4 |
| Executáveis com estado preparado no banco | CP-38, CP-45, CP-125–128 e CP-138–149 |
| Com decisão pendente | Nenhuma regra de aprovação; pendências funcionais estão na seção 15 |
| Com verificação adicional | CP-80 e CP-83 |
| Casos novos com execução automatizada nesta revisão | CP-150–159 |

## 17. Instalação limpa e inicialização (RNF-ISO-08)

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| CP-158 | PE | Iniciar a aplicação com PostgreSQL 16 vazio, sem tabelas ou histórico Flyway prévios | A aplicação inicia, `GET /api/auth/csrf` retorna 200 e as migrations criam as tabelas necessárias sem erro |