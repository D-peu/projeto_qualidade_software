# Sistema de Alocacao de Recursos Universitarios - PRD

**Status:** Escopo funcional consolidado com inventario demonstrativo; falta validar a suficiencia dos dados ficticios com equipe/docente.
**Versao:** 0.3
**Contexto:** Projeto academico para uso no contexto universitario.

## Objetivo

Desenvolver e validar uma aplicacao para organizar a alocacao de salas, professores e materiais sem conflitos de horario. O produto deve permitir que professores e coordenadores consultem disponibilidade e gerenciem reservas, enquanto Responsaveis e Administradores executam as atribuicoes de perfil descritas neste PRD.

## Contexto

O projeto e academico e ainda nao existe uma aplicacao para esse processo. A especificacao da disciplina exige demonstrar nao apenas funcionamento, mas tambem qualidade, seguranca, rastreabilidade e desempenho. Os requisitos sao derivados da especificacao fornecida e das decisoes de escopo registradas neste documento.

## Perfis

| Perfil | Responsabilidade principal |
|---|---|
| Solicitante | Professor ou coordenador que consulta disponibilidade e cria, altera ou cancela suas reservas. |
| Responsavel | Aprova todas as reservas e alteracoes de data/horario, valida a alocacao de docentes e acompanha retiradas e devolucoes de materiais. |
| Administrador | Gerencia salas, professores, materiais, usuarios, bloqueios e periodos de manutencao. |

## Escopo funcional consolidado

- Autenticacao e autorizacao por perfil.
- Cadastro e consulta de salas, professores e materiais; pesquisa por tipo, capacidade, localizacao, competencia e disponibilidade.
- Criacao, alteracao, cancelamento e acompanhamento de reservas.
- Toda reserva nova inicia como `SOLICITADA` e exige aprovacao do Responsavel, inclusive quando todos os recursos sao nao restritos. Esta e uma regra do projeto mais restritiva que o minimo descrito na disciplina. A classificacao de recurso restrito continua registrada para identificar itens que exigem cuidado especial, mas nao altera a regra geral de aprovacao. Consulte o [inventario demonstrativo](prd/inventario-recursos.md).
- Uma reserva informa data, horario de inicio e horario de termino. A data deve ser futura, inicio e termino devem cair em incrementos de 30 minutos, e o termino deve ser posterior ao inicio. Nao ha outros limites de antecedencia, horario institucional, duracao maxima ou recorrencia no escopo atual.
- Reservas confirmadas nao podem se sobrepor para sala, material ou professor. Solicitacoes pendentes podem coexistir no mesmo intervalo; a disponibilidade deve ser revalidada na aprovacao, e no maximo uma solicitacao conflitante pode ser aprovada.
- Alteracoes de data/horario podem ser solicitadas ate duas horas antes do inicio original e exigem aprovacao do Responsavel para qualquer recurso. A reserva original permanece valida ate a aprovacao da alteracao. O Responsavel deve decidir ate o inicio original; caso contrario, o pedido expira e a reserva original permanece.
- O Solicitante pode cancelar sua reserva ate duas horas antes do inicio, inclusive quando ela ja estiver aprovada, sem aprovacao do Responsavel. Cancelamentos nao apagam o registro: mudam o estado e geram auditoria com usuario e data/hora; justificativa nao e obrigatoria.
- Recursos em manutencao nao recebem novas reservas. Reservas e solicitacoes existentes nao sao canceladas automaticamente; o Responsavel e alertado para avaliar a situacao.
- O ciclo de vida inclui `SOLICITADA`, `APROVADA`, `EM_USO` e `CONCLUIDA`, alem de `REJEITADA`, `CANCELADA`, `NAO_COMPARECEU` e `EXPIRADA`. Se uma reserva nova nao for decidida ate o inicio, expira automaticamente, libera os recursos e gera auditoria. Alteracao pendente expira no inicio original e mantem a reserva original. Mudancas de estado geram auditoria. Reservas iniciadas nao podem ser apagadas.
- Notificacoes serao simuladas dentro do sistema, sem integracao com servico externo.
- O inventario atual e demonstrativo e ficticio. Se recursos reais forem usados, a equipe deve avaliar riscos de uso e registrar a aplicabilidade de seguranca operacional (safety).

## Metas de qualidade e ambiente

