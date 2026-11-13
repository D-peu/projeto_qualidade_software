# Gestão de Defeitos e Inconsistências

**Escopo:** processo proposto para a equipe e registro atualizado em 2026-11-13 com a branch `release/rc1-blocker-fixes`.
**Base de rastreabilidade:** [RTM](rtm.md), [arquitetura e análise ATAM](arquitetura-atam.md) e [inventário de regras e riscos](testes/inventario-regras.md).
**Limite de evidência:** testes locais desta branch foram executados; ainda não há CI remota nem revisão independente para estas alterações. Resultados JMeter atuais estão em `docs/performance/resultado-01.md`; execuções CI citadas continuam históricas.

Este registro não altera os status da RTM nem declara itens encerrados sem aceitação/revisão. “Corrigido e retestado” nesta branch não significa que a mudança foi integrada na `main`.

## 1. Processo de gestão de defeitos

O repositório não contém procedimento formal de gestão de bugs. O fluxo abaixo é uma **proposta operacional** para adoção pela equipe, não uma prática cuja execução tenha sido comprovada.

### Identificação e registro

1. Qualquer integrante pode reportar defeito a partir de revisão de código, teste, uso exploratório, CI ou divergência documental.
2. Abrir um item no rastreador acordado pela equipe. Se não houver rastreador, manter um registro versionado aprovado pelo grupo. Não presumir que exista GitHub Issue associado a este documento.
3. Registrar: título, ambiente/revisão, pré-condições, passos e entradas, resultado esperado, resultado observado, frequência/reprodutibilidade, RF/RNF/RN/INV, arquivos/componentes, evidências anexas, testes relacionados e autor/revisor.
4. Marcar evidência como **reproduzida**, **estática**, **histórica** ou **não verificada**. Relatório ou caso especificado não substitui reprodução.
5. Se o comportamento requerido depende de decisão pendente (por exemplo, recurso restrito), classificar primeiro como divergência/decisão pendente; não registrar como falha funcional reproduzida.

### Severidade e prioridade

Severidade descreve impacto técnico/ao usuário; prioridade define ordem de tratamento. Não são equivalentes. A tabela é uma convenção proposta; deve ser aplicada pela equipe ao triage.

| Severidade | Critério de classificação |
|---|---|
| **Crítica** | Risco de comprometimento amplo de dados/identidade, perda ou corrupção irreversível, ou indisponibilidade total de função essencial sem contorno seguro. |
| **Alta** | Violação de regra crítica (incluindo dupla alocação ou autorização indevida confirmada), corrupção/indisponibilidade relevante ou bloqueio de fluxo essencial. |
| **Média** | Violação funcional/contratual delimitada, exposição ou comportamento incorreto com alcance limitado ou contorno conhecido. |
| **Baixa** | Problema de impacto reduzido, inconsistência documental não bloqueante ou melhoria sem falha de requisito demonstrada. |

| Prioridade | Critério de ordenação recomendado |
|---|---|
| **P1 — imediata** | Segurança/privacidade ou risco alto/crítico do inventário; falha em RF crítico; tratar contenção/mitigação antes de novas entregas. |
| **P2 — próxima entrega** | Impacto médio, contrato de API/documentação incorreto, ou lacuna de validação que impede aceitar um requisito como verificado. |
| **P3 — planejada** | Impacto baixo, limitação não bloqueante, decisão de escopo ou melhoria futura. |

Prioridade pode divergir da severidade por decisão de escopo, dependência ou mitigação, mas o motivo deve constar no item. Os impactos de RF07–RF11 e INV-01–INV-09 vêm do inventário; esta proposta não reclassifica a prioridade funcional definida nele.

### Estados do ciclo de vida

Estados solicitados: **aberto → em análise → em correção → corrigido → retestado → encerrado**. “Corrigido” significa que a alteração foi implementada, não que esteja aceita. Reabertura devolve o item a **aberto** ou **em análise** com nova evidência.

| Estado | Significado e critério de transição |
|---|---|
| **Aberto** | Registro criado, ainda sem triage concluído. |
| **Em análise** | Evidência, escopo, requisito, severidade, prioridade e reprodução estão sendo confirmados; decisões pendentes são explicitadas. |
| **Em correção** | Responsável técnico e plano de mudança/regressão foram atribuídos. |
| **Corrigido** | Correção foi incorporada na branch de trabalho e há referência à alteração; aguardando reteste. |
| **Retestado** | QA/revisor executou os testes de regressão especificados na revisão corrigida, registrou resultado e evidência. |
| **Encerrado** | Critérios de encerramento atendidos e aceitos pelo responsável de produto/QA; ou item foi encerrado como não defeito/duplicado com justificativa e referência. |

