# Organização de Recursos

Sistema de alocação de salas, professores e materiais sem conflitos de horário. Projeto semestral de Qualidade de Software (Senac TADS 2026.2), com Java 21, Spring Boot 3, Maven e PostgreSQL.

## Estado atual

A API de reservas está implementada para salas, com alocação opcional de docentes/materiais e login por sessão HTTP. Permanecem pendentes cadastro/gestão administrativa, bloqueios por período, auditoria, movimentação de materiais e relatórios. A situação de cada requisito está em [`docs/prd/requisitos-funcionais.md`](docs/prd/requisitos-funcionais.md), na seção "Situação da implementação atual".

## Pré-requisitos

- Java 21
- Docker (PostgreSQL local e testes com Testcontainers)
- Git

## Como executar

```
docker compose up -d
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. A interface em `/` autentica contra as contas do banco usando sessão HTTP e limita as operações ao perfil autenticado. As credenciais de demonstração são apenas para desenvolvimento local; veja os limites em [`docs/api-reservas.md`](docs/api-reservas.md).

Na primeira execução são criadas contas de demonstração, uma sala (30 lugares) e dois materiais ("Projetor multimídia" e "Notebook"):

| Perfil | E-mail | Senha |
|---|---|---|
| Professor | `professor@teste.local` | `Professor@123` |
| Coordenador | `coordenador@teste.local` | `Coordenador@123` |
| Responsável | `responsavel@teste.local` | `Responsavel@123` |
| Administrador | `admin@teste.local` | `Admin@123` |

As senhas são armazenadas com hash e o login mantém sessão HTTP; estes usuários e senhas são somente para demonstração local. Para desligar a criação/atualização das contas demonstrativas, use `APP_SEED_ENABLED=false`.

Variáveis de ambiente opcionais: `DB_URL`, `DB_USER`, `DB_PASSWORD`.
Para publicar atrás de HTTPS, configure `SESSION_COOKIE_SECURE=true`; mantenha `APP_AUTH_ENABLED=true`.

## Exemplo de uso da API

As rotas protegidas exigem cookie de sessão e token CSRF. Para testar com `curl`, obtenha o token, autentique-se e envie o token retornado no login:

```
curl -s -c cookies.txt http://localhost:8080/api/auth/csrf > csrf.json
CSRF=$(jq -r .csrfToken csrf.json)
curl -s -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -H "X-CSRF-Token: $CSRF" \
  -d '{"email":"professor@teste.local","senha":"Professor@123"}' > usuario.json
CSRF=$(jq -r .csrfToken usuario.json)
SOLICITANTE_ID=$(jq -r .id usuario.json)
curl -i -b cookies.txt -X POST http://localhost:8080/api/reservas \
  -H "Content-Type: application/json" -H "X-CSRF-Token: $CSRF" \
  -d "{\"salaId\":1,\"solicitanteId\":$SOLICITANTE_ID,\"tipoSolicitante\":\"PROFESSOR\",\"inicio\":\"2026-11-02T10:00:00\",\"fim\":\"2026-11-02T11:00:00\"}"
```

Contrato completo em [`docs/openapi.yaml`](docs/openapi.yaml) e guia em [`docs/api-reservas.md`](docs/api-reservas.md).

O formulário permite selecionar docentes e materiais. A conta Professor autenticada é sempre alocada à própria reserva; contas Coordenador podem selecionar docentes cadastrados. Para chamadas à API, consulte os catálogos autenticados `GET /api/professores` e `GET /api/materiais` e envie os IDs em `professorIds`/`materialIds`.

## Como testar

```
./mvnw -B verify
node --test tests/*.test.js
```

A suíte automatizada inclui testes Java unitários e de integração com PostgreSQL via Testcontainers, além de testes Node do contrato frontend/API. O planejamento e os limites de cobertura estão em [`docs/testes/`](docs/testes/).

## SonarCloud

O workflow do GitHub Actions executa a análise SonarCloud após `./mvnw -B verify`, quando
as variáveis `SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY` estiverem cadastradas no
repositório. Cadastre também o segredo `SONAR_TOKEN`; ele não deve ser salvo no código.
O relatório XML de cobertura enviado à análise é gerado pelo JaCoCo em
`target/site/jacoco/jacoco.xml`.

Para executar localmente, use os valores reais do projeto e forneça o token pelo ambiente:

```
$env:SONAR_TOKEN = "<token>"
.\mvnw.cmd -B verify sonar:sonar "-Dsonar.organization=<organização>" "-Dsonar.projectKey=<projectKey>" "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"
```

No PowerShell, remova o token da sessão ao terminar:

```
Remove-Item Env:SONAR_TOKEN
```

## Documentação

- [PRD](docs/prd.md) e [requisitos funcionais](docs/prd/requisitos-funcionais.md)
- [Requisitos não funcionais](docs/prd/requisitos-nao-funcionais.md)
- [Fluxos de sequência](docs/prd/fluxos-sequencia.md) e [personas](docs/prd/personas/)
- [Estratégia de testes](docs/testes/estrategia.md)
- [Inventário de regras e riscos](docs/testes/inventario-regras.md)
- [Casos de caixa preta](docs/testes/caixa-preta.md) e [de caixa branca](docs/testes/caixa-branca.md)
- [Registro do uso de IA](docs/ia/log-prompts.md)

## Estrutura

```
.
├── pom.xml, mvnw, docker-compose.yml
├── src/main/java/br/com/projeto_qualidade_software/projeto/
│   ├── api/            controllers, DTOs e tratamento de erros
│   ├── config/         dados iniciais de desenvolvimento
│   ├── entities/       entidades JPA
│   ├── enums/          estados e tipos
│   ├── exceptions/     exceções de negócio
│   ├── repositories/   acesso a dados
│   └── services/       regras de negócio
├── src/main/resources/static/   interface web estática
├── docs/               requisitos, contrato da API e documentos de teste
└── tests/              verificação do contrato do front-end (Node)
```

## Como contribuir

Trabalhe em uma branch, nunca direto na `main`. Rode `./mvnw -B verify` antes de cada commit e abra pull request com revisão de outro integrante do grupo.