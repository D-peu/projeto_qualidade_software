# Requisitos Nao Funcionais - ISO/IEC 25010

Este documento usa o modelo de qualidade de produto da ISO/IEC 25010:2023 como estrutura para requisitos e evidencias de validacao. A referencia nao significa certificacao ou conformidade formal.

`RNF-ISO-xx` identifica o requisito de qualidade. Metas da disciplina estao identificadas como tais; metas de projeto e criterios de validacao estao registradas separadamente.

| ID | Caracteristica | Requisito de qualidade | Evidencia / criterio de validacao |
|---|---|---|---|
| RNF-ISO-01 | Adequacao funcional | Os requisitos criticos devem estar cobertos por cenarios de verificacao e rastreados na RTM. | Meta da disciplina: 100% dos requisitos criticos na RTM. Validar RF07-RF11 com casos positivos, negativos e de limite. |
| RNF-ISO-02 | Eficiencia de desempenho | Os fluxos de consulta de disponibilidade e reserva devem ser medidos sob carga. | Criterio do projeto: 25 usuarios simultaneos por 10 minutos, com p95 de resposta de ate 2 segundos. Evidenciar plano, configuracao e resultado do JMeter. |
| RNF-ISO-03 | Compatibilidade | As integracoes externas confirmadas no escopo devem ser testadas em ambiente controlado. | O escopo atual usa notificacoes simuladas, sem integracao externa; WireMock nao se aplica enquanto esse escopo permanecer. |
| RNF-ISO-04 | Capacidade de interacao | A interface deve ser responsiva e apresentar mensagens de erro compreensiveis. | Validar navegacao por teclado, foco visivel, contraste legivel e fluxos em desktop/celular nos navegadores Chrome e Firefox atuais. Nao ha meta formal de conformidade WCAG. |
| RNF-ISO-05 | Confiabilidade | O sistema deve preservar a consistencia das reservas diante de concorrencia, conflitos e manutencao. | Testes devem provar que solicitacoes pendentes podem coincidir, mas no maximo uma solicitacao conflitante e aprovada; testar tambem alteracoes concorrentes, manutencao e bloqueios. |
| RNF-ISO-06 | Seguranca | Autenticacao, autorizacao por perfil, validacao de entrada e tratamento seguro de erros devem ser verificados. | Testes de autorizacao e entrada; meta da disciplina: zero defeitos ou vulnerabilidades criticas. |
| RNF-ISO-07 | Manutenibilidade | O projeto deve permitir verificacao automatizada e rastreavel da qualidade. | Metas da disciplina: 80% de cobertura de linhas, 70% de branches; JUnit 5, testes unitarios parametrizados, integracao, API caixa-preta, E2E, CI, JaCoCo e SonarCloud. |
| RNF-ISO-08 | Flexibilidade | A aplicacao deve ser executavel com instrucoes de instalacao documentadas e ter ambientes suportados explicitados. | Entregar aplicacao executavel e instrucoes; suportar Chrome e Firefox atuais em desktop e celular. |
| RNF-ISO-09 | Seguranca operacional (safety) | A equipe deve avaliar se recursos reais exigem restricoes adicionais para prevenir uso inadequado. | Inventario atual e ficticio; avaliar riscos e registrar aplicabilidade se a equipe usar recursos reais. |

## Evidencias e ferramentas exigidas pela disciplina

- Testes unitarios com JUnit 5, incluindo testes parametrizados.
- Testes de integracao, API em caixa-preta e end-to-end.
- Banco de dados containerizado com Testcontainers.
- WireMock para testes de integracoes externas, caso uma integracao seja adicionada ao escopo.
- Teste de concorrencia demonstrando protecao contra dupla reserva.
- Integracao continua em pull requests, cobertura JaCoCo e SonarCloud.
- JMeter para teste de carga, com plano e resultado documentados.

As metas percentuais sao requisitos da disciplina e nao valores prescritos pela ISO/IEC 25010. As metas de carga sao criterios iniciais escolhidos para o projeto; a disciplina exige teste de carga, mas nao fixa esses valores.

## Pendencias

- Validar com equipe/docente se o inventario demonstrativo ficticio e suficiente; avaliar safety se recursos reais forem usados.

## Referencias

- [PRD principal](../prd.md)
- [Requisitos funcionais](requisitos-funcionais.md)
- [Inventario demonstrativo](inventario-recursos.md)
- [Especificacao do projeto da disciplina](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)