### Responsabilidades e encerramento

Papéis são funções, não pessoas ou atribuições já existentes:

- **Relator:** fornece cenário, passos e evidência sem antecipar causa.
- **Triage (QA + responsável de produto):** confirma rastreabilidade, valida resultado esperado, decide severidade/prioridade e trata dúvidas de requisito.
- **Responsável técnico/desenvolvedor:** analisa causa, propõe correção e teste de regressão; não encerra sozinho um defeito de alto impacto.
- **QA/revisor independente:** verifica a correção e registra execução/revisão dos testes acordados.
- **Responsável de produto/equipe:** resolve decisão funcional pendente e aprova encerramento quando a definição de comportamento depende do escopo.

Encerrar somente quando: requisito/contrato e causa estiverem identificados; correção estiver referenciada; testes de regressão aplicáveis tiverem resultado e revisão registrados; impacto colateral estiver considerado; documentação rastreável estiver atualizada se necessário; e QA/produto aceitarem. Se não for reproduzível ou o escopo não estiver decidido, manter em análise/pendente, ou fechar como não defeito apenas com justificativa explícita. Não contar CI geral como prova de um caso específico.

## 2. Registro de defeitos, divergências e limitações

### Defeitos confirmados ou divergências confirmadas

| Ref. | Classificação, severidade / prioridade | Requisito e impacto | Evidência estática e componentes | Testes/evidência existente | Tratamento recomendado e regressão |
|---|---|---|---|---|---|
| **DF-01** | **Defeito funcional confirmado, corrigido e retestado localmente nesta branch.** Severidade **Alta**, prioridade **P1** (INV-05/07). | RF11; RN04; INV-05/07; RNF-ISO-05. A aprovação precisava revalidar a disponibilidade da sala após uma solicitação já criada. | `ReservaService.aprovarReserva` agora bloqueia/revalida a sala e impede aprovar se ela entrou em `EM_MANUTENCAO`. | `ReservaPostgresIntegrationTest.deveRecusarAprovacaoQuandoSalaEntraEmManutencao` (CP-124) passou no `./mvnw -B clean verify` em Java 21: 200 testes, zero falhas/erros. | A correção está retestada localmente; aguarda revisão independente, CI da branch e integração na `main` para encerramento de release. |
| **DF-02** | **Defeito funcional confirmado, corrigido e retestado localmente nesta branch.** Severidade **Alta**, prioridade **P1** (INV-04). | RF02; RF10; RN05; INV-04. O serviço aceitava Administrador aprovando reserva sem recurso restrito, contrariando a aprovação pelo Responsável para toda reserva nova. | `ReservaService.aprovarReserva` recusa `ADMINISTRADOR` antes de alterar a reserva; o método de domínio de aprovação por Administrador foi removido. | `ReservaServiceTest.approvalRequiresResponsibleRegardlessOfMaterialRestriction`, `ReservaControllerTest.administradorNaoPodeAprovarReserva` e `ReservaAutenticacaoPostgresE2ETest.administradorNaoPodeAprovarReservaSemMaterialRestrito` passaram no `./mvnw -B clean verify` em Java 21 (200 testes no total). | Corrigido e retestado localmente para aprovação de reservas novas. A aprovação de alterações de horário continua como lacuna de RF10; aguarda revisão independente e CI remota. |
| **DF-03** | **Defeito documental confirmado e corrigido nesta branch.** Severidade **Média**, prioridade **P2** (INV-14). | RF18; INV-11/14. O contrato omitia `403` para cancelamento e alteração de horário. | `docs/openapi.yaml` agora declara `403` nessas operações; CP-71, CP-75 e CP-88 foram atualizados. | Inspeção estática do OpenAPI e dos casos documentais; ainda não há teste automatizado de conformidade do contrato contra respostas reais. | A omissão documental foi corrigida nesta branch. Manter como lacuna de validação até automatizar ou executar revisão de contrato independente; alterações ainda aguardam integração na `main`. |
| **DF-04** | **Defeito de inicialização confirmado, corrigido e retestado localmente nesta branch.** Severidade **Alta**, prioridade **P1** (bloqueava execução pela instalação limpa). | RNF-ISO-08; CP-158. Em PostgreSQL vazio, Flyway falhava porque uma migration posterior referenciava `reservas` antes de a tabela existir. | `V0_1__bootstrap_migration_dependencies.sql` cria as tabelas mínimas necessárias antes das migrations existentes; Hibernate completa o schema. | `AplicacaoBancoLimpoPostgresTest` inicia com PostgreSQL sem schema prévio, valida HTTP 200 em `/api/auth/csrf` e verifica as tabelas. `./mvnw -B clean verify` em Java 21: 200 testes, zero falhas/erros. Smoke HTTP manual no banco descartável também retornou 200. | Corrigido e retestado localmente. Aguardar revisão independente, CI da branch e execução do README no commit integrado antes de encerrar o bloqueador. Ver detalhes em [DF-04](defeitos/DF-04-inicializacao-banco-limpo.md). |

