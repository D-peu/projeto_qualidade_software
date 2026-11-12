# API de Reservas

**Status:** Implementacao inicial
**Versao do contrato:** 1.1.0
**Contrato OpenAPI:** [openapi.yaml](openapi.yaml)

## Escopo

A API autentica as contas cadastradas com senha protegida por hash e sessao HTTP, sem JWT. Ela expoe a consulta de salas e os fluxos de reserva implementados: solicitacao, consulta, listagem filtrada por estado ou solicitante, alteracao de horario, cancelamento, aprovacao e rejeicao. O frontend servido pelo Spring Boot usa essas rotas; os dados apresentados sao lidos e gravados no backend, nao em uma lista local. O contrato OpenAPI pode ser aberto no Swagger Editor ou carregado por uma instalacao Swagger UI.

O repositorio possui configuracao Maven e aplicacao Spring Boot. A interface Swagger UI, entretanto, nao esta publicada pela aplicacao porque o Springdoc ainda nao foi configurado; este contrato pode ser aberto no Swagger Editor ou carregado por uma instalacao Swagger UI.

## Validacao das regras de negocio

Esta API implementa apenas parte das regras previstas no PRD. O comportamento observado no backend e:

| Regra | Situacao atual |
|---|---|
| RN01 - Intervalo | Implementada: o início deve estar no futuro segundo o relógio de `America/Sao_Paulo`; início e término devem estar nos minutos 00 ou 30, com segundos e nanos zerados; o término deve ser posterior ao início. |
| RN02 - Conflito de horario | Implementada para sala, docentes associados e materiais associados. Intervalos adjacentes sao aceitos. |
| RN03 - Concorrencia | Implementada para sala, docente e material: o serviço bloqueia os registros dos recursos em ordem determinística antes de verificar conflitos; testes PostgreSQL cobrem solicitações concorrentes em salas distintas que disputam o mesmo docente/material. |
| RN04 - Bloqueio/manutencao | Parcial: a sala so e aceita quando `DISPONIVEL`; nao existem bloqueios por periodo nem gestao administrativa desses estados. |
| RN05 - Aprovacao restrita | Parcial: a decisao aceita somente `RESPONSAVEL` ou `ADMINISTRADOR` e exige ID existente no cadastro correspondente. Ainda nao ha classificacao de recurso restrito para exigir aprovacao somente nesses casos. |
| RN06 - Ciclo de estados | Parcial: a API permite `SOLICITADA` para `APROVADA` ou `REJEITADA`, e cancelamento de solicitadas/aprovadas. Nao implementa transicoes para `EM_USO`, `CONCLUIDA` ou `NAO_COMPARECEU`. |
| RN07 - Protecao e auditoria | Parcial: a API nao oferece rota de exclusao, mas permite cancelar reservas `APROVADA` sem verificar se o horario de inicio ja passou. Tambem nao registra historico auditavel. O repository continua expondo operacoes de persistencia, portanto a regra nao e uma garantia do modelo inteiro. |
| RN08 - Materiais | Parcial: materiais podem ser associados a reservas e têm consulta de catálogo. Retirada e devolução ainda não são registradas. |

Na criacao, o payload precisa incluir `tipoSolicitante` (`PROFESSOR` ou `COORDENADOR`), e o backend exige correspondencia com a conta autenticada. Aprovacao/rejeicao exige perfil autenticado de `RESPONSAVEL` ou `ADMINISTRADOR`; o ID e o tipo enviados no payload precisam corresponder a essa conta. Solicitantes so consultam e alteram suas proprias reservas; filas de aprovacao sao restritas a Responsavel e Administrador.

Reservas podem incluir `professorIds` e `materialIds` opcionais. Cada professor precisa ser uma conta existente do tipo `PROFESSOR`; quando quem solicita é Professor, sua própria conta é associada automaticamente, mesmo se a lista estiver vazia. Os materiais precisam existir no catálogo. O sistema rejeita sobreposição de intervalo em qualquer sala, docente ou material associado. As listas de catálogo não retornam e-mail nem senha.

## Rotas

