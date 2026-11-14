# Revisão de segurança

**Escopo:** revisão estática anterior do projeto Java/Spring Boot. Este
documento não representa certificação nem atesta a segurança do sistema.

## Reavaliação da alegação sobre reservas pendentes

O achado anterior afirmava que `GET /api/reservas/pendentes` era público. Essa
afirmação **não foi confirmada na revisão da branch
`release/rc1-blocker-fixes` em 2026-11-14**. O interceptor é registrado para
`/api/**`; sem sessão, a rota responde 401. O teste
`AutenticacaoRotasControllerTest.getApiSemSessaoRetorna401` inclui explicitamente
essa URI (CP-160/RF01/RF02/INV-08) e passou em
`./mvnw -B clean verify` (222 testes, zero falhas).

| Alegação anterior | Estado verificado | Evidência |
|---|---|---|
| `GET /api/reservas/pendentes` não exige autenticação | Não confirmada no estado desta branch; rota protegida | `AutenticacaoInterceptor`, teste parametrizado CP-160 retorna 401 sem sessão. |

A revisão não identificou o SHA da árvore originalmente associada à alegação;
portanto, não afirma se ela era válida naquela revisão histórica. Até obter
reprodução vinculada ao SHA exato, não registrar esse achado como vulnerabilidade
corrigida nem como vulnerabilidade atual.

## Limitações

- O resultado cobre a rota mencionada e as demais rotas enumeradas pelo teste
  CP-160/CP-161 sem sessão. Não comprova autorização correta para cada perfil e
  operação em ambiente de produção.
- O build e os testes locais não são análise de dependências nem Quality Gate
  SonarCloud; zero vulnerabilidades críticas não foi comprovado nesta sessão.
- Revisão estática e testes automatizados não substituem configuração segura de
  produção, revisão independente e avaliação de segurança completa.