### Limitações conhecidas — não registradas como bugs reproduzidos

| Ref. | Requisitos / prioridade | Limitação e evidência | Testes relacionados / lacuna | Tratamento e regressão futura |
|---|---|---|---|---|
| **LK-01** | RF01/RF02/RF06; RNF-ISO-06; INV-08/10/12. **P1 para fechar a evidência de autorização**, sem afirmar falha de todos os controles. | Login, sessão, perfil, CSRF e verificações de posse existem. Não foi identificada jornada integrada completa com banco real e autorização habilitada. `ReservaApiPostgresE2ETest` usa `app.auth.enabled=false`, logo não é evidência de autorização. | `AutenticacaoControllerTest.loginCriaSessaoEPermiteRestaurarUsuarioAutenticado` e `bloqueiaApiSemSessaoEOperacaoComTokenCsrfInvalido`; `ContextoAutenticacaoTest.solicitanteNaoPodeInformarOutroIdOuTipo` e `responsavelNaoPodeSePassarPorAdministrador`; testes de posse em `ReservaServiceTest`. CI #21 executou Maven na revisão de referência, mas não cobre por si só cada rota/perfil. | Adicionar jornada integrada autenticada com banco para perfis, posse e CSRF nas rotas sensíveis. Regressão deve provar tanto rejeição não autorizada quanto sucesso autorizado. |
| **LK-02** | RF01/RF02; RNF-ISO-06; INV-08. **P1 para revalidar configuração antes de produção.** | `AutenticacaoWebConfig` só registra interceptor quando `app.auth.enabled=true`; padrão é true, mas a propriedade pode desligar a proteção em `/api/**`. `SECURITY-REVIEW.md` chama `/api/reservas/pendentes` de público; a afirmação não corresponde ao código/configuração padrão atual. Se autenticação for desligada, o interceptor deixa de proteger todas as rotas, não apenas essa listagem. Não há deployment de produção evidenciado. | `AutenticacaoControllerTest.bloqueiaApiSemSessaoEOperacaoComTokenCsrfInvalido` cobre alguns casos de sessão/CSRF, não demonstra todas as rotas sob configurações distintas. `ReservaControllerTest.listarReservasPendentesRetornaListaComHttpOk` testa listagem, não autorização. | Revalidar e corrigir o relatório de segurança em etapa aprovada; definir configuração de produção. Regressão: sem sessão, acessar `/api/reservas/pendentes` com autenticação habilitada e esperar 401; confirmar acesso permitido para perfil autorizado. Tratar desligamento global do interceptor apenas em ambiente de teste controlado. |
| **LK-03** | RF03/RF04/RF05; INV-07/18. **P1** para manutenção temporal por INV-07; **P3** para gestão/filtros por INV-18. | Gestão CRUD de contas/recursos, bloqueios por período e pesquisa filtrada não foram encontrados. Existem apenas listagens de salas, professores e materiais. Não há evidência de falha em código que implemente essas funções: são capacidades ausentes/parciais conforme RTM. | `SalaControllerTest` e `CatalogoRecursosControllerTest` verificam listagens; não cobrem CRUD ou filtros. `caixa-preta.md` identifica casos de bloqueio temporal como pendentes/inexecutáveis. | Definir escopo e construir os endpoints/serviços antes de abrir defeitos de regressão sobre funcionalidades inexistentes. Para manutenção, testar período dentro/fora, reservas já existentes e aprovação depois de alteração de disponibilidade. |
| **LK-04** | RF12; INV-18. **P3**, impacto baixo no inventário, sujeito a reavaliação. | Não foi encontrada implementação ou caso de teste específico para Responsável validar alocação docente. CP-91/92 e testes de listagem de reservas pendentes são fila de solicitações, não prova de RF12. | `ReservaControllerTest.listarReservasPendentesRetornaListaComHttpOk` e `ReservaApiPostgresE2ETest.deveListarSomenteReservasSolicitadasEmOrdemDeSolicitacao` cobrem listagem, não validação docente. | Definir fluxo de validação docente; depois criar teste de decisão/aprovação e teste negativo por perfil. Não associar testes de fila ao requisito. |
| **LK-05** | RF13/RF14/RF16/RF19; RNF-ISO-03; INV-13/15/16/17. **P2** para auditoria (impacto médio); **P3** para movimentação, relatórios e notificação (impacto baixo). | Movimentação de materiais e relatórios não estão implementados; auditoria cobre parte dos eventos. O cliente HTTP de notificação existe, mas diverge do PRD, que exige simulação interna sem API externa. | Auditoria tem testes de persistência/consulta; WireMock verifica o cliente externo, não a notificação interna exigida por RN11. | Completar trilha, movimentação e relatórios; resolver divergência de escopo em RF19/RN11 e testar o comportamento aprovado. |
| **LK-06** | RF15; RN06; RNF-ISO-05; INV-09. **P1**, impacto alto. | `StatusReserva` declara sete estados, mas a API atual alcança `SOLICITADA`, `APROVADA`, `REJEITADA` e `CANCELADA`; transições operacionais para `EM_USO`, `CONCLUIDA` e `NAO_COMPARECEU` não foram identificadas. Enum/estado preparado em banco não é evidência de transição de negócio. | `ReservaTest`, `ReservaServiceTest` e `ReservaPostgresIntegrationTest.deveBloquearSomenteEstadosQueOcupamASala` cobrem partes de estados e ocupação; a integração prepara estados para consulta, não implementa rotas de transição ausentes. | Definir transições permitidas e atores; testar cada transição válida e inválida na entidade/serviço e API. |
| **LK-07** | RF08/RF09; RNF-ISO-05; INV-02/03. **P1**, impacto alto/crítico. | Conflitos e concorrência estão implementados por consultas/locks pessimistas e cobertos por testes PostgreSQL. Trechos da documentação de sequência ainda dizem que a concorrência de professor/material não está coberta. É inconsistência documental, não defeito de concorrência confirmado. | `ReservaPostgresIntegrationTest` cobre solicitações concorrentes por professor e material; a RTM registra CI #21. A documentação de sequência é contraditória e requer correção aprovada. | Atualizar documentação em etapa autorizada e preservar regressões de conflito/concorrência para sala, professor, material e intervalos adjacentes. |
| **LK-08** | RNF-ISO-02/07; RF17; INV-14/17. **P2** para evidência e desempenho. | O baseline local de 13/11/2026 cobre consulta, criação e contenção e tem JTL compactados versionados em `docs/performance/`. As metas continuam provisórias e a execução não caracteriza produção. O CI após PR #13 executa Node, aplica os limites JaCoCo de 80% linhas/70% branches e publica relatórios Java/JaCoCo por 14 dias; JMeter permanece fora da CI. | `resultado-01.md` registra 35.225 consultas `200`, 7.702 criações `201` com `Location` e 50 requisições de contenção (1 `201`, 49 `409` esperados), em Java 21/JMeter 5.6.3/PostgreSQL 16.16. CI #24 é evidência histórica da `main`, não valida esta alteração local nem contém execução JMeter. A validação integrada completa de autorização continua ausente; RF12 e RF15 permanecem pendentes na RTM. | Formalizar metas e ambiente de desempenho; repetir em ambiente caracterizado, coletar métricas de host/banco e ampliar a validação integrada de autorização. Não declarar conformidade por presença de plano ou baseline local. |

