# Persona: Solicitante

## Descricao

Professor ou coordenador universitario que consulta disponibilidade e cria, altera ou cancela as proprias reservas de salas, professores e materiais, conforme as permissoes definidas para sua conta.

## Objetivos

- Encontrar recursos adequados a atividade e ao horario desejado.
- Consultar disponibilidade antes de solicitar uma reserva.
- Criar, alterar e cancelar as proprias reservas.
- Saber se uma solicitacao foi aprovada, rejeitada ou concluida.

## Jornada na Aplicacao

1. **Login** - acessa as funcoes de solicitacao.
2. **Busca** - filtra salas e materiais por tipo, capacidade, localizacao e disponibilidade; consulta competencia de docentes.
3. **Selecao** - informa intervalo de horario e recursos necessarios.
4. **Validacao** - recebe retorno claro se houver conflito ou indisponibilidade.
5. **Solicitacao** - cria a reserva e acompanha o pedido `SOLICITADA` ate a decisao do Responsavel.
6. **Gestao** - solicita alteracao de data/horario, que depende de aprovacao, ou cancela a propria reserva ate duas horas antes do inicio, sem aprovacao e com auditoria.

## Regras de Negocio

- **RN01 - Intervalo:** o termino deve ser posterior ao inicio.
- **RN02 - Conflitos:** o mesmo recurso nao pode ser reservado em horarios sobrepostos.
- **RN03 - Concorrencia:** solicitacoes simultaneas conflitantes nao podem resultar em dupla reserva.
- **RN04 - Indisponibilidade:** recursos bloqueados ou em manutencao nao podem ser reservados.
- **RN05 - Aprovacao:** toda reserva nova exige aprovacao do Responsavel, inclusive para recursos nao restritos.
- **RN09 - Cancelamento:** pode cancelar a propria reserva ate duas horas antes do inicio; nao precisa de aprovacao, mas a mudanca de estado e auditada.
- **RN10 - Alteracao:** alteracao de data/horario deve ser solicitada ate duas horas antes do inicio original e exige aprovacao. O horario original permanece ativo enquanto aguarda; o pedido expira no inicio original se nao houver decisao.
- **RN12 - Expiracao:** se uma reserva nova nao for aprovada ate o inicio, ela expira, gera auditoria e libera os recursos; alteracao pendente expira e mantem a reserva original.

## Permissoes

| Fluxo | Acesso previsto |
|---|---|
| Consultar disponibilidade | Permitido |
| Criar reservas e solicitar alteracao de data/horario | Permitido, sujeito a aprovacao do Responsavel |
| Cancelar as proprias reservas | Permitido ate duas horas antes do inicio, sem aprovacao e com auditoria |
| Consultar e alterar reservas de outros solicitantes | Nao permitido |
| Gerenciar cadastros, usuarios ou permissoes | Nao atribuido a este perfil |
| Aprovar reservas ou alteracoes de data/horario | Nao permitido |

## Qualidade relevante - ISO/IEC 25010

- **Capacidade de interacao:** busca compreensivel, disponibilidade visivel e mensagens de erro acionaveis.
- **Adequacao funcional:** os filtros e validacoes devem refletir os recursos e restricoes necessarios a atividade.
- **Confiabilidade:** o resultado da reserva deve permanecer correto sob conflitos e solicitacoes simultaneas.
- **Eficiencia de desempenho:** disponibilidade e reserva devem atender a meta inicial de p95 de ate 2 segundos sob 25 usuarios simultaneos por 10 minutos, conforme o PRD.

## Dados e restricoes do fluxo

- Busca por tipo, capacidade, localizacao e disponibilidade; para docentes, considerar competencia.
- Somente datas futuras, sem limite adicional de antecedencia, duracao maxima ou recorrencia.
- Reservas iniciadas nao podem ser apagadas; alteracoes devem ser pedidas antes do inicio original.