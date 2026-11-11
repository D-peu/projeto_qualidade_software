# Estrategia de implementacao e testes

Este plano deriva da [RTM validada](RTM.md), do [PRD](../prd.md) e da especificacao da disciplina. Prioriza a fundacao executavel, os requisitos criticos e a prova de qualidade. Ainda nao e uma story de desenvolvimento; as stories formais devem ser criadas pelo fluxo de @sm e validadas antes de implementacao.

## Priorizacao e sequencia sugerida

| Ordem | Prioridade | Pacote candidato a story | Requisitos principais | Resultado para sair da etapa |
|---|---|---|---|---|
| 0 | P0 - bloqueador | Fundacao executavel e CI | RNF-ISO-07, RNF-ISO-08 | Aplicacao Spring Boot 3 / Java 21 inicia em ambiente limpo; build e testes minimos executam; GitHub Actions valida pull requests. Selecionar banco de dados antes de criar persistencia de produto. |
| 1 | P0 - critico | Autenticacao e autorizacao | RF01, RF02, RNF-ISO-06 | Autenticacao real e permissoes aplicadas no servidor; testes provam acesso permitido e negado por perfil. |
| 2 | P0 - prerequisito | Cadastros e agenda de recursos | RF03, RF04, RF05, RF12 | Persistir e pesquisar salas, docentes, materiais, usuarios e estados de disponibilidade; estabelecer relacoes necessarias para uma reserva. |
| 3 | P0 - regra critica | Reserva, aprovacao e estados | RF06, RF07, RF08, RF10, RF15 | Criar pedido `SOLICITADA`; validar datas/intervalos de 30 min; aprovacao/rejeicao somente por Responsavel; cancelamento conforme prazo; alteracao somente por pedido aprovado; expiracao em `EXPIRADA` nos prazos definidos; registrar transicoes. |
| 4 | P0 - regra critica | Concorrencia, manutencao e auditoria | RF09, RF11, RF14, RNF-ISO-05 | Revalidar disponibilidade de sala, docente e materiais atomicamente; no maximo uma aprovacao por recurso/intervalo conflitante; manutencao preserva reservas existentes e alerta Responsavel; toda mudanca relevante deixa auditoria. |
| 5 | P1 | Movimentacao e notificacoes internas | RF13, RF19 | Registrar retirada/devolucao ligada a reserva e apresentar notificacoes simuladas, sem servico externo. |
| 6 | P1 | Relatorios e documentacao publica | RF16, RF18 | Consultar utilizacao por recurso, carga horaria e conflitos evitados; documentar API ou fluxos publicos com exemplos. |
| 7 | P1 | Interface integrada e acessivel | RF17 | Integrar a interface ao backend e verificar desktop/celular, teclado, foco visivel, contraste e mensagens. |
| 8 | Gate de entrega | Evidencias finais de qualidade | RNF-ISO-01 a RNF-ISO-09 | Cobertura, CI, SonarCloud, JMeter, registros de defeitos, RTM atualizada, diagramas e relatorio final evidenciados. |

Os pacotes 3 e 4 podem ser decompostos em mais de uma story se o tamanho exceder uma entrega revisavel. Nao separar a garantia de concorrencia da operacao que aprova/atualiza reservas: ambas precisam preservar a mesma consistencia transacional.

## Estrategia de testes