## 3. Plano priorizado de tratamento

Prioridade considera os impactos do inventário e a existência de divergência confirmada. A recomendação não modifica as prioridades oficiais nem transforma lacuna de requisito em bug reproduzido.

| Ordem | Referência | Obrigatória ou melhoria | Tratamento | Regressão / evidência de aceite |
|---|---|---|---|---|
| **P1** | DF-01 / RF11 / INV-05/07 | **Correção implementada e retestada localmente** | A aprovação bloqueia/revalida a sala e recusa a transição se ela entrou em manutenção. | CP-124 / `deveRecusarAprovacaoQuandoSalaEntraEmManutencao` passou no `./mvnw -B clean verify` em Java 21; revisão independente e CI remota ainda pendentes. |
| **P1** | DF-02 / RF10 / INV-04 | **Correção implementada e validada** | Recusar aprovação por Administrador para qualquer reserva e manter a classificação como demonstrativa, conforme RN05. | `./mvnw -B verify` passou em 2026-11-13; teste de integração autenticado confirma 403 e ausência de mudança de estado/auditoria. |
| **P1** | LK-01/LK-02 / RF01/RF02/RF06 | **Fechamento de segurança/evidência obrigatório antes de aceitar autorização completa** | Criar jornada integrada autenticada e revalidar a alegação de endpoint público; fixar configuração segura de produção. | Sem sessão/CSRF/perfil/posse na jornada com `app.auth.enabled=true`, além do caso positivo; registrar configuração/revisão executada. |
| **P1** | LK-03/LK-06 / RF11/RF15 | **Implementação funcional prioritária** | Fechar decisão e implementação de manutenção temporal e transições operacionais ausentes conforme RF/INV. | Testes de intervalo bloqueado/não bloqueado, reserva já existente, alteração e aprovação; transições válidas/inválidas por estado e perfil. |
| **P2** | DF-03 / RF18 / INV-14 | **Omissão corrigida; validação completa continua pendente** | O OpenAPI declara 403 para alteração/cancelamento e os casos de caixa-preta correspondentes foram atualizados. | Automatizar ou executar revisão independente que compare respostas reais e schemas OpenAPI. |
| **P2** | LK-05 RF14 / INV-13 | **Implementação recomendada pelo impacto médio** | Definir requisitos de auditoria e implementar registro verificável das mudanças quando aprovado. | Testar persistência da trilha para cada alteração/transição; provar que leitura do histórico não depende apenas do estado corrente. |
| **P2** | LK-08 / RNF-ISO-02/07 | **Melhoria de evidência/qualidade** | Formalizar metas e ambiente de desempenho; repetir o baseline em ambiente caracterizado e avaliar se JMeter deve entrar na CI. Node, limites JaCoCo e publicação de relatórios já estão configurados no CI após PR #13. | Baseline local identificado e JTL compactados versionados em `docs/performance/`; medir ambiente/recursos e revisar no CI da revisão integrada. |
| **P3** | LK-03/LK-04/LK-05 | **Melhorias futuras conforme escopo** | Planejar CRUD, pesquisa, validação docente, movimentação, relatórios e notificação após decisões do produto. | Criar casos por RF antes de implementação e regressões que rastreiem cenário, componente e resultado. |

