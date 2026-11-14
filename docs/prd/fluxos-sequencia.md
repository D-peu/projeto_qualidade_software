# Sequências críticas — estado implementado e lacunas

**Revisão representada:** branch `release/rc1-blocker-fixes`, verificada em
2026-11-14. Estes diagramas descrevem o comportamento atual; notas destacam
onde ele difere do PRD. Os cenários Gherkin relacionados são especificações,
não execução BDD independente.

## 1. Solicitar e aprovar reserva (RF06–RF11, RF14–RF15)

```mermaid
sequenceDiagram
    actor S as Solicitante
    actor R as Responsável
    participant UI as Interface web
    participant API as Controllers / interceptor
    participant SV as ReservaService
    participant DB as PostgreSQL 16
    participant NC as NotificacaoClient
    participant EXT as Serviço HTTP externo

    S->>UI: Informa sala, intervalo e recursos
    UI->>API: POST /api/reservas + sessão + CSRF
    API->>SV: Solicitar reserva autenticada
    SV->>SV: Valida identidade, intervalo e dados
    SV->>DB: Bloqueia recursos em ordem; consulta conflitos
    DB-->>SV: Recurso disponível ou conflito
    alt Dados válidos e sem conflito
        SV->>DB: Grava SOLICITADA e evento de auditoria
        DB-->>SV: Reserva persistida
        SV-->>API: Reserva criada
        API-->>UI: 201 Created + Location
        UI-->>S: Exibe solicitação
    else Inválido, indisponível ou conflitante
        SV-->>API: Erro de validação, permissão ou conflito
        API-->>UI: 400, 403, 404 ou 409
        UI-->>S: Exibe mensagem de erro
    end

    R->>UI: Decide uma solicitação pendente
    UI->>API: POST /api/reservas/{id}/aprovacao + sessão + CSRF
    API->>SV: Aprovar como Responsável
    SV->>DB: Bloqueia reserva/sala e revalida disponibilidade confirmada
    DB-->>SV: Disponibilidade atual
    alt Responsável autorizado e sem conflito
        SV->>DB: Grava APROVADA e auditoria na transação
        SV->>NC: Envia evento de mudança de estado
        NC->>EXT: HTTP POST NotificacaoReserva
        alt Integração responde 2xx
            EXT-->>NC: Resposta recebida
        else HTTP 500 ou timeout
            EXT-->>NC: Falha; log; não desfaz estado salvo
        end
        SV-->>API: Reserva aprovada
        API-->>UI: 200 OK
    else Perfil inválido ou recurso indisponível
        SV-->>API: 403 ou 409; sem aprovação
        API-->>UI: Erro documentado
    end
```

O fluxo normal rejeita Administrador na aprovação, independentemente do recurso
(403). A revalidação e o estado persistido são cobertos por testes de serviço,
PostgreSQL e HTTP autenticado. O envio HTTP ocorre dentro da operação do serviço;
falhas são capturadas e não revertem a mudança. Esse cliente externo diverge de
RF19/RN11 e não equivale a notificação interna.

## 2. Solicitações concorrentes para a mesma sala/intervalo (RF08–RF09, RN02–RN03)

```mermaid
sequenceDiagram
    actor S1 as Solicitante A
    actor S2 as Solicitante B
    participant API as ReservaController
    participant SV as ReservaService
    participant DB as PostgreSQL 16

    par Solicitação A
        S1->>API: POST /api/reservas (sala R, intervalo T)
        API->>SV: Validar e solicitar
        SV->>DB: Lock pessimista da sala R
        DB-->>SV: Lock concedido
        SV->>DB: Verificar conflitos incluindo SOLICITADA
        SV->>DB: Gravar única reserva vencedora
        DB-->>SV: Reserva persistida
        SV-->>API: Reserva criada
        API-->>S1: 201 Created
    and Solicitação B simultânea
        S2->>API: POST /api/reservas (sala R, intervalo T)
        API->>SV: Validar e solicitar
        SV->>DB: Aguardar lock da sala R
        DB-->>SV: Lock liberado; conflito com reserva persistida
        SV-->>API: Conflito
        API-->>S2: 409 Conflict
    end
```