- Desempenho: teste JMeter com 25 usuarios simultaneos por 10 minutos e percentil 95 (p95) de resposta de ate 2 segundos nos fluxos medidos de disponibilidade e reserva.
- Concorrencia: nenhum teste pode resultar em mais de uma reserva aprovada para o mesmo recurso e intervalo conflitante.
- Acessibilidade: atender aos requisitos da disciplina, com navegacao por teclado, foco visivel e contraste legivel. Nao se declara conformidade formal com nivel WCAG.
- Compatibilidade: Chrome e Firefox atuais, em desktop e celular.
- Modelo de qualidade: ISO/IEC 25010:2023, como referencia de organizacao e validacao, sem alegacao de certificacao ou conformidade.
- Metas academicas: 100% dos requisitos criticos na RTM, cobertura minima de 80% de linhas e 70% de branches, zero defeitos ou vulnerabilidades criticas, pipeline e relatorio de carga/qualidade.

As metas de desempenho acima sao criterios iniciais escolhidos para este projeto; a especificacao da disciplina exige teste de carga, mas nao fixa esses valores.

## Decisoes registradas

| Tema | Decisao |
|---|---|
| Aprovacao | Toda reserva nova exige aprovacao do Responsavel, independentemente da classificacao de restricao; regra do projeto mais restritiva que o minimo da disciplina. |
| Classificacao de restritos | Indicador demonstrativo por recurso, conforme [inventario](prd/inventario-recursos.md); nao altera a regra geral de aprovacao. |
| Conflito pendente | Solicitacoes pendentes podem coincidir; o conflito e revalidado ao aprovar e so uma solicitacao conflitante pode ser aprovada. |
| Alteracao | Alteracoes de data/horario devem ser solicitadas ate duas horas antes do inicio original e sempre exigem aprovacao. Horario original permanece valido; decisao deve ocorrer ate o inicio original, senao o pedido expira. |
| Cancelamento | Permitido ao Solicitante ate duas horas antes do inicio, inclusive para reserva aprovada, sem aprovacao. Registrar usuario e data/hora; justificativa opcional. |
| Pedido de reserva sem decisao | Expira no inicio se nao for aprovado; registrar auditoria e liberar os recursos. |
| Manutencao | Bloqueia novas reservas, preserva registros existentes e alerta o Responsavel; sem cancelamento automatico. |
| Periodo | Somente datas futuras; sem outros limites de antecedencia, horario de funcionamento, duracao maxima ou recorrencia. Inicio e termino em incrementos de 30 minutos. |
| Notificacoes | Simuladas dentro do sistema; sem servico externo. |
| Carga | 25 usuarios simultaneos, 10 minutos, p95 de ate 2 segundos. |
| Acessibilidade e compatibilidade | Teclado, foco visivel, contraste legivel; Chrome e Firefox atuais em desktop e celular. |
| Qualidade | ISO/IEC 25010:2023 como referencia; safety para recursos reais fica sujeita a inventario real. |

## Criterios academicos de sucesso

- Nenhuma reserva conflitante aceita, inclusive em solicitacoes simultaneas.
- Recursos em manutencao indisponiveis para novas reservas.
- 100% dos requisitos criticos ligados a risco, teste e evidencia na RTM.
- Cobertura minima de 80% de linhas e 70% de branches, conforme a especificacao da disciplina.
- Zero defeitos ou vulnerabilidades criticas na avaliacao final.
- Plano e resultado de teste de carga, estrategia de testes e relatorio de qualidade entregues.

Os percentuais e entregaveis acima sao criterios da disciplina, nao limites definidos pela ISO.

## Documentos do PRD

- [Requisitos funcionais e rastreabilidade](prd/requisitos-funcionais.md)
- [Requisitos nao funcionais ISO 25010](prd/requisitos-nao-funcionais.md)
- [RTM - Matriz de Rastreabilidade de Requisitos](prd/RTM.md)
- [Estrategia de implementacao e testes](prd/estrategia-implementacao-testes.md)
- [Fluxos e diagramas de sequencia](prd/fluxos-sequencia.md)
- [Inventario demonstrativo de recursos](prd/inventario-recursos.md)
- Personas: [Administrador](prd/personas/admin-sistema.md), [Responsavel](prd/personas/responsavel.md) e [Solicitante](prd/personas/solicitante.md)

## Pendencias

- Validar com equipe/docente se o inventario demonstrativo ficticio e suficiente para a entrega; se forem usados recursos reais, refazer inventario e avaliar safety.

## Referencias

- [Especificacao do projeto da disciplina](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)
- [Repositorio de referencia para organizacao do PRD](https://github.com/afonsolelis/cinesenac)
