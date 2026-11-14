# API de Reservas

**Status:** Implementação parcial; consultar a [RTM única](./RTM.md) para estados por requisito.
**Versao do contrato:** 1.3.0
**Contrato OpenAPI:** [openapi.yaml](openapi.yaml)

## Escopo

A API autentica as contas cadastradas com senha protegida por hash e sessao HTTP, sem JWT. Ela expoe a consulta e o cadastro de salas pelo Administrador, alem dos fluxos de reserva implementados: solicitacao, consulta, listagem filtrada por estado ou solicitante, alteracao de horario, cancelamento, aprovacao e rejeicao. O frontend servido pelo Spring Boot usa essas rotas; os dados apresentados sao lidos e gravados no backend, nao em uma lista local. O contrato OpenAPI pode ser aberto no Swagger Editor ou carregado por uma instalacao Swagger UI.

O repositorio possui configuracao Maven e aplicacao Spring Boot. A interface Swagger UI, entretanto, nao esta publicada pela aplicacao porque o Springdoc ainda nao foi configurado; este contrato pode ser aberto no Swagger Editor ou carregado por uma instalacao Swagger UI.

## Validacao das regras de negocio

Esta API implementa apenas parte das regras previstas no PRD. O comportamento observado no backend e:

| Regra | Situacao atual |
|---|---|
| RN01 - Intervalo | Implementada: o início deve estar no futuro segundo o relógio de `America/Sao_Paulo`; início e término devem estar nos minutos 00 ou 30, com segundos e nanos zerados; o término deve ser posterior ao início. |
| RN02 - Conflito de horario | Implementada para sala, docentes associados e materiais associados. Intervalos adjacentes sao aceitos. Na criação, a consulta de conflito também considera `SOLICITADA`. |
| RN03 - Concorrencia | Os locks de PostgreSQL e testes Testcontainers protegem criação, aprovação e alteração simultâneas; no máximo uma operação conflitante é aceita. Limite: o PRD permite pedidos pendentes coincidentes, mas a criação atual recusa o segundo pedido porque inclui `SOLICITADA` na consulta de conflito. |
| RN04 - Bloqueio/manutencao | Parcial: Administrador pode alterar o estado operacional da sala; `EM_MANUTENCAO` impede novas solicitações e aprovações. Ainda não há bloqueios por período ou alerta automático ao Responsável. |
| RN05 - Aprovacao | Parcial: toda reserva nova só pode ser aprovada por Responsável, independentemente dos recursos; Administrador recebe 403. A aprovação de alterações de horário ainda não tem fluxo próprio. |
| RN06 - Ciclo de estados | Parcial: as transições operacionais estão disponíveis e solicitações não decididas até o início expiram para `EXPIRADA`. A expiração é aplicada ao consultar reservas; alterações pendentes e sua expiração ainda não estão implementadas. |
| RN07 - Proteção e auditoria | Parcial: criação, decisões, cancelamento e transições registram auditoria transacional com ator/perfil e data/hora; consultas de auditoria e histórico de alteração de horário são restritas à equipe. Alterações diretas de horário enquanto a reserva está solicitada ainda não geram um evento de auditoria. |
| RN08 - Materiais | Parcial: materiais podem ser associados a reservas e têm consulta de catálogo. Retirada e devolução ainda não são registradas. |
| RF19 / RN11 - Notificações | Parcial e divergente do PRD: o backend envia eventos de aprovação, rejeição e cancelamento por HTTP e a integração é testada com WireMock; o PRD vigente pede notificações simuladas dentro da aplicação, sem serviço externo. |

O schema OpenAPI descreve `EXPIRADA` como estado consultável em
`GET /api/reservas?status=EXPIRADA`. A expiração de novas solicitações é
processada sob demanda nas consultas; não existe agenda independente.

Na criacao, o payload precisa incluir `tipoSolicitante` (`PROFESSOR` ou `COORDENADOR`), e o backend exige correspondencia com a conta autenticada. Aprovar exige perfil `RESPONSAVEL`; Administrador recebe `403 Forbidden`, mesmo em reservas sem recurso restrito. Rejeitar aceita Responsável ou Administrador. O ID e o tipo enviados no payload precisam corresponder à conta autenticada. Solicitantes só consultam e alteram suas próprias reservas; filas de decisão são restritas a Responsável e Administrador.

Nas operações operacionais, `ADMINISTRADOR` pode atuar em qualquer reserva. `RESPONSAVEL` pode atuar apenas quando seu ID autenticado corresponde a `Reserva.responsavel`; assim, quem aprova como Responsável também assume a responsabilidade operacional. `SOLICITANTE` não pode iniciar, concluir ou registrar ausência. Essas três rotas não recebem ID de usuário: identidade e perfil vêm exclusivamente da sessão autenticada.

