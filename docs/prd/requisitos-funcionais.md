# Requisitos Funcionais - Sistema de Alocacao de Recursos

Requisitos derivados da especificacao academica e das decisoes de escopo registradas no [PRD principal](../prd.md). `RFxx` identifica um requisito funcional; `RNxx` identifica uma regra de negocio.

## 1. Autenticacao e controle de acesso

| ID | Requisito | Persona |
|---|---|---|
| RF01 | A aplicacao deve autenticar usuarios e disponibilizar funcionalidades conforme o perfil Solicitante, Responsavel ou Administrador. | Todas |
| RF02 | A aplicacao deve impedir que um usuario execute operacoes nao permitidas pelo seu perfil. | Todas |

## 2. Cadastros e disponibilidade

| ID | Requisito | Persona |
|---|---|---|
| RF03 | A aplicacao deve permitir ao Administrador cadastrar, consultar, editar e desativar salas, professores e materiais, incluindo a classificacao demonstrativa de recursos que exigem cuidado especial. | Administrador |
| RF04 | A aplicacao deve permitir ao Administrador gerenciar usuarios, bloqueios de recursos e periodos de manutencao. | Administrador |
| RF05 | A aplicacao deve permitir pesquisar recursos por tipo, capacidade, localizacao, competencia e disponibilidade. | Solicitante, Responsavel, Administrador |

## 3. Reservas e alocacao

| ID | Requisito | Persona |
|---|---|---|
| RF06 | A aplicacao deve permitir ao Solicitante criar, alterar e cancelar suas reservas conforme as regras de antecedencia, disponibilidade e aprovacao definidas neste documento. | Solicitante |
| RF07 | A aplicacao deve validar data futura, inicio e termino em incrementos de 30 minutos, e termino posterior ao inicio. | Solicitante, Responsavel, Administrador |
| RF08 | A aplicacao deve detectar sobreposicao de horario para cada sala, material ou professor envolvido na reserva. | Todas |
| RF09 | A aplicacao deve proteger a confirmacao e a alteracao de data/horario contra operacoes simultaneas conflitantes. Solicitacoes pendentes podem coincidir, mas a disponibilidade deve ser revalidada na aprovacao, permitindo no maximo uma reserva confirmada por recurso em intervalos sobrepostos. | Todas |
| RF10 | A aplicacao deve exigir aprovacao do Responsavel para toda reserva nova e alteracao de data/horario, independentemente da classificacao de restricao do recurso. | Solicitante, Responsavel |
| RF11 | A aplicacao deve impedir novas reservas de recursos bloqueados ou em manutencao, preservar os registros existentes e alertar o Responsavel. | Solicitante, Responsavel, Administrador |
| RF12 | A aplicacao deve permitir ao Responsavel validar a alocacao de docentes. | Responsavel |

## 4. Acompanhamento, auditoria e relatorios

| ID | Requisito | Persona |
|---|---|---|
| RF13 | A aplicacao deve registrar retirada e devolucao de materiais e equipamentos vinculados a uma reserva. | Responsavel |
| RF14 | A aplicacao deve manter historico auditavel das alteracoes e mudancas de estado, incluindo cancelamentos, com usuario e data/hora. Justificativa de cancelamento nao e obrigatoria. | Responsavel, Administrador |
| RF15 | A aplicacao deve suportar os estados solicitada, aprovada, em uso, concluida, rejeitada, cancelada, nao compareceu e expirada. Pedidos novos nao decididos ate o inicio expiram automaticamente; alteracoes pendentes expiram no inicio original e mantem a reserva original. | Todas |
| RF16 | A aplicacao deve permitir consultar relatorios de utilizacao por recurso, carga horaria alocada e conflitos evitados. | Responsavel, Administrador |
| RF17 | A aplicacao deve oferecer interface responsiva, mensagens de erro compreensiveis, navegacao por teclado, foco visivel e contraste legivel. | Todas |
| RF18 | A aplicacao deve documentar a API ou os fluxos publicos do sistema. | Todas |
| RF19 | A aplicacao deve emitir notificacoes simuladas dentro do sistema, sem integracao com servico externo. | Todas |

