Feature: Conflitos e concorrência em reservas de salas
  As a solicitante
  I want to solicitar reservas sem conflito de horario
  So that uma sala nao seja reservada duas vezes no mesmo intervalo

  Background:
    Given uma sala disponivel e um solicitante do tipo PROFESSOR

  Scenario: No maximo uma solicitacao conflitante e aceita quando enviadas simultaneamente
    When duas solicitacoes simultaneas usam a mesma sala e o mesmo intervalo
    Then uma solicitacao e aceita
    And a outra solicitacao e rejeitada por conflito de horario
    And existe somente uma reserva persistida para aquele intervalo

  Scenario: Solicitacoes simultaneas em intervalos nao conflitantes sao aceitas
    When duas solicitacoes simultaneas usam a mesma sala em intervalos nao sobrepostos
    Then ambas as solicitacoes sao aceitas
    And existem duas reservas persistidas para aqueles intervalos

  Scenario: Apenas uma alteracao simultanea pode ocupar o mesmo horario
    Given existem duas reservas solicitadas da mesma sala em horarios diferentes
    When duas alteracoes simultaneas movem as reservas para o mesmo horario
    Then uma alteracao e aceita
    And a outra alteracao e rejeitada por conflito
    And somente uma reserva persiste no horario de destino

  Scenario Outline: Uma reserva em estado que ocupa a sala bloqueia sobreposicao
    Given existe uma reserva na mesma sala com status <status>
    When uma solicitacao para um intervalo sobreposto e processada
    Then a solicitacao e rejeitada por conflito de horario

    Examples:
      | status     |
      | SOLICITADA |
      | APROVADA   |
      | EM_USO     |

  Scenario Outline: Uma reserva em estado que nao ocupa a sala nao bloqueia sobreposicao
    Given existe uma reserva na mesma sala com status <status>
    When uma solicitacao para um intervalo sobreposto e processada
    Then a solicitacao e aceita

    Examples:
      | status          |
      | CONCLUIDA       |
      | REJEITADA       |
      | CANCELADA       |
      | NAO_COMPARECEU  |

  # Especificacao Gherkin vinculada a testes JUnit existentes; nao executada por runner BDD.
  # Testes: ReservaPostgresIntegrationTest.deveAceitarSomenteUmaReservaEmSolicitacoesConcorrentesComConflito
  #         ReservaPostgresIntegrationTest.deveAceitarSolicitacoesConcorrentesParaIntervalosNaoSobrepostos
  #         ReservaPostgresIntegrationTest.deveAceitarNoMaximoUmaAlteracaoConcorrenteParaMesmoHorario
  #         ReservaPostgresIntegrationTest.deveBloquearSomenteEstadosQueOcupamASala

  Scenario Outline: Somente Responsavel aprova qualquer reserva
    Given uma reserva solicitada contem material com classificacao restrito igual a <restrito>
    When o perfil <aprovador> tenta aprovar a reserva
    Then o resultado da aprovacao e <resultado>
    And o estado da reserva e <estado>

    Examples:
      | restrito | aprovador    | resultado          | estado      |
      | true     | RESPONSAVEL  | aprovada            | APROVADA    |
      | true     | ADMINISTRADOR| acesso negado       | SOLICITADA  |
      | false    | ADMINISTRADOR| acesso negado       | SOLICITADA  |

  # Teste JUnit relacionado: ReservaServiceTest.approvalRequiresResponsibleRegardlessOfMaterialRestriction
  # Rastreabilidade: RF02 / RF10 / RN05 / INV-04 / CP-54 / CP-55 / CP-156 / CP-157.