Reservas podem incluir `professorIds` e `materialIds` opcionais. Cada professor precisa ser uma conta existente do tipo `PROFESSOR`; quando quem solicita é Professor, sua própria conta é associada automaticamente, mesmo se a lista estiver vazia. Os materiais precisam existir no catálogo. O sistema rejeita sobreposição de intervalo em qualquer sala, docente ou material associado. As listas de catálogo não retornam e-mail nem senha.

## Rotas

| Metodo | Rota | Finalidade | Sucesso |
|---|---|---|---|
| `GET` | `/api/auth/csrf` | Obter token CSRF antes de autenticar | `200 OK` |
| `POST` | `/api/auth/login` | Autenticar e iniciar sessão HTTP | `200 OK` |
| `GET` | `/api/auth/me` | Consultar conta da sessão atual | `200 OK` |
| `POST` | `/api/auth/logout` | Encerrar a sessão atual | `204 No Content` |
| `GET` | `/api/salas?status=DISPONIVEL&capacidadeMinima=30` | Listar/filtrar salas por estado e capacidade mínima | `200 OK` |
| `POST` | `/api/salas` | Cadastrar sala (somente Administrador) | `201 Created` |
| `PUT` | `/api/salas/{salaId}/status` | Alterar estado operacional (somente Administrador) | `200 OK` |
| `GET` | `/api/professores` | Listar docentes cadastrados para alocação (somente ID e nome) | `200 OK` |
| `GET` | `/api/materiais` | Listar materiais cadastrados para alocação | `200 OK` |
| `GET` | `/api/reservas?status=SOLICITADA` | Listar reservas pelo estado | `200 OK` |
| `POST` | `/api/reservas` | Solicitar reserva | `201 Created` |
| `GET` | `/api/reservas/{reservaId}` | Consultar reserva | `200 OK` |
| `GET` | `/api/reservas/{reservaId}/auditoria` | Consultar eventos de estado (equipe) | `200 OK` |
| `GET` | `/api/reservas/{reservaId}/historico-horario` | Consultar alterações aplicadas de horário (equipe) | `200 OK` |
| `GET` | `/api/reservas/solicitante/{solicitanteId}` | Listar reservas do solicitante | `200 OK` |
| `GET` | `/api/reservas/pendentes` | Listar reservas `SOLICITADA`, ordenadas por data da solicitação crescente | `200 OK` |
| `PUT` | `/api/reservas/{reservaId}/horario` | Alterar horario enquanto solicitada | `200 OK` |
| `POST` | `/api/reservas/{reservaId}/cancelamento` | Cancelar reserva solicitada ou aprovada | `200 OK` |
| `POST` | `/api/reservas/{reservaId}/aprovacao` | Aprovar reserva solicitada | `200 OK` |
| `POST` | `/api/reservas/{reservaId}/rejeicao` | Rejeitar reserva solicitada | `200 OK` |
| `POST` | `/api/reservas/{reservaId}/iniciar` | Iniciar utilização durante o intervalo reservado | `200 OK` |
| `POST` | `/api/reservas/{reservaId}/concluir` | Concluir uma utilização em andamento | `200 OK` |
| `POST` | `/api/reservas/{reservaId}/nao-comparecimento` | Registrar ausência a partir do fim reservado | `200 OK` |

Todas as rotas da API, exceto `GET /api/auth/csrf` e `POST /api/auth/login`, exigem sessão autenticada. O cadastro e a alteração de estado das salas exigem perfil Administrador; outros perfis recebem `403 Forbidden`, e chamadas sem sessão recebem `401 Unauthorized`. As consultas de auditoria e histórico de horário exigem perfil Responsável ou Administrador. Requisições de alteração também precisam enviar o token da sessão no cabeçalho `X-CSRF-Token`. O cliente do frontend faz essa troca automaticamente. O navegador precisa ser servido pela mesma aplicação/origem da API.

### Códigos de erro e notificações

- `400 Bad Request`: corpo ausente/malformado, parâmetro ou intervalo inválido.
- `401 Unauthorized`: sessão ausente ou expirada; `GET /api/reservas/pendentes`
  também exige sessão.
- `403 Forbidden`: CSRF ausente/inválido ou perfil/identidade sem permissão.
  Em particular, Administrador não aprova reservas; só Responsável aprova.
- `404 Not Found`: recurso ou reserva inexistente quando a rota o exige.
- `409 Conflict`: sobreposição, sala indisponível ou transição/estado inválido.