## Regras de negocio

| ID | Regra | Requisitos relacionados |
|---|---|---|
| RN01 | A reserva deve ser para uma data futura, com inicio e termino em incrementos de 30 minutos; o termino deve ser posterior ao inicio. Nao ha outros limites de antecedencia, horario institucional, duracao maxima ou recorrencia no escopo atual. | RF07 |
| RN02 | Intervalos confirmados nao podem se sobrepor para a mesma sala, material ou professor. Intervalos adjacentes, em que o termino de um coincide com o inicio do seguinte, nao se sobrepoem. | RF08 |
| RN03 | Solicitacoes pendentes podem se sobrepor. No momento de aprovar, a disponibilidade deve ser revalidada; no maximo uma solicitacao por recurso em intervalos sobrepostos pode ser aprovada. | RF09 |
| RN04 | Recursos bloqueados ou em manutencao nao podem receber novas reservas. A manutencao nao cancela automaticamente reservas ou solicitacoes existentes e deve alertar o Responsavel. | RF04, RF11 |
| RN05 | Toda reserva nova exige aprovacao do Responsavel. A classificacao de restricao e um atributo demonstrativo para identificar recursos que exigem cuidado especial, mas nao dispensa aprovacao para os demais. | RF03, RF10 |
| RN06 | Os estados previstos sao solicitada, aprovada, em uso e concluida; estados alternativos incluem rejeitada, cancelada e nao compareceu. | RF14, RF15 |
| RN07 | Reservas iniciadas nao podem ser apagadas. Toda mudanca de estado deve gerar auditoria; o registro minimo inclui usuario e data/hora. Justificativa de cancelamento nao e obrigatoria. | RF14, RF15 |
| RN08 | Retiradas e devolucoes de materiais devem ser registradas. | RF13 |
| RN09 | O Solicitante pode cancelar sua propria reserva ate duas horas antes do inicio, inclusive quando aprovada, sem aprovacao do Responsavel. Apos esse limite, o cancelamento pelo Solicitante nao e permitido. O cancelamento muda o estado e gera auditoria com usuario e data/hora. | RF06, RF14, RF15 |
| RN10 | O Solicitante pode solicitar alteracao de data/horario ate duas horas antes do inicio original. Toda alteracao exige aprovacao do Responsavel e revalida conflitos de forma atomica; o horario original permanece valido ate a aprovacao. O Responsavel deve decidir ate o inicio original; caso contrario, o pedido expira e a reserva original permanece. | RF06, RF08, RF09, RF10 |
| RN11 | Notificacoes sao simuladas e exibidas dentro da aplicacao, sem dependencia de API externa. | RF19 |
| RN12 | Reserva nova em `SOLICITADA` sem decisao ate o inicio expira automaticamente, libera seus recursos e gera auditoria. Alteracao pendente expira no inicio original e preserva a reserva e o horario originais. | RF10, RF14, RF15 |

## Matriz de rastreabilidade

Esta tabela resume apenas a relacao regra-requisito. A rastreabilidade completa para risco, cenario de teste e evidencia esta na [RTM](RTM.md).

| Regra de negocio | Requisitos funcionais |
|---|---|
| RN01 - Data e intervalo validos | RF07 |
| RN02 - Ausencia de sobreposicao confirmada | RF08 |
| RN03 - Concorrencia e revalidacao de aprovacao | RF09 |
| RN04 - Bloqueio e manutencao | RF04, RF11 |
| RN05 - Aprovacao de todas as reservas | RF03, RF10 |
| RN06 - Estados da reserva | RF14, RF15 |
| RN07 - Nao exclusao e auditoria | RF14, RF15 |
| RN08 - Movimentacao de materiais | RF13 |
| RN09 - Limite de cancelamento | RF06, RF14, RF15 |
| RN10 - Alteracao e aprovacao de horario | RF06, RF08, RF10 |
| RN11 - Notificacoes simuladas | RF19 |
| RN12 - Expiracao de pedidos pendentes | RF10, RF14, RF15 |

