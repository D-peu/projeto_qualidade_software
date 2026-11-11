# Fluxos - Diagramas de Sequencia

Diagramas dos fluxos criticos, atualizados conforme as decisoes do [PRD principal](../prd.md). Os participantes representam papeis e responsabilidades, sem definir arquitetura tecnica.

## 1. Solicitar reserva e validar disponibilidade (RF06-RF11, RN01-RN05)

```mermaid
sequenceDiagram
    actor S as Solicitante
    participant APP as Aplicacao
    actor R as Responsavel

    S->>APP: Informa recurso, data futura, inicio e termino
    APP->>APP: Valida intervalo, incremento de 30 min, manutencao e conflitos confirmados
    alt Intervalo invalido, conflito confirmado ou recurso indisponivel
        APP-->>S: Recusa e informa o motivo
    else Dados validos
        APP->>APP: Registra solicitacao como SOLICITADA
        APP-->>R: Disponibiliza solicitacao para analise
        R->>APP: Aprova ou rejeita
        APP->>APP: Revalida conflitos e manutencao antes de aprovar
        alt Ainda disponivel
            APP->>APP: Atualiza estado e registra auditoria
            APP-->>S: Informa a decisao
        else Conflito ou manutencao
            APP-->>R: Impede aprovacao e informa indisponibilidade
            APP-->>S: Informa que a solicitacao nao foi aprovada
        end
        opt Nenhuma decisao ate o inicio
            APP->>APP: Altera estado para EXPIRADA, registra auditoria e libera recursos
            APP-->>S: Informa expiracao do pedido
        end
    end
```

Todas as reservas novas exigem aprovacao, independentemente da classificacao de restricao. Solicitacoes pendentes podem coincidir; a revalidacao no momento da aprovacao impede que mais de uma reserva conflitante seja confirmada. Se o pedido nao for decidido ate o inicio, ele expira como `EXPIRADA`, gera auditoria e libera os recursos. Notificacoes sao simuladas dentro do sistema.

## 2. Solicitacoes simultaneas para o mesmo recurso (RF08-RF09, RN02-RN03)

```mermaid
sequenceDiagram
    actor S1 as Solicitante A
    actor S2 as Solicitante B
    participant APP as Aplicacao
    participant AGENDA as Agenda de recursos
    actor R as Responsavel

    S1->>APP: Solicita recurso R no intervalo T
    S2->>APP: Solicita recurso R no intervalo T
    APP->>AGENDA: Verifica reservas confirmadas
    AGENDA-->>APP: Nenhuma reserva confirmada no intervalo
    APP-->>S1: Registra pedido pendente
    APP-->>S2: Registra pedido pendente
    R->>APP: Tenta aprovar uma solicitacao
    APP->>AGENDA: Revalida disponibilidade de R no intervalo T
    AGENDA-->>APP: Reserva aprovada ocupa R no intervalo T
    R->>APP: Tenta aprovar a outra solicitacao
    APP->>AGENDA: Revalida novamente a disponibilidade
    AGENDA-->>APP: Conflito com reserva aprovada
    APP-->>R: Impede a segunda aprovacao
```

Toda solicitacao exige aprovacao do Responsavel. A verificacao/confirmacao deve ser atomica sob concorrencia: apenas uma solicitacao conflitante pode ser aprovada. Intervalos adjacentes, sem sobreposicao, podem ser confirmados.

## 3. Alterar ou cancelar uma reserva (RF06, RF10, RF14-RF15, RN07, RN09-RN10)

```mermaid
sequenceDiagram
    actor S as Solicitante
    participant APP as Aplicacao
    actor R as Responsavel

    alt Alteracao de data/horario
        S->>APP: Solicita alteracao ate duas horas antes do inicio original
        APP->>APP: Valida prazo, intervalo e conflitos
        APP->>APP: Registra alteracao pendente; mantem horario original
        APP-->>R: Disponibiliza alteracao para decisao
        R->>APP: Aprova ou rejeita ate o inicio original
        APP->>APP: Revalida conflito antes de aplicar
        alt Aprovada dentro do prazo e ainda disponivel
            APP->>APP: Substitui horario e registra auditoria
        else Rejeitada, conflitada ou prazo expirado
            APP->>APP: Mantem horario original e registra decisao/expiracao
        end
    else Cancelamento
        S->>APP: Solicita cancelamento ate duas horas antes do inicio
        APP->>APP: Valida limite e altera estado para CANCELADA
        APP->>APP: Registra usuario e data/hora da mudanca
        APP-->>S: Confirma cancelamento
    end
```

Cancelamento pelo Solicitante nao exige aprovacao e pode ser feito mesmo para reserva aprovada, desde que respeitado o limite de duas horas. Apos o inicio, a reserva nao pode ser apagada. Justificativa de cancelamento nao e obrigatoria.

## 4. Manutencao de recurso (RF04, RF11, RN04)

```mermaid
sequenceDiagram
    actor A as Administrador
    participant APP as Aplicacao
    actor R as Responsavel

    A->>APP: Marca recurso como em manutencao
    APP->>APP: Bloqueia novas reservas e preserva registros existentes
    APP-->>R: Emite alerta simulado sobre reservas/solicitacoes existentes
    R->>APP: Avalia cada caso existente
```

## 5. Retirada e devolucao de material (RF13, RN08)

```mermaid
sequenceDiagram
    actor R as Responsavel
    participant APP as Aplicacao

    R->>APP: Consulta material associado a reserva
    R->>APP: Registra retirada
    APP->>APP: Registra evento e horario da retirada
    R->>APP: Registra devolucao
    APP->>APP: Registra evento e horario da devolucao
    APP-->>R: Exibe movimentacao atualizada
```

## Cobertura dos fluxos

| Fluxo | Requisitos | Regras de negocio |
|---|---|---|
| Solicitacao, aprovacao e disponibilidade | RF06-RF11 | RN01-RN05 |
| Concorrencia de reserva | RF08-RF09 | RN02-RN03 |
| Alteracao e cancelamento | RF06, RF10, RF14-RF15 | RN07, RN09-RN10 |
| Manutencao | RF04, RF11 | RN04 |
| Movimentacao de material | RF13-RF14 | RN07-RN08 |
