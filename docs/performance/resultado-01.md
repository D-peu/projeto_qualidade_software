# Resultado de desempenho 01 — JMeter

**Executado em:** 2026-11-13 (UTC)  
**Escopo:** baseline local exploratório da API; não representa ambiente de produção nem aprovação de meta oficial.

## Ambiente

- Java 21.0.13+2 (Microsoft build), Spring Boot 3.5.7.
- Apache JMeter 5.6.3 em modo não-GUI.
- PostgreSQL 16.16 em contêiner dedicado, projeto Compose `reservas-jmeter`, porta local 5434.
- Aplicação em `http://localhost:8080`, conectada apenas ao banco descartável.
- Dados sintéticos; seed desativado.
- `APP_AUTH_ENABLED=false` somente para estes planos, que não autenticam sessões. Portanto, esta execução **não é evidência de autenticação ou autorização**.
- Host Linux do workspace. CPU, memória, latência de banco e características de produção não foram medidos.

JMeter foi primeiro iniciado com o JDK 25 disponível por padrão; o script Groovy do plano de criação falhou com `Unsupported class file major version 69`. Essa tentativa foi excluída dos resultados. A execução válida foi repetida com o JDK 21 do projeto; os três planos finalizaram e suas asserções passaram.

## Método

Os três planos existentes foram executados com 10 usuários, ramp-up de 10 segundos e duração configurada de 60 segundos para consulta e criação. A contenção foi uma rajada única com 50 threads sincronizadas. O banco continha 50 salas, um solicitante sintético e uma reserva de referência. Os IDs foram consultados no banco após a preparação.

Resultados calculados a partir de `statistics.json` e conferidos nos JTL. `p90`, `p95` e `p99` correspondem aos percentis do relatório JMeter. A janela observada mede do início da primeira amostra HTTP ao fim da última; pequenas diferenças em relação aos 60 segundos configurados são esperadas por ramp-up e encerramento.

## Resultados

| Cenário | Amostras HTTP | Janela observada | Média | p50 | p90 | p95 | p99 | Máximo | Throughput | Resultado |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Consultas (dois endpoints) | 35.225 | 59,894 s | 15,62 ms | 14 ms | 22 ms | 26 ms | 36 ms | 230 ms | 588,12 req/s | 35.225 respostas `200`; zero falhas |
| Criação sem conflito | 7.702 | 58,472 s | 71,15 ms | 65 ms | 98 ms | 112 ms | 154 ms | 470 ms | 131,72 req/s | 7.702 respostas `201` com `Location`; zero falhas |
| Contenção, rajada única | 50 | 2,196 s incluindo amostras auxiliares | 291,02 ms | 279,5 ms | 458,8 ms | 492,55 ms | 514 ms | 514 ms | Não comparável | 1 resposta `201`, 49 respostas `409` esperadas; zero respostas inesperadas |

No cenário de consultas, foram 17.616 leituras por ID e 17.609 listagens por solicitante, todas `200 OK`. Na criação, cada iteração usou um intervalo distinto e a asserção verificou tanto `201 Created` quanto a presença de `Location`. Na contenção, o plano também executou uma amostra de inicialização e uma amostra final de conferência; por isso o JTL contém 52 linhas no total, mas apenas 50 são requisições HTTP concorrentes. A asserção final confirmou a contagem exata de um sucesso e 49 conflitos.

Após os cenários, a consulta ao PostgreSQL encontrou 7.704 reservas `SOLICITADA`: uma reserva de referência, 7.702 criações sem conflito e uma criação vencedora da disputa.

## Interpretação e limites

- Os resultados são uma medição local exploratória com carga menor e mais curta que os valores experimentais padrão documentados (50 usuários e 300 segundos).
- Os alvos provisórios de latência e taxa de erro ainda não são requisitos oficiais aprovados. Este relatório não declara conformidade ou reprovação contra eles.
- A contenção foi executada uma única vez; não permite inferência estatística sobre repetibilidade.
- Autenticação foi desligada no processo dedicado de carga; os resultados não substituem testes de segurança.
- Não foram coletadas métricas de CPU/memória, nem métricas internas de banco ou host. Não se pode concluir capacidade de produção nem atribuir latência a um componente.
- O JDK 25 teve uma incompatibilidade real com o compilador Groovy embarcado no JMeter 5.6.3. Seus resultados não foram usados; a execução válida usou Java 21.

## Reprodutibilidade e evidências brutas

Os planos, comandos de preparação, isolamento do banco e limpeza estão em [guia JMeter](../testes/jmeter/README.md). Os arquivos JTL originais foram compactados sem alteração e versionados junto deste relatório:

| Cenário | JTL compactado | SHA-256 |
|---|---|---|
| Consulta | [consulta.jtl.gz](evidencias/resultado-01/consulta.jtl.gz) | `a216d2a7ef7ca39e764733d4327f71c5a99c37573053bc0b0ed9962939c469c2` |
| Criação | [criacao.jtl.gz](evidencias/resultado-01/criacao.jtl.gz) | `022c40936335f80185d992269ec8b80948779ea9df21bc10bdad9d5b9988435f` |
| Contenção | [contencao.jtl.gz](evidencias/resultado-01/contencao.jtl.gz) | `b4a63a97062a2bb85425cb11ab43e5015804750206330cbffb10f5bf5f3057e4` |

Os JTL registram somente os resultados HTTP dos dados sintéticos. Os relatórios HTML gerados pelo JMeter permanecem em `target/jmeter/` e não são necessários para conferir as contagens e percentis consolidados aqui.