Os exemplos de cadastro de sala, sessão e mutações correspondem aos DTOs e
respostas exercitados por `SalaControllerTest`,
`ReservaControllerTest`, `AutenticacaoControllerTest` e
`ReservaNotificacaoWireMockIntegrationTest`. O teste WireMock verifica o payload
`NotificacaoReserva` enviado ao endpoint HTTP configurado, resposta 200/corpo,
resposta 500 e timeout. Essa integração externa existe no código, mas não
satisfaz RF19/RN11, que especifica notificação simulada interna; não é declarada
como requisito implementado.

### Contas de demonstração

Com `APP_SEED_ENABLED=true` (padrão local), a aplicação cria as contas e senhas demonstrativas listadas no README. As senhas são armazenadas com PBKDF2; registros antigos com a senha fictícia são atualizados pelo inicializador. Não use essas credenciais em produção.

Exemplo de consulta da fila:

```http
GET /api/reservas?status=SOLICITADA
Accept: application/json
```

`status` e obrigatorio em `GET /api/reservas` e aceita somente os valores do enum `StatusReserva`. A resposta contem os campos da reserva e o ID do solicitante, sem dados pessoais do cadastro.

### Transições operacionais (RF15, RN06 e RN07)

As operações `iniciar`, `concluir` e `nao-comparecimento` são `POST` sem corpo e exigem sessão autenticada, token CSRF, perfil permitido e estado de origem correto. Retornam a reserva atualizada; ausência de sessão resulta em `401`, perfil/identidade sem permissão em `403`, reserva inexistente em `404` e estado ou limite temporal inválido em `409`.

- `APROVADA` → `EM_USO`: somente se `agora >= inicio && agora < fim`.
- `APROVADA` → `NAO_COMPARECEU`: somente se `agora >= fim`.
- `EM_USO` → `CONCLUIDA`: a qualquer momento após iniciada, inclusive antes de `fim`.
- Conclusão antecipada e ausência não encurtam o intervalo reservado. Conflitos usam o intervalo original `[inicio, fim)`, incluindo reservas `CONCLUIDA` e `NAO_COMPARECEU`; intervalos adjacentes continuam permitidos.
- Cada criação registra `estadoAnterior = null` e `novoEstado = SOLICITADA`. Cada transição grava os estados anterior e novo, ID/perfil do ator e data/hora na mesma transação da reserva.

## Integracao do frontend

O frontend autentica no servidor, restaura a sessão ao recarregar a página e mostra a área correspondente ao perfil retornado: Solicitante, Responsável ou Administrador. O ID e o tipo de solicitante/aprovador são derivados da conta autenticada; não existe mais configuração manual de IDs. A sessão expira após 30 minutos de inatividade. Na tela de Administrador, é possível cadastrar uma sala informando somente sua capacidade (mínimo 20); o servidor gera o ID e inicia o estado como `DISPONIVEL`. Edição/desativação de salas e cadastro de outros recursos ainda não estão disponíveis.

### Cadastrar sala

Somente a conta de perfil `ADMINISTRADOR` pode cadastrar uma sala. O corpo aceita exclusivamente a capacidade; o ID é gerado pelo banco e o estado inicial é `DISPONIVEL`.

```http
POST /api/salas
Content-Type: application/json
X-CSRF-Token: <token da sessão>

{"capacidade":20}
```

Retorna `201 Created` com a representação persistida:

```json
{"id":2,"capacidade":20,"statusSala":"DISPONIVEL"}
```

Capacidade ausente ou inferior a 20 retorna `400 Bad Request`; sessão ausente retorna `401 Unauthorized`; perfil sem permissão retorna `403 Forbidden`. O catálogo `GET /api/salas` apresenta a nova sala nas consultas seguintes.

### Filtrar salas

`GET /api/salas` aceita os filtros opcionais `status` e `capacidadeMinima`. Por
exemplo, `GET /api/salas?status=DISPONIVEL&capacidadeMinima=30` retorna salas
disponíveis com capacidade mínima de 30 pessoas. Localização ainda não existe no
modelo de sala, portanto esse filtro permanece pendente.

### Marcar sala em manutenção

Somente Administrador pode alterar o estado. As reservas já existentes são
preservadas, mas novas solicitações e aprovações para a sala são recusadas
enquanto ela estiver indisponível.

```http
PUT /api/salas/2/status
Content-Type: application/json
X-CSRF-Token: <token da sessão>

{"statusSala":"EM_MANUTENCAO"}
```

### Consultar auditoria

Responsável e Administrador podem consultar mudanças de estado e histórico
imutável de alterações aplicadas ao horário por `GET
/api/reservas/{reservaId}/auditoria` e `GET
/api/reservas/{reservaId}/historico-horario`. Eventos automáticos de expiração
usam `perfilUsuario: SISTEMA` e `usuarioId: null`.

## Exemplos

Datas devem ser enviadas em ISO 8601 como horário local de São Paulo, sem offset; o servidor compara o início com seu relógio `America/Sao_Paulo`. O início deve ser posterior ao momento atual nessa zona; ambos os horários devem usar minuto `00` ou `30`, com segundos e nanos zerados, e o término deve ser posterior ao início. Por exemplo, para uma execução antes de novembro de 2040: `2040-11-02T10:00:00`.

### Solicitar

```http
POST /api/reservas
Content-Type: application/json

{
  "salaId": 1,
  "solicitanteId": 12,
  "tipoSolicitante": "PROFESSOR",
  "professorIds": [],
  "materialIds": [1],
  "inicio": "2040-11-02T09:00:00",
  "fim": "2040-11-02T10:30:00"
}
```

Retorna `201 Created`, inclui a URI da reserva no cabecalho `Location` e representa a reserva no estado `SOLICITADA`.

### Alterar horario

```http
PUT /api/reservas/42/horario
Content-Type: application/json

{
  "solicitanteId": 12,
  "inicio": "2040-11-02T10:00:00",
  "fim": "2040-11-02T11:30:00"
}
```

### Cancelar, aprovar ou rejeitar

Cancelamento recebe o ID do solicitante:

```http
POST /api/reservas/42/cancelamento
Content-Type: application/json

{
  "usuarioId": 12
}
```

Aprovacao e rejeicao recebem o ID e o papel do aprovador. O ID deve existir no cadastro correspondente:

```json
{
  "usuarioId": 4,
  "tipoAprovador": "ADMINISTRADOR"
}
```

`tipoAprovador` aceita `RESPONSAVEL` ou `ADMINISTRADOR` no corpo, mas a rota de aprovação aceita somente `RESPONSAVEL`; uma tentativa de aprovação por Administrador retorna `403`. A rota de rejeição aceita os dois perfis. A resposta informa o decisor em `responsavelId` ou `administradorId`.

### Iniciar, concluir ou registrar não comparecimento

Essas operações não aceitam ID ou perfil no corpo; a sessão determina o ator:

```http
POST /api/reservas/42/iniciar
X-CSRF-Token: <token da sessão>
```

## Respostas de erro

Os erros retornam JSON no formato `{"mensagem":"..."}`.

| Status | Significado |
|---|---|
| `400 Bad Request` | Corpo inválido, campos obrigatórios ausentes ou intervalo que viola RN01; a mensagem identifica data/hora não futura, incremento de 30 minutos, segundos/nanos ou término não posterior |
| `404 Not Found` | Reserva, sala, solicitante, responsavel ou administrador inexistente |
| `401 Unauthorized` | Credenciais inválidas ou sessão ausente/expirada |
| `403 Forbidden` | Token CSRF inválido, perfil sem permissão, ou identidade da sessão não corresponde à operação |
| `409 Conflict` | Sobreposicao de horario ou estado que nao permite a operacao |

O intervalo exige início estritamente futuro em `America/Sao_Paulo`, minutos `00` ou `30` em ambos os horários, segundos/nanos iguais a zero e `fim` posterior a `inicio`. A mesma validação é aplicada ao solicitar e ao alterar horário. Reservas solicitadas, aprovadas ou em uso ocupam a sala; salas indisponiveis nao aceitam novas solicitacoes.

## Seguranca e limites atuais

O login usa sessão HTTP, cookies `HttpOnly` e `SameSite=Lax`, token CSRF em operações mutáveis, senhas PBKDF2 e autorização por perfil/posse da reserva. Configure `SESSION_COOKIE_SECURE=true` quando servir a aplicação por HTTPS. As contas seed são somente demonstração; cadastro/gestão de contas, recuperação de senha e autenticação institucional ainda não existem.

O modelo permite verificar conflitos de sala, docentes e materiais; contas do tipo Professor são reutilizadas como docentes e materiais têm entidade própria. A classificação demonstrativa de materiais não altera a regra de aprovação. A expiração prevista em RF15/RN12, alterações pendentes aprovadas, manutenção por período, CRUD administrativo e movimentação de materiais ainda dependem de regras e rotas ausentes. A auditoria atual cobre criação e mudanças de estado, não alterações de horário. O Hibernate atualiza o schema em desenvolvimento; ambientes existentes devem deixar essa atualização ser aplicada antes de usar as novas tabelas.

## Referencias

- [Requisitos funcionais](prd/requisitos-funcionais.md)
- [Fluxo de solicitacao de reserva](prd/fluxos-sequencia.md)
- [Especificacao do projeto](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)