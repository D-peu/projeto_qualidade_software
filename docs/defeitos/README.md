# Registro de defeitos

O registro com causa raiz, commit, teste de regressão e estado está em
[gestao-defeitos.md](../gestao-defeitos.md). Esta pasta mantém os relatos
detalhados.

| ID | Achado | Estado na branch |
|---|---|---|
| DF-01 | Aprovação sem revalidar manutenção | Corrigido em `d7039d5`; teste de integração passou localmente. |
| DF-02 | Administrador aprovava reserva | Corrigido em `d7039d5`; regressões de serviço, integração e HTTP passaram. |
| DF-04 | [Inicialização falha em PostgreSQL vazio](DF-04-inicializacao-banco-limpo.md) | Corrigido em `d7039d5`; Testcontainers passou em `2026-11-14`. |
| DF-05 | POM sem `starter-web` e JaCoCo | Corrigido historicamente em `2c1f015`; `clean verify` e JaCoCo passaram na branch. |
| DF-06 | Duas classes `@SpringBootApplication` | Corrigido em `2c1f015`; existe uma classe principal e a suíte passou. |
| DF-07 | `ReservaService` defasado do controller/testes | Corrigido em `96fb630`; suíte atual passou. |

DF-03 (alegação de ausência de 403 no OpenAPI) não foi confirmado: o contrato já
declarava essas respostas antes do commit de correção. A alegação antiga de
`/api/reservas/pendentes` público também não foi confirmada na revisão atual; ver
[reavaliação](../../SECURITY-REVIEW.md).
