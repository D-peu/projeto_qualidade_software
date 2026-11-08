# Requisitos Funcionais - Sistema de Alocacao de Recursos

Requisitos derivados da especificacao academica e das responsabilidades confirmadas para as personas. `RFxx` identifica um requisito funcional; `RNxx` identifica uma regra de negocio.

## 1. Autenticacao e controle de acesso

| ID | Requisito | Persona |
|---|---|---|
| RF01 | A aplicacao deve autenticar usuarios e disponibilizar funcionalidades conforme o perfil Solicitante, Responsavel ou Administrador. | Todas |
| RF02 | A aplicacao deve impedir que um usuario execute operacoes nao permitidas pelo seu perfil. | Todas |

## 2. Cadastros e disponibilidade

| ID | Requisito | Persona |
|---|---|---|
| RF03 | A aplicacao deve permitir ao Administrador cadastrar, consultar, editar e desativar salas, professores e materiais. | Administrador |
| RF04 | A aplicacao deve permitir ao Administrador gerenciar usuarios, bloqueios de recursos e periodos de manutencao. | Administrador |
| RF05 | A aplicacao deve permitir pesquisar recursos por tipo, capacidade, localizacao, competencia e disponibilidade. | Solicitante, Responsavel, Administrador |

## 3. Reservas e alocacao

| ID | Requisito | Persona |
|---|---|---|
| RF06 | A aplicacao deve permitir ao Solicitante criar, alterar e cancelar suas reservas. | Solicitante |
| RF07 | A aplicacao deve validar que o termino de cada reserva seja posterior ao inicio. | Solicitante, Responsavel, Administrador |
| RF08 | A aplicacao deve detectar sobreposicao de horario para cada sala, material ou professor envolvido na reserva. | Todas |
| RF09 | A aplicacao deve proteger a confirmacao da reserva contra solicitacoes simultaneas conflitantes, aceitando no maximo uma reserva para o mesmo recurso e intervalo. | Todas |
| RF10 | A aplicacao deve exigir aprovacao do Responsavel para solicitacoes que envolvam recursos classificados como restritos. | Solicitante, Responsavel |
| RF11 | A aplicacao deve impedir novas reservas de recursos bloqueados ou em manutencao. | Solicitante, Administrador |
| RF12 | A aplicacao deve permitir ao Responsavel validar a alocacao de docentes. | Responsavel |

## 4. Acompanhamento, auditoria e relatorios

| ID | Requisito | Persona |
|---|---|---|
| RF13 | A aplicacao deve registrar retirada e devolucao de materiais e equipamentos vinculados a uma reserva. | Responsavel |
| RF14 | A aplicacao deve manter historico auditavel das alteracoes e mudancas de estado das reservas. | Responsavel, Administrador |
| RF15 | A aplicacao deve suportar os estados solicitada, aprovada, em uso e concluida, e os estados alternativos rejeitada, cancelada e nao compareceu. | Todas |
| RF16 | A aplicacao deve permitir consultar relatorios de utilizacao por recurso, carga horaria alocada e conflitos evitados. | Responsavel, Administrador |
| RF17 | A aplicacao deve oferecer interface responsiva e mensagens de erro compreensiveis. | Todas |
| RF18 | A aplicacao deve documentar a API ou os fluxos publicos do sistema. | Todas |
| RF19 | A aplicacao deve permitir notificacao simulada ou integracao com API externa, conforme decisao de escopo a validar. | Todas |

## Regras de negocio

| ID | Regra | Requisitos relacionados |
|---|---|---|
| RN01 | O termino deve ser posterior ao inicio da reserva. | RF07 |
| RN02 | Reservas do mesmo recurso nao podem se sobrepor; recurso inclui sala, material e professor. | RF08 |
| RN03 | Duas solicitacoes simultaneas conflitantes devem resultar em no maximo uma reserva aceita. | RF09 |
| RN04 | Recursos em manutencao ou bloqueados nao podem ser reservados. | RF11 |
| RN05 | Somente o perfil Responsavel aprova solicitacoes de recursos restritos. | RF10 |
| RN06 | Os estados previstos sao solicitada, aprovada, em uso e concluida; estados alternativos incluem rejeitada, cancelada e nao compareceu. | RF14, RF15 |
| RN07 | Reservas iniciadas nao podem ser apagadas; mudancas de estado devem gerar registro de auditoria. | RF14, RF15 |
| RN08 | Retiradas e devolucoes de materiais devem ser registradas. | RF13 |