`ReservaPostgresIntegrationTest.deveAceitarSomenteUmaReservaEmSolicitacoesConcorrentesComConflito`
usa `ExecutorService`, `CountDownLatch` e Testcontainers; o resultado esperado
é no máximo uma criação aceita. A implementação atual também conta estados
`SOLICITADA` no conflito da criação. Isso impede criar pedidos pendentes
coincidentes, embora RN03 do PRD permita pendências coincidentes e peça
revalidação somente ao aprovar. Essa diferença permanece parcial na RTM.

## 3. Aprovações concorrentes de pedidos já existentes (RF09, RN03)

```mermaid
sequenceDiagram
    actor R1 as Responsável A
    actor R2 as Responsável B
    participant API as ReservaController
    participant SV as ReservaService
    participant DB as PostgreSQL 16

    Note over DB: Duas SOLICITADA conflitantes preparadas para o teste de integração
    par Aprovação A
        R1->>API: POST /{id-A}/aprovacao
        API->>SV: Aprovar como Responsável
        SV->>DB: Bloquear sala e revalidar estados confirmados
        SV->>DB: Gravar APROVADA + auditoria
        DB-->>SV: Transação efetivada
        SV-->>API: Reserva aprovada
        API-->>R1: 200 OK
    and Aprovação B concorrente
        R2->>API: POST /{id-B}/aprovacao
        API->>SV: Aprovar como Responsável
        SV->>DB: Esperar lock e consultar estados confirmados
        DB-->>SV: Conflito com A aprovada
        SV-->>API: Conflito; B continua SOLICITADA
        API-->>R2: 409 Conflict
    end
```

Teste: `ReservaPostgresIntegrationTest.deveAprovarNoMaximoUmaReservaConflitanteEmOperacoesConcorrentes`.
Os pedidos sobrepostos são fixtures já persistidas; a API de criação atual
recusa o segundo pedido pendente sobreposto, conforme a observação da seção 2.

## 4. Alterar horário e cancelar — comportamento presente (RF06, RF10, RF14)

```mermaid
sequenceDiagram
    actor S as Solicitante
    participant API as ReservaController
    participant SV as ReservaService
    participant DB as PostgreSQL 16
    participant NC as NotificacaoClient
    participant EXT as Serviço HTTP externo

    S->>API: PUT /api/reservas/{id}/horario + sessão/CSRF
    API->>SV: Alterar horário de reserva própria
    SV->>DB: Bloquear sala; validar SOLICITADA, intervalo e conflitos
    alt Válido
        SV->>DB: Atualizar horário diretamente
        DB-->>S: 200 OK (sem etapa de aprovação pendente)
    else Inválido ou conflita
        SV-->>S: 403 ou 409
    end

    S->>API: POST /api/reservas/{id}/cancelamento + sessão/CSRF
    API->>SV: Cancelar reserva própria
    SV->>DB: Validar estado e gravar CANCELADA + auditoria
    SV->>NC: Enviar NotificacaoReserva
    NC->>EXT: HTTP POST externo
    SV-->>S: 200 OK
```

**Lacunas:** alterar horário aplica a mudança diretamente a uma reserva
`SOLICITADA`, sem pedido separado, aprovação do Responsável ou trilha própria
de alteração. O prazo de duas horas definido por RN09/RN10 também não está
implementado. Não desenhar esses passos como comportamento disponível.

## 5. Manutenção atual

O Administrador pode alterar o estado operacional da sala por
`PUT /api/salas/{salaId}/status`. Solicitação ou aprovação de sala não
disponível resulta em 409. Registros existentes são preservados. Não existe
agenda de manutenção por período nem alerta automático ao Responsável; esses
passos do fluxo originalmente planejado estão pendentes.

## 6. Fluxos ainda sem sequência de execução

Não há fluxo implementado de retirada/devolução de materiais (RF13), relatórios
de utilização (RF16), notificação interna (RF19/RN11), gestão completa de
recursos/usuários (RF03/RF04) ou aprovação de alteração de horário (RN10/RN12).
O status e os riscos estão na [RTM única](../RTM.md).