| Nivel/tecnica | Escopo minimo | Evidencia esperada |
|---|---|---|
| Unitario - JUnit 5 | Regras de intervalo, incrementos de 30 minutos, transicoes de estado, prazos de alteracao/cancelamento/expiracao e calculos de relatorio. Usar testes parametrizados para limites e combinacoes de estados. | Relatorios JUnit no build; testes independentes e deterministas. |
| Integracao - Testcontainers | Persistencia real de recursos, reservas, auditoria, manutencao e movimentacao de materiais. Verificar rollback e consistencia das operacoes. | Testes executados com banco containerizado; banco de teste equivalente ao escolhido para a aplicacao. |
| API caixa-preta | Percorrer endpoints publicos por HTTP: autenticacao, autorizacao, cadastros, pesquisa, reserva, aprovacao, alteracao, cancelamento e erros. Validar status e formato documentado. | Suite automatizada contra aplicacao iniciada, sem invocar servicos internos diretamente. |
| Concorrencia | Disparar simultaneamente pedidos/aprovacoes/alteracoes sobre o mesmo recurso e intervalo; testar tambem recursos diferentes e intervalos adjacentes. | Resultado prova no maximo uma reserva confirmada por recurso com sobreposicao; verificar estado final no banco. |
| End-to-end | Fluxos completos por perfil: solicitante cria e acompanha; Responsavel aprova/rejeita ou decide alteracao; Solicitante cancela dentro do prazo; Administrador bloqueia recurso; Responsavel registra retirada/devolucao. | Evidencia de execucao nos navegadores suportados e resultados dos fluxos. |
| Acessibilidade e compatibilidade | Chrome e Firefox atuais em desktop e celular; teclado, ordem/foco visivel, contraste legivel, mensagens de erro e layouts responsivos. | Checklist manual mais testes E2E aplicaveis. Nao declarar nivel WCAG formal. |
| Carga - JMeter | 25 usuarios simultaneos por 10 minutos nos fluxos de disponibilidade e reserva; capturar p95, throughput e erros. Incluir tentativas concorrentes sobre recursos coincidentes. | Plano/script JMeter e relatorio com ambiente, configuracao, resultado e analise. Criterio definido: p95 <= 2 s. |
| CI e analise estatica | Em cada pull request: build, testes, cobertura, verificacoes estaticas e relatorio SonarCloud. | GitHub Actions verde; JaCoCo >= 80% de linhas e >= 70% de branches; SonarCloud associado ao repositorio. |

WireMock nao faz parte do escopo atual porque notificacoes sao internas/simuladas. Se uma integracao externa for aprovada posteriormente, incluir WireMock e atualizar RNF-ISO-03, testes e RTM.

## Cenarios criticos para iniciar a suite

1. Duas aprovacoes simultaneas para a mesma sala e intervalo: exatamente uma pode ser confirmada.
2. Conflitos independentes para docente e material: tambem devem bloquear a segunda confirmacao.
3. Intervalos adjacentes: permitir quando um termina exatamente quando o outro comeca.
4. Pedido novo ainda `SOLICITADA` no instante de inicio: mudar para `EXPIRADA`, auditar e liberar os recursos.
5. Alteracao pendente no inicio original: expirar o pedido de alteracao e manter reserva/horario originais.
6. Cancelar dentro do limite de duas horas: cancelar sem aprovacao, registrar usuario/data-hora e liberar recurso.
7. Cancelar fora do limite ou apos inicio: rejeitar a operacao do Solicitante sem apagar a reserva.
8. Colocar recurso em manutencao com pedidos/reservas existentes: bloquear novos pedidos, preservar registros e alertar Responsavel.
9. Tentativas com perfil sem permissao, datas passadas, inicio igual/fim anterior, incremento invalido e entrada malformada: rejeitar sem alterar dados.

## Decisoes tecnicas que bloqueiam detalhamento

- **Banco de dados:** nao definido nos artefatos atuais. @architect deve recomendar e validar a escolha antes das stories de persistencia e configuracao Testcontainers.
- **Execucao de expiracoes:** a regra funcional esta definida, mas a estrategia tecnica (por exemplo, verificacao ao consultar/decidir versus tarefa periodica) deve ser escolhida na arquitetura; qualquer abordagem deve funcionar apos reinicio e ter teste para o limite de tempo.
- **Fuso horario:** a regra de data futura e os prazos dependem de um fuso de referencia, ainda nao registrado. Definir antes de implementar comparacoes de prazo e teste de carga.
- **Aprovacao de pedidos novos:** nao ha prazo anterior ao inicio para decidir; se chegar ao inicio sem decisao, o pedido expira automaticamente e e auditado.

## Atualizacao da RTM

Ao fechar uma story, atualizar a linha correspondente em [RTM](RTM.md) com caminho da suite/relatorio, data da execucao e resultado observado. Codigo sem teste executado permanece pendente; prototipo em memoria permanece como evidencia parcial.

## Referencias

- [Especificacao do projeto da disciplina](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)
- [PRD principal](../prd.md)
- [RTM](RTM.md)
- [Requisitos funcionais](requisitos-funcionais.md)
- [Requisitos nao funcionais](requisitos-nao-funcionais.md)