## Matriz de rastreabilidade

| Regra de negocio | Requisitos funcionais |
|---|---|
| RN01 - Validacao de intervalo | RF07 |
| RN02 - Ausencia de sobreposicao | RF08 |
| RN03 - Concorrencia | RF09 |
| RN04 - Bloqueio e manutencao | RF04, RF11 |
| RN05 - Aprovacao de recurso restrito | RF10 |
| RN06 - Estados da reserva | RF14, RF15 |
| RN07 - Nao exclusao e auditoria | RF14, RF15 |
| RN08 - Movimentacao de materiais | RF13 |

## Situacao da implementacao atual

Esta matriz registra o que o backend existente realmente garante. Ela nao substitui os requisitos-alvo nem significa que os itens parciais ou pendentes foram dispensados.

| Requisitos | Situacao no backend atual | Limite conhecido |
|---|---|---|
| RF01-RF02 | Parcial | Login por e-mail/senha com contas persistidas, hash PBKDF2 e sessão HTTP; rotas de reserva aplicam perfil e posse. Cadastro/gestão de contas, recuperação de senha e autenticação institucional não existem. |
| RF03-RF05 | Parcial | A API permite consultar salas, docentes (contas Professor) e materiais; cadastro/edição/desativação, gestão de usuários/bloqueios e pesquisa com filtros ainda não estão disponíveis. |
| RF06 | Parcial | A interface autenticada cria, consulta, altera e cancela reservas via API; o servidor deriva a identidade da sessão e restringe solicitantes às próprias reservas. |
| RF07 | Implementado | O intervalo rejeita inicio/fim ausentes e exige termino posterior ao inicio. |
| RF08 | Implementado | A sobreposição é verificada para sala, docentes associados e materiais associados; contas Professor são reutilizadas como docentes e intervalos adjacentes são aceitos. |
| RF09 | Implementado | Bloqueios pessimistas dos recursos em ordem determinística e testes PostgreSQL concorrentes verificam que sala, docente ou material compartilhado não é reservado em duplicidade. |
| RF10 | Parcial | Decisão exige sessão de Responsável ou Administrador e registra o decisor. Não há classificação de recurso restrito para exigir aprovação somente nesses casos. |
| RF11 | Parcial | A reserva rejeita sala cujo estado nao seja `DISPONIVEL`; nao ha agenda de bloqueios/manutencoes nem endpoint administrativo. |
| RF12-RF14 | Pendente | Validacao de alocacao docente, movimentacao de material e auditoria nao estao implementadas. |
| RF15 | Parcial | Os estados existem no enum, mas a API nao oferece o ciclo completo ate `EM_USO`, `CONCLUIDA` e `NAO_COMPARECEU`. |
| RF16 | Pendente | Relatorios ainda nao estao implementados. |
| RF17 | Parcial | O frontend responsivo apresenta estados e erros da API e oferece telas por perfil autenticado; verificação em navegador real permanece pendente. |
| RF18 | Implementado parcialmente | A API de autenticação, catálogos e reservas tem contrato OpenAPI e guia; fluxos mais amplos previstos no PRD ainda não foram implementados ou documentados. |
| RF19 | Pendente | Notificacao simulada ou integracao externa ainda nao foi definida/implementada. |

Detalhes de rotas e respostas: [documentacao da API de reservas](../api-reservas.md). RF01-RF02 têm implementação parcial de login por sessão e controle de acesso; não considerar o sistema pronto para produção enquanto os limites listados nesta matriz e os demais requisitos pendentes não forem tratados.

## Referencias

- [Personas](personas/)
- [Especificacao do projeto da disciplina](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)