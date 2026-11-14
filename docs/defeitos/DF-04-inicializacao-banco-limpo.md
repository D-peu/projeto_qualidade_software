# DF-04 — Inicialização falha em PostgreSQL vazio

**Status:** corrigido no commit `d7039d5` e retestado nesta branch; aguarda revisão independente, CI remota e integração na `main`.
**Severidade/prioridade:** alta / P1, bloqueava inicialização e demonstração.  
**Rastreabilidade:** RNF-ISO-08; CP-158.

## Reprodução e causa

1. Subir um PostgreSQL 16 novo, sem tabelas ou histórico Flyway.
2. Iniciar a aplicação conforme o [README](../../README.md).
3. A inicialização falha no Flyway quando uma migration existente referencia `reservas` antes de a tabela existir.

O Flyway executa antes do Hibernate. As migrations existentes pressupunham tabelas prévias, enquanto o Hibernate só completaria o schema depois da execução do Flyway.

## Correção

`V0_1__bootstrap_migration_dependencies.sql` cria as tabelas mínimas referenciadas pelas migrations posteriores, usando `IF NOT EXISTS`. O Hibernate completa o schema restante conforme a configuração de desenvolvimento. A migration não substitui nem altera os registros históricos existentes.

## Reteste e evidência

- `AplicacaoBancoLimpoPostgresTest` usa Testcontainers com PostgreSQL vazio, valida HTTP 200 em `/api/auth/csrf` e verifica tabelas/coluna após as migrations.
- Em `2026-11-14 UTC`, `./mvnw -B clean verify` passou com 222 testes, zero falhas/erros/ignorados; JaCoCo 89,60% de linhas (991/1106) e 75,16% de branches (242/322). Esse comando executou `AplicacaoBancoLimpoPostgresTest`.
- Smoke HTTP manual não foi repetido nesta sessão e não é usado como evidência atual.
- A execução local não é CI da branch nem comprova que o commit foi integrado à `main`.

## Critério de encerramento

Obter revisão independente, executar CI verde no commit que contém a correção e repetir os passos do README no commit integrado. Só então atualizar o checklist de release; nenhuma tag foi criada.