## Situacao da implementacao atual

Esta matriz registra o que o backend existente realmente garante. Ela nao substitui os requisitos-alvo nem significa que os itens parciais ou pendentes foram dispensados.

| Requisitos | Situacao no backend atual | Limite conhecido |
|---|---|---|
| RF01-RF02 | Parcial | Login por e-mail/senha com contas persistidas, hash PBKDF2 e sessão HTTP; rotas de reserva aplicam perfil e posse. Cadastro/gestão de contas, recuperação de senha e autenticação institucional não existem. |
| RF03-RF05 | Parcial | O Administrador pode cadastrar salas e alterar seu estado; a consulta aceita filtros por estado e capacidade mínima. Docentes e materiais podem ser consultados. Edição/desativação, CRUD dos demais recursos e filtros por localização/competência ainda não estão disponíveis. |
| RF06 | Parcial | A interface autenticada cria, consulta, altera e cancela reservas via API; o servidor deriva a identidade da sessão e restringe solicitantes às próprias reservas. |
| RF07 | Implementado | O serviço injeta `Clock` e valida início estritamente futuro, minutos 00/30 e segundos/nanos zerados em criação e alteração, além de exigir término posterior. Testes unitários parametrizados, integração HTTP e contrato Node cobrem as violações, intervalos válidos e adjacência. |
| RF08 | Implementado | A sobreposição é verificada para sala, docentes associados e materiais associados; contas Professor são reutilizadas como docentes e intervalos adjacentes são aceitos. |
| RF09 | Implementado | Bloqueios pessimistas dos recursos em ordem determinística e testes PostgreSQL concorrentes verificam que sala, docente ou material compartilhado não é reservado em duplicidade. |
| RF10 | Parcial | Reservas novas exigem decisão do Responsável, independentemente do recurso; a API recusa aprovação por Administrador. Alterações de horário ainda não seguem um fluxo de solicitação e aprovação separado. |
| RF11 | Parcial | Administrador pode alterar o estado operacional da sala; novas solicitações e aprovações verificam o estado atual. Ainda não há agenda de bloqueios/manutenções por período nem alerta automático ao Responsável. |
| RF12-RF13 | Pendente | Validacao de alocacao docente e movimentacao de material nao estao implementadas. |
| RF14 | Parcial | Criação, mudanças de estado e alterações aprovadas de horário são registradas; a equipe consulta esses históricos por API. Alterações diretas de horário enquanto a reserva está solicitada ainda não geram trilha própria. |
| RF15 | Parcial | Transições operacionais estão disponíveis e solicitações não decididas até o início são expiradas e auditadas na próxima consulta. Alterações pendentes e sua expiração ainda não estão implementadas. |
| RF16 | Pendente | Relatorios ainda nao estao implementados. |
| RF17 | Parcial | O frontend responsivo apresenta estados e erros da API e oferece telas por perfil autenticado; verificação em navegador real permanece pendente. |
| RF18 | Implementado parcialmente | A API de autenticação, catálogos e reservas tem contrato OpenAPI e guia; fluxos mais amplos previstos no PRD ainda não foram implementados ou documentados. |
| RF19 | Parcial, divergente do PRD | O código atual envia eventos de aprovação, rejeição e cancelamento por HTTP e possui testes WireMock, mas o PRD vigente exige notificação simulada dentro da aplicação, sem serviço externo; essa forma ainda não está implementada. |

Detalhes de rotas e respostas: [documentacao da API de reservas](../api-reservas.md). RF01-RF02 têm implementação parcial de login por sessão e controle de acesso; não considerar o sistema pronto para produção enquanto os limites listados nesta matriz e os demais requisitos pendentes não forem tratados.

## Referencias

- [Personas](personas/)
- [PRD principal](../prd.md)
- [Especificacao do projeto da disciplina](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)
