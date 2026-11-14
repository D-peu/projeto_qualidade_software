# Gestão de defeitos e registro de achados

**Atualizado:** 2026-11-14
**Revisão de código verificada:** `release/rc1-blocker-fixes`, `2c83627`.
**RTM canônica:** [docs/RTM.md](./RTM.md).

Este registro separa defeitos reproduzidos/confirmados por histórico de código,
divergências de requisito e lacunas de funcionalidade. “Corrigido” significa
que existe commit de correção; a verificação local não equivale a revisão
independente, CI na `main` ou aceite para release.

## 1. Processo de gestão

### Identificação e registro

1. Registrar cenário, revisão/ambiente, passos, entrada, esperado e observado,
   RF/RN/RNF/INV, arquivos envolvidos, autor/revisor e evidência.
2. Classificar a evidência como reproduzida, estática, histórica ou não
   verificada. Código/teste presente não equivale a execução.
3. Se o comportamento requerido depende de decisão, registrar como divergência
   ou pendência até a decisão; não inventar um resultado esperado.
4. Cada item deve ter ID, título, severidade, prioridade, causa raiz, correção
   com commit (ou “não corrigido”), teste de regressão e estado atual.

### Severidade e prioridade

| Classificação | Critério |
|---|---|
| Crítica | Comprometimento amplo de dados/identidade, perda irreversível ou indisponibilidade total de função essencial sem contorno. |
| Alta | Violação de regra crítica, dupla reserva, autorização indevida confirmada ou bloqueio de fluxo essencial. |
| Média | Violação funcional/contratual delimitada ou risco operacional com alcance limitado. |
| Baixa | Inconsistência documental não bloqueante ou impacto reduzido. |

| Prioridade | Critério |
|---|---|
| P1 | Segurança, regra crítica, dupla reserva, aplicação que não inicia ou fluxo essencial bloqueado. |
| P2 | Impacto médio, contrato incorreto ou evidência necessária para aceitar requisito. |
| P3 | Melhoria planejada ou limitação de impacto baixo. |

### Estados e responsabilidades

Estados do processo: **aberto → em análise → em correção → corrigido →
retestado → encerrado**. “Corrigido” não significa aceito. O responsável técnico
registra causa e correção; QA/revisor executa o teste de regressão; responsável
de produto/equipe decide questões de requisito e aceita o encerramento. Um
defeito de alto impacto não deve ser encerrado somente pelo autor da correção.
Sem decisão ou evidência suficiente, manter aberto/pendente e explicitar o
bloqueio.

## 2. Registro de defeitos confirmados

| ID e título | Descrição / requisito | Severidade / prioridade | Causa raiz confirmada | Correção (commit) | Teste de regressão / evidência | Status |
|---|---|---|---|---|---|---|
| **DF-01 — Aprovação não revalidava manutenção da sala** | Reserva solicitada podia ser aprovada após a sala entrar em manutenção (RF11/RN04; INV-05). | Alta / P1 | A aprovação não verificava o estado atual do recurso dentro da operação protegida por lock. | `d7039d5` (`Fix release blockers and refresh quality evidence`): revalidação e bloqueio. | `ReservaPostgresIntegrationTest.deveRecusarAprovacaoQuandoSalaEntraEmManutencao` (CP-124), executado em `./mvnw -B clean verify` em 2026-11-14. | Corrigido e retestado localmente; revisão/CI integrada pendente. |
| **DF-02 — Administrador aprovava reserva** | O serviço permitia Administrador aprovar, contrariando a regra do Responsável para toda reserva (RF10/RN05; INV-04). | Alta / P1 | O serviço/modelo possuíam caminho de aprovação por Administrador e a autorização não aplicava o perfil exigido pelo PRD. | `d7039d5`: recusa aprovação por Administrador, inclusive para recursos não restritos. | `ReservaServiceTest.approvalRequiresResponsibleRegardlessOfMaterialRestriction`; `ReservaPostgresIntegrationTest.deveRecusarAprovacaoDeMaterialRestritoPorAdministrador`; `ReservaAutenticacaoPostgresE2ETest.administradorNaoPodeAprovarReservaSemMaterialRestrito`. Os testes passaram em E1. | Corrigido e retestado localmente; aprovação de alteração de horário continua pendente na RTM. |
| **DF-04 — Inicialização falhava em PostgreSQL vazio** | A instalação limpa falhava durante migrations por referência a `reservas` antes de a tabela existir (RNF-ISO-08; CP-158). | Alta / P1 | Ordem/dependência das migrations não criava primeiro as tabelas mínimas necessárias. | `d7039d5`: migration `V0_1__bootstrap_migration_dependencies.sql` cria a base anterior às migrations existentes. | `AplicacaoBancoLimpoPostgresTest` inicializa PostgreSQL sem schema prévio, valida HTTP 200 em `/api/auth/csrf` e verifica tabelas; passou em E1. | Corrigido e retestado localmente; clone limpo/README e CI do commit final ainda precisam de verificação. |
| **DF-05 — POM sem starter-web e JaCoCo** | Uma revisão histórica do POM não declarava o starter web necessário à aplicação e não tinha relatório/limites JaCoCo (RNF-ISO-08 e metas de cobertura). | Alta / P1 | Alterações de build removeram dependência e configuração de cobertura, deixando a aplicação web e gate JaCoCo sem configuração no POM. Evidência: `git show 2c1f015^:pom.xml` não contém os itens; `git show 2c1f015:pom.xml` os adiciona. | `2c1f015` (`build: restore web and coverage configuration`). | `./mvnw -B clean verify` passou em 2026-11-14; gerou `target/site/jacoco/jacoco.xml` e aplicou os limites. Essa execução valida o estado corrigido, não reproduz o build histórico quebrado. | Correção histórica confirmada; reteste local atual passou. |
| **DF-06 — Duas classes Spring Boot de inicialização** | O estado anterior a `2c1f015` continha `ProjetoApplication` e `ProjetoQualidadeSoftwareApplication`, ambas anotadas com `@SpringBootApplication`, tornando ambígua a inicialização/empacotamento. | Alta / P1 | A nova classe principal foi adicionada sem remover a classe principal antiga. | `2c1f015`: removeu `ProjetoApplication.java`. | `./mvnw -B clean verify` passou; `rg -l '@SpringBootApplication' src/main/java src/test/java` localizou uma única classe principal. | Corrigido e verificado no estado atual. |
| **DF-07 — ReservaService defasado em relação à API/testes** | O serviço anterior continha operações/suporte incompletos frente ao controller e testes; a camada da API não tinha implementação consistente para os fluxos de reserva. | Alta / P1 | A implementação do serviço estava incompleta/desalinhada das assinaturas e comportamentos já esperados pelo controller e testes. Evidência histórica: o commit restaurador adicionou operações ao serviço, repositório, entidade e testes. | `96fb630` (`fix: restore reservation service operations`). | `ReservaControllerTest.solicitarReservaRetornaCreatedLocationEReserva`, `alterarHorarioRetornaReservaComHorarioAtualizado`, `cancelarReservaRetornaReservaCancelada` e a suíte E1 passaram em 2026-11-14. | Corrigido historicamente; suíte atual passou. |

## 3. Divergências e pendências declaradas