### Critérios gerais de aceite

- Toda correção tem requisito/RN/INV e caso de teste associado.
- Retestes locais desta branch foram registrados com comando, ambiente e resultado; ainda falta CI remota e revisão independente no commit integrado.
- Itens dependentes de decisão não são encerrados como “corrigidos” apenas por mudança documental.
- Evidência CI global histórica, teste especificado, código de teste existente, execução local atual e relatório JMeter permanecem categorias distintas.
- Após implementação aprovada, atualizar a RTM e a documentação relacionada em tarefas autorizadas; este documento sozinho não muda status de requisito.

## 4. Referências

- [RTM](rtm.md) e [arquitetura/ATAM](arquitetura-atam.md).
- [Inventário de regras e riscos](testes/inventario-regras.md), [casos de caixa-preta](testes/caixa-preta.md), [estratégia de testes](testes/estrategia.md), [relatório JMeter](testes/jmeter/relatorio-desempenho.md).
- [Contrato OpenAPI](openapi.yaml), [guia da API](api-reservas.md) e [revisão de segurança existente](../SECURITY-REVIEW.md).
- Código envolvido: `src/main/java/br/com/projeto_qualidade_software/projeto/services/ReservaService.java`, `api/ReservaController.java`, `api/ApiExceptionHandler.java`, `security/AutenticacaoWebConfig.java`, `security/AutenticacaoInterceptor.java`.
- Testes citados: `src/test/java/br/com/projeto_qualidade_software/projeto/services/ReservaServiceTest.java`, `api/ReservaControllerTest.java`, `api/AutenticacaoControllerTest.java`, `security/ContextoAutenticacaoTest.java`, `integration/ReservaPostgresIntegrationTest.java` e `integration/ReservaApiPostgresE2ETest.java`.
