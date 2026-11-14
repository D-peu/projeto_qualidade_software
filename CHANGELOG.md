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

- `./mvnw -B clean verify` — 222 testes, sem falhas, erros ou ignorados; JaCoCo 89,60% de linhas e 75,16% de branches. Execução local em clone limpo em 2026-11-14, com JDK 25.0.5.
- Node: `node --test tests/*.test.js` — 19 testes aprovados.
- CI do PR #20 no SHA `9cad467`: Verify e Node passaram com Java 21; artifact de relatórios publicado. SonarCloud foi ignorado.

Estes numeros sao evidencias locais da branch `release/rc1-blocker-fixes`, nao
resultados da `main` ou de um release publicado.

### Ainda não incluído como release

- Revisão independente e merge na `main` continuam pendentes. O CI do PR #20 passou no SHA `9cad467` com Java 21; SonarCloud foi ignorado, e não há CI desse SHA integrado na `main`, tag anotada ou índice de evidências associado a tag.
- Permanecem pendências funcionais e de validação listadas em [`docs/entrega/checklist.md`](docs/entrega/checklist.md) e [`docs/relatorio-qualidade.md`](docs/relatorio-qualidade.md).
