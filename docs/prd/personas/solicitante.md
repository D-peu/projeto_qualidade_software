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
5. **Solicitacao** - cria a reserva e acompanha seu estado, inclusive eventual aprovacao especial.
6. **Gestao** - altera ou cancela a propria reserva conforme as regras aplicaveis.

## Regras de Negocio

- **RN01 - Intervalo:** o termino deve ser posterior ao inicio.
- **RN02 - Conflitos:** o mesmo recurso nao pode ser reservado em horarios sobrepostos.
- **RN03 - Concorrencia:** solicitacoes simultaneas conflitantes nao podem resultar em dupla reserva.
- **RN04 - Indisponibilidade:** recursos bloqueados ou em manutencao nao podem ser reservados.
- **RN05 - Aprovacao:** recursos restritos exigem aprovacao do Responsavel.

## Permissoes

| Fluxo | Acesso previsto |
|---|---|
| Consultar disponibilidade | Permitido |
| Criar, alterar ou cancelar as proprias reservas | Permitido, sujeito a regras e estados |
| Consultar e alterar reservas de outros solicitantes | Nao permitido |
| Gerenciar cadastros, usuarios ou permissoes | Nao atribuido a este perfil |
| Aprovar solicitacoes especiais | Nao permitido |

## Situacao atual da API

O solicitante entra com sua conta de Professor ou Coordenador. O backend usa a sessão para obter seu ID e tipo, e só permite consultar, alterar ou cancelar as próprias reservas; os IDs enviados pelo cliente não substituem a identidade da sessão. As contas iniciais são demonstrativas. As reservas verificam conflito de horário para sala, docentes associados e materiais associados. Professores são contas existentes do tipo PROFESSOR; materiais estão disponíveis em catálogo e podem ser associados à reserva.

## Qualidade relevante - ISO/IEC 25010

- **Capacidade de interacao:** busca compreensivel, disponibilidade visivel e mensagens de erro acionaveis.
- **Adequacao funcional:** os filtros e validacoes devem refletir os recursos e restricoes necessarios a atividade.
- **Confiabilidade:** o resultado da reserva deve permanecer correto sob conflitos e solicitacoes simultaneas.
- **Eficiencia de desempenho:** tempo de resposta da busca deve ser medido; a meta ainda precisa ser definida.

## Pontos a validar

- Informacoes necessarias para comparar salas, materiais e disponibilidade docente.
- Regras de antecedencia, recorrencia, duracao e cancelamento.
- Quais alteracoes sao permitidas apos uma reserva ser aprovada ou iniciada.