| Metodo | Rota | Finalidade | Sucesso |
|---|---|---|---|
| `GET` | `/api/auth/csrf` | Obter token CSRF antes de autenticar | `200 OK` |
| `POST` | `/api/auth/login` | Autenticar e iniciar sessão HTTP | `200 OK` |
| `GET` | `/api/auth/me` | Consultar conta da sessão atual | `200 OK` |
| `POST` | `/api/auth/logout` | Encerrar a sessão atual | `204 No Content` |
| `GET` | `/api/salas` | Listar salas cadastradas | `200 OK` |
| `GET` | `/api/professores` | Listar docentes cadastrados para alocação (somente ID e nome) | `200 OK` |
| `GET` | `/api/materiais` | Listar materiais cadastrados para alocação | `200 OK` |
| `GET` | `/api/reservas?status=SOLICITADA` | Listar reservas pelo estado | `200 OK` |
| `POST` | `/api/reservas` | Solicitar reserva | `201 Created` |
| `GET` | `/api/reservas/{reservaId}` | Consultar reserva | `200 OK` |
| `GET` | `/api/reservas/solicitante/{solicitanteId}` | Listar reservas do solicitante | `200 OK` |
| `GET` | `/api/reservas/pendentes` | Listar reservas `SOLICITADA`, ordenadas por data da solicitação crescente | `200 OK` |
| `PUT` | `/api/reservas/{reservaId}/horario` | Alterar horario enquanto solicitada | `200 OK` |
| `POST` | `/api/reservas/{reservaId}/cancelamento` | Cancelar reserva solicitada ou aprovada | `200 OK` |
| `POST` | `/api/reservas/{reservaId}/aprovacao` | Aprovar reserva solicitada | `200 OK` |
| `POST` | `/api/reservas/{reservaId}/rejeicao` | Rejeitar reserva solicitada | `200 OK` |

Todas as rotas da API, exceto `GET /api/auth/csrf` e `POST /api/auth/login`, exigem sessão autenticada. Requisições de alteração também precisam enviar o token da sessão no cabeçalho `X-CSRF-Token`. O cliente do frontend faz essa troca automaticamente. O navegador precisa ser servido pela mesma aplicação/origem da API.

### Contas de demonstração

Com `APP_SEED_ENABLED=true` (padrão local), a aplicação cria as contas e senhas demonstrativas listadas no README. As senhas são armazenadas com PBKDF2; registros antigos com a senha fictícia são atualizados pelo inicializador. Não use essas credenciais em produção.

Exemplo de consulta da fila:

```http
GET /api/reservas?status=SOLICITADA
Accept: application/json
```

`status` e obrigatorio em `GET /api/reservas` e aceita somente os valores do enum `StatusReserva`. A resposta contem os campos da reserva e o ID do solicitante, sem dados pessoais do cadastro.

## Integracao do frontend

O frontend autentica no servidor, restaura a sessão ao recarregar a página e mostra a área correspondente ao perfil retornado: Solicitante, Responsável ou Administrador. O ID e o tipo de solicitante/aprovador são derivados da conta autenticada; não existe mais configuração manual de IDs. A sessão expira após 30 minutos de inatividade. A tela de Administrador continua sem gestão de salas porque a API não oferece rotas de cadastro.

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

`tipoAprovador` aceita `RESPONSAVEL` ou `ADMINISTRADOR`. A resposta informa o decisor em `responsavelId` ou `administradorId`.

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

O modelo permite verificar conflitos de sala, docentes e materiais; contas do tipo Professor são reutilizadas como docentes e materiais têm entidade própria. Histórico auditável, classificação de recursos restritos, manutenção por período, CRUD administrativo e movimentação de materiais ainda dependem de regras e rotas ausentes. O Hibernate atualiza o schema em desenvolvimento; ambientes existentes devem deixar essa atualização ser aplicada antes de usar os novos campos/tabelas.

## Referencias

- [Requisitos funcionais](prd/requisitos-funcionais.md)
- [Fluxo de solicitacao de reserva](prd/fluxos-sequencia.md)
- [Especificacao do projeto](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)