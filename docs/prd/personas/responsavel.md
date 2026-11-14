# Persona: Responsavel

## Descricao

Perfil que aprova todas as reservas novas e alteracoes de data/horario, valida a alocacao de docentes e acompanha retiradas e devolucoes de materiais. O cancelamento pelo Solicitante nao exige aprovacao, mas e auditado.

## Objetivos

- Revisar todas as reservas novas e alteracoes de data/horario pendentes.
- Validar a alocacao de docentes.
- Acompanhar a retirada e a devolucao de materiais e equipamentos.
- Consultar informacoes suficientes para identificar conflitos e indisponibilidades.

## Jornada na Aplicacao

1. **Login** - acessa as funcoes atribuidas ao perfil.
2. **Solicitacoes** - consulta pedidos pendentes de decisao.
3. **Analise** - verifica horario, docentes e disponibilidade dos recursos.
4. **Decisao** - aprova ou rejeita reservas e alteracoes pendentes, apos revalidar conflitos e disponibilidade.
5. **Alocacao docente** - valida os docentes associados as reservas.
6. **Movimentacao de material** - registra retirada e devolucao.
7. **Acompanhamento** - consulta estados e historico das reservas.

## Regras de Negocio

- **RN05 - Aprovacao:** toda reserva nova exige aprovacao do Responsavel, independentemente da classificacao de restricao.
- **RN10 - Alteracao:** alteracoes de data/horario exigem aprovacao; a reserva original permanece ativa enquanto aguarda.
- **RN09 - Cancelamento:** o Solicitante pode cancelar ate duas horas antes do inicio, sem aprovacao; usuario e data/hora ficam auditados.
- **RN12 - Expiracao:** reservas novas sem decisao ate o inicio expiram, geram auditoria e liberam os recursos; alteracoes pendentes expiram no inicio original e preservam a reserva.
- **RN07 - Auditoria:** mudancas de estado devem gerar registro auditavel.
- **RN08 - Materiais:** retiradas e devolucoes devem ser registradas.

## Permissoes

| Fluxo | Acesso previsto |
|---|---|
| Aprovar reservas novas e alteracoes de data/horario | Permitido |
| Aprovar cancelamento solicitado pelo Solicitante | Nao exigido |
| Validar alocacao de docentes | Permitido |
| Registrar retirada e devolucao de materiais | Permitido |
| Gerenciar cadastros, usuarios ou permissoes | Nao atribuido a este perfil |

## Situacao atual da API

O Responsável autentica-se com a própria conta. Somente uma sessão de Responsável pode aprovar reservas; Responsável e Administrador podem rejeitar. O backend verifica o ID da sessão antes de registrar quem decidiu. A classificação de recursos restritos é demonstrativa e não altera essa regra. Validação de docentes, retirada/devolução de materiais e partes do fluxo de alterações de horário permanecem pendentes.

## Qualidade relevante - ISO/IEC 25010

- **Seguranca:** somente o perfil Responsavel pode aprovar reservas e alteracoes de data/horario.
- **Confiabilidade:** estados e registros de movimentacao devem permanecer consistentes.
- **Capacidade de interacao:** a fila de pedidos deve deixar claros os dados e o resultado de cada decisao.
- **Adequacao funcional:** permitir validar docentes e acompanhar materiais conforme as atribuicoes do perfil.

## Pontos operacionais

- Como tratar atraso, perda ou devolucao incompleta de materiais.