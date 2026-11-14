# C4 — Diagrama de contexto

**Revisão representada:** branch `release/rc1-blocker-fixes`, verificada em
2026-11-14. O diagrama mostra integrações e atores do recorte implementado; não
implica que requisitos pendentes estejam disponíveis.

```mermaid
C4Context
    title Sistema de Organização de Recursos — contexto

    Person(solicitante, "Solicitante", "Professor ou coordenador: consulta disponibilidade e solicita/acompanha suas reservas.")
    Person(responsavel, "Responsável", "Decide reservas e acompanha a utilização dos recursos.")
    Person(administrador, "Administrador", "Administra salas e operações disponíveis para seu perfil.")

    System_Boundary(sistema, "Sistema de Organização de Recursos") {
        System(app, "Aplicação web e API", "Spring Boot 3.5.7; interface web, sessão/CSRF e API REST de reservas e recursos.")
    }

    SystemDb(db, "PostgreSQL 16", "Persistência de contas, reservas, recursos, auditoria e histórico de horário; schema gerenciado por Flyway.")
    System_Ext(notificacao, "Serviço HTTP de notificação", "Destino configurado por notificacao.url; recebe eventos de aprovação, rejeição e cancelamento.")

    Rel(solicitante, app, "Consulta e solicita/cancela reservas", "HTTPS/HTTP + sessão e CSRF")
    Rel(responsavel, app, "Consulta fila e aprova/rejeita", "HTTPS/HTTP + sessão e CSRF")
    Rel(administrador, app, "Consulta salas e executa operações administrativas disponíveis", "HTTPS/HTTP + sessão e CSRF")
    Rel(app, db, "Lê/grava dados e auditoria", "JPA/JDBC")
    Rel(app, notificacao, "Envia NotificacaoReserva após mudança de estado", "HTTP POST")
```

## Limites e divergências

- `GET /api/auth/csrf`, `POST /api/auth/login` e arquivos estáticos iniciam o
  fluxo sem identidade; as demais rotas `/api/**` exigem sessão. Mutations
  também exigem CSRF.
- A aplicação e o PostgreSQL são integrações realmente usadas pelo recorte
  documentado. O serviço de notificação externo existe e é testado com
  WireMock, mas diverge de RF19/RN11: o requisito do projeto pede notificação
  simulada dentro do sistema, sem serviço externo. A divergência permanece
  pendente e não deve ser interpretada como requisito atendido.
- Cadastro/gestão de usuários e recursos, manutenção por período, movimentação
  de materiais, relatórios e fluxo aprovado de alteração de horário estão
  pendentes conforme [RTM](../RTM.md).

## Diagramas relacionados

- [Componentes implementados](../prd/modelagem/estatica/componentes.md)
- [Sequências críticas e limitações](../prd/fluxos-sequencia.md)