| ID | Título / requisito | Severidade / prioridade | Causa ou estado observado | Correção | Regressão / evidência | Status |
|---|---|---|---|---|---|---|
| **PD-01 — Notificação externa não atende notificação interna** | RF19/RN11 requer notificações simuladas dentro do sistema, sem serviço externo. | Média / P2 | O código atual faz HTTP para `notificacao.url`. Os testes WireMock provam essa integração externa, não o requisito interno. | Não corrigido nesta entrega mínima; nenhuma mudança de escopo foi feita. | `ReservaNotificacaoWireMockIntegrationTest.approvalSendsExpectedNotificationBody`, `serverErrorDoesNotUndoRejection` e `notificationTimeoutDoesNotUndoCancellation` passaram em E1, mas não testam notificação interna. | **PENDÊNCIA DECLARADA** |
| **PD-02 — Fluxo de alteração pendente não implementado** | RF06/RF09/RF10/RF15; RN10/RN12 exigem aprovação de alteração e preservação do horário original. | Alta / P1 | A alteração atual muda diretamente o horário da reserva; não existe solicitação separada, decisão ou expiração pendente. | Não corrigido. | Testes atuais cobrem alteração direta e concorrência, não o fluxo pendente exigido. E1 não comprova o requisito ausente. | **PENDÊNCIA DECLARADA** |
| **PD-03 — Movimentação, relatórios e manutenção por período ausentes** | RF04/RF11/RF13/RF16; RN04/RN08. | Média / P2 para manutenção; Baixa / P3 para movimentação/relatórios | Não foram localizados endpoints/serviços para agenda de bloqueios, retirada/devolução ou relatórios. | Não corrigido. | Nenhum teste específico; conferir status por requisito em [RTM](./RTM.md). | **PENDÊNCIA DECLARADA** |
| **PD-04 — Pesquisa, CRUD e validação docente incompletos** | RF03/RF05/RF12. | Baixa / P3 | Existem cadastro/status de sala e filtros por estado/capacidade; não há CRUD completo, pesquisa por tipo/localização/competência ou fluxo de validação docente. | Não corrigido. | Testes do recorte implementado estão listados nas linhas 2–3 da RTM; itens ausentes não foram executados. | **PENDÊNCIA DECLARADA** |

## 4. Falso positivo não registrado como defeito

**DF-03 — “OpenAPI omitia resposta 403”** foi anotado anteriormente como defeito,
mas não foi confirmado: o próprio `d7039d5^:docs/openapi.yaml` já documentava
`403` para alteração e cancelamento. Não há correção de código/contrato necessária
para essa alegação; a ausência de validação automática do contrato permanece
como limitação, não como defeito reproduzido.

`SECURITY-REVIEW.md` afirmava que `GET /api/reservas/pendentes` era público.
Na revisão atual, a afirmação **não foi confirmada**: a rota está sob o interceptor
`/api/**` e `AutenticacaoRotasControllerTest.getApiSemSessaoRetorna401` a testa
sem sessão com resposta 401 (CP-160/RF01/RF02/INV-08). Portanto, não há DF para
essa alegação nesta revisão. A validade histórica do achado no SHA originalmente
revisado não foi reproduzida; o relatório está anotado como desatualizado para o
estado corrente em `SECURITY-REVIEW.md`.

## 5. Observações de execução não classificadas como defeitos confirmados

- E1 terminou com `BUILD SUCCESS`, mas Surefire emitiu “going to kill self fork
  JVM” após 30 segundos. Não foi localizado `System.exit` no código do projeto.
  A persistência de uma thread não daemon/recurso Spring/Testcontainers durante
  o shutdown é uma hipótese; a causa não foi confirmada. Manter como observação,
  sem atribuir causa ou falha aos testes.
- Na primeira execução de `verify` após os testes não relacionados a notificação,
  houve chamada a `localhost:8089` em `ReservaApiPostgresE2ETest`. O stub de
  `NotificacaoClient` foi adicionado a essa classe e à classe de concorrência.
  O reteste completo E1 não apresentou `Connection refused`; os testes de
  notificação são isolados em sua classe WireMock.

## 6. Evidência local desta atualização

- E1 — `./mvnw -B clean verify`, executado em 2026-11-14 UTC: 222 testes Java;
  zero falhas, erros ou ignorados; JaCoCo 89,60% linhas (991/1106) e 75,16%
  branches (242/322).
- E2 — `node --test tests/*.test.js`, executado em 2026-11-14 UTC: 19 testes;
  zero falhas.
- Os resultados são da branch local; esta documentação não declara CI remota,
  merge em `main`, SonarCloud ou revisão independente.
