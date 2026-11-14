# Organização de Recursos

Sistema de alocação de salas, professores e materiais sem conflitos de horário. Projeto semestral de Qualidade de Software (Senac TADS 2026.2), com Java 21, Spring Boot 3, Maven e PostgreSQL.

## Estado atual

A API de reservas está implementada parcialmente para salas, com alocação opcional de docentes/materiais e login por sessão HTTP. O Administrador pode cadastrar salas adicionais; edição/desativação e cadastro/gestão de outros recursos, bloqueios por período, aprovação de alterações de horário, auditoria completa, movimentação de materiais e relatórios permanecem pendentes ou parciais. A RTM canônica é [`docs/RTM.md`](docs/RTM.md). Não declarar o sistema pronto para produção com base nos testes locais.

## Pré-requisitos

- Java 21
- Docker Engine com `docker compose` (PostgreSQL local e testes Testcontainers)
- Node.js 24 para os testes do frontend (usa `node:test`, sem instalar pacotes)
- Git
- Apache JMeter 5.6.3 somente para reproduzir o teste de carga
- `jq` somente para o exemplo opcional de chamadas `curl`

## Como executar

```
docker compose up -d db
./mvnw -B spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. Em um PostgreSQL vazio, o bootstrap do Flyway prepara as tabelas necessárias às migrations existentes; o restante do schema é atualizado pelo Hibernate. O caso CP-158 e o teste Testcontainers `AplicacaoBancoLimpoPostgresTest` verificam esse início limpo. A interface em `/` autentica contra as contas do banco usando sessão HTTP e limita as operações ao perfil autenticado. As credenciais de demonstração são apenas para desenvolvimento local; veja os limites em [`docs/api-reservas.md`](docs/api-reservas.md).

Na primeira execução são criadas contas de demonstração, uma sala (30 lugares) e dois materiais ("Projetor multimídia" e "Notebook"):

| Perfil | E-mail | Senha |
|---|---|---|
| Professor | `professor@teste.local` | `Professor@123` |
| Coordenador | `coordenador@teste.local` | `Coordenador@123` |
| Responsável | `responsavel@teste.local` | `Responsavel@123` |
| Administrador | `admin@teste.local` | `Admin@123` |

As senhas são armazenadas com hash e o login mantém sessão HTTP; estes usuários e senhas são somente para demonstração local. Para desligar a criação/atualização das contas demonstrativas, use `APP_SEED_ENABLED=false`.

Entre como Administrador para cadastrar salas adicionais; cada sala recebe ID do banco e inicia como `DISPONIVEL`.

Variáveis de ambiente opcionais: `DB_URL`, `DB_USER`, `DB_PASSWORD`,
`APP_SEED_ENABLED`, `APP_AUTH_ENABLED`, `SESSION_COOKIE_SECURE` e
`NOTIFICACAO_URL`. Para publicar atrás de HTTPS, configure
`SESSION_COOKIE_SECURE=true`; mantenha `APP_AUTH_ENABLED=true`. Pare a
aplicação com Ctrl+C; `docker compose down` para o banco sem remover o volume.

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
  -d "{\"salaId\":1,\"solicitanteId\":$SOLICITANTE_ID,\"tipoSolicitante\":\"PROFESSOR\",\"inicio\":\"2040-11-02T10:00:00\",\"fim\":\"2040-11-02T11:00:00\"}"
```

Contrato completo em [`docs/openapi.yaml`](docs/openapi.yaml) e guia em [`docs/api-reservas.md`](docs/api-reservas.md).

O formulário permite selecionar docentes e materiais. A conta Professor autenticada é sempre alocada à própria reserva; contas Coordenador podem selecionar docentes cadastrados. Para chamadas à API, consulte os catálogos autenticados `GET /api/professores` e `GET /api/materiais` e envie os IDs em `professorIds`/`materialIds`.

Após aprovação, rejeição ou cancelamento, o backend envia um `POST` com o evento
`NotificacaoReserva` ao endereço configurado em `NOTIFICACAO_URL` (propriedade
`notificacao.url`; padrão local `http://localhost:8089/notificacoes/reservas`).
O timeout de conexão e leitura é configurável por `NOTIFICACAO_TIMEOUT` (padrão
`500ms`). Erro HTTP ou timeout é registrado no log e não reverte a reserva.
O contrato do corpo está em [`docs/openapi.yaml`](docs/openapi.yaml).

## Como testar

```
./mvnw -B clean verify
node --test tests/*.test.js
```

A suíte inclui testes Java unitários e de integração com PostgreSQL via
Testcontainers, concorrência de criação/aprovação/alteração, testes de
notificação HTTP com WireMock e Node do contrato frontend/API. Os testes Java de
integração precisam de Docker. `verify` gera JaCoCo e falha se o bundle ficar
abaixo de 80% de linhas ou 70% de branches. Abra
`target/site/jacoco/index.html` no navegador; o XML fica em
`target/site/jacoco/jacoco.xml`.

### Verificação local registrada

Na branch `release/rc1-blocker-fixes`, `./mvnw -B clean verify` passou em
2026-11-14 com 222 testes Java, zero falhas/erros/ignorados; cobertura JaCoCo
89,60% de linhas (991/1106) e 75,16% de branches (242/322).
`node --test tests/*.test.js` passou com 19 testes. São resultados locais da branch, não da
CI ou `main`; veja [RTM](docs/RTM.md) e [decisão de qualidade](docs/decisao-qualidade.md).

### JMeter

Instale JMeter 5.6.3 e siga o [plano de carga](docs/testes/jmeter/README.md),
que cria banco Compose isolado na porta 5434, prepara dados sintéticos e executa
consulta, criação e contenção por intervalo. Não use o banco de desenvolvimento.
Os resultados em [`docs/performance/resultado-01.md`](docs/performance/resultado-01.md)
são históricos; o ensaio não foi reproduzido nesta sessão e não representa
desempenho de produção.

## SonarCloud

O workflow do GitHub Actions executa a análise SonarCloud após `./mvnw -B verify`
quando as Actions **variables** `SONAR_ORGANIZATION` e `SONAR_PROJECT_KEY`
estiverem cadastradas no repositório. Cadastre `SONAR_TOKEN` em Actions
**secrets**; nunca salve token no código ou em arquivos `.env` versionados.
O relatório XML de cobertura enviado à análise é gerado pelo JaCoCo em
`target/site/jacoco/jacoco.xml`.

Para executar localmente, use os valores reais do projeto e forneça o token pelo
ambiente (não o escreva no comando nem no histórico do shell):

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
- [C4 de contexto](docs/arquitetura/c4-contexto.md) e [componentes](docs/prd/modelagem/estatica/componentes.md)
- [Estratégia de testes](docs/testes/estrategia.md)
- [Inventário de regras e riscos](docs/testes/inventario-regras.md)
- [Casos de caixa preta](docs/testes/caixa-preta.md) e [de caixa branca](docs/testes/caixa-branca.md)
- [RTM única](docs/RTM.md), [relatório de qualidade](docs/relatorio-qualidade.md), [decisão de qualidade](docs/decisao-qualidade.md) e [checklist de entrega](docs/entrega/checklist.md)
- [Resultado de desempenho JMeter](docs/performance/resultado-01.md) e [registro de defeitos](docs/gestao-defeitos.md)
- [Changelog](CHANGELOG.md)
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