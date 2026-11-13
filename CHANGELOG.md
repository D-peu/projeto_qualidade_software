# Changelog

## Unreleased

Esta seção reúne alterações da branch `release/rc1-blocker-fixes`. Ainda não corresponde a uma versão publicada, tag ou release candidate.

### Corrigido

- Inicialização com PostgreSQL vazio: adicionada migration de bootstrap para satisfazer as dependências das migrations existentes antes do `ddl-auto=update`; teste de integração verifica o início da API em banco limpo.
- Aprovação de reserva agora revalida o estado de manutenção da sala; teste PostgreSQL confirma que a solicitação permanece sem aprovação.
- Aprovação por Administrador é recusada independentemente de o material ser restrito, conforme RF10/RN05 do PRD.
- Expiração de solicitações pendentes registra auditoria de sistema.
- Contrato OpenAPI declara respostas `403` para cancelamento e alteração de horário.

### Adicionado

- Consulta autenticada do histórico de auditoria e das alterações de horário aplicadas.
- Filtros de salas por capacidade mínima e estado, e atualização administrativa do estado operacional da sala.
- Baseline JMeter local de consulta, criação e contenção, com JTL compactados e versionados em [`docs/performance/resultado-01.md`](docs/performance/resultado-01.md).

### Verificação local

- Java 21: `./mvnw -B clean verify` — 200 testes, sem falhas, erros ou ignorados; JaCoCo 87,79% de linhas e 74,84% de branches.
- Node: `node --test tests/*.test.js` — 19 testes aprovados.

### Ainda não incluído como release

- Não há revisão independente, CI/SonarCloud para esta branch, merge na `main`, tag anotada ou índice de evidências associado a commit de tag.
- Permanecem pendências funcionais e de validação listadas em [`docs/entrega/checklist.md`](docs/entrega/checklist.md) e [`docs/relatorio-qualidade.md`](docs/relatorio-qualidade.md).
