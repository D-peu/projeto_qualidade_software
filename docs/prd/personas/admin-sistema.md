# Persona: Administrador do Sistema

## Descricao

Perfil responsavel pela administracao dos cadastros e da disponibilidade dos recursos universitarios. Gerencia salas, professores, materiais, usuarios, bloqueios e periodos de manutencao. O alcance exato das permissoes administrativas permanece sujeito a validacao institucional.

## Objetivos

- Manter atualizados os cadastros de salas, professores, materiais e usuarios.
- Bloquear recursos indisponiveis e registrar periodos de manutencao.
- Evitar que informacoes incompletas ou desatualizadas causem reservas invalidas.
- Consultar o historico das alteracoes administrativas.

## Jornada na Aplicacao

1. **Login** - acessa as funcoes administrativas com sua conta.
2. **Visao de recursos** - consulta cadastros e disponibilidade.
3. **Gestao de cadastros** - cria ou atualiza salas, professores e materiais.
4. **Bloqueios e manutencao** - registra a indisponibilidade de recursos.
5. **Gestao de usuarios** - administra contas conforme permissoes concedidas.
6. **Auditoria** - consulta mudancas relevantes e encerra a sessao.

## Regras de Negocio

- **RN04 - Indisponibilidade:** recursos bloqueados ou em manutencao nao podem receber novas reservas.
- **RN05 - Aprovacao:** reservas e alteracoes de data/horario dependem do Responsavel; Administrador nao aprova.
- **RN07 - Auditoria:** mudancas de estado e alteracoes relevantes devem ser registradas.

## Permissoes

| Fluxo | Acesso previsto |
|---|---|
| Gerenciar salas, professores e materiais | Permitido |
| Gerenciar usuarios | Permitido; escopo de cada operacao a validar |
| Registrar bloqueios e manutencao | Permitido |
| Consultar historico e relatorios | A validar |
| Aprovar solicitacao de recurso restrito | Nao atribuido a este perfil pela especificacao atual |

## Qualidade relevante - ISO/IEC 25010

- **Seguranca:** restringir funcoes administrativas a usuarios autorizados e registrar alteracoes.
- **Adequacao funcional:** manter cadastros e indisponibilidades coerentes com a realidade.
- **Capacidade de interacao:** permitir identificar estado e disponibilidade do recurso com clareza.
- **Manutenibilidade:** apoiar diagnostico por historico, testes e evidencias rastreaveis.

## Pontos a validar

- Se existe mais de um nivel de Administrador.
- Quais operacoes de usuario sao permitidas (criar, editar, ativar/desativar ou excluir).
- Manutencao bloqueia novas reservas, preserva registros existentes e alerta o Responsavel para avaliar cada caso.