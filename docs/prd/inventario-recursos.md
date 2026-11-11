# Inventario demonstrativo de recursos

Este inventario usa exclusivamente dados ficticios do prototipo e exemplos aprovados para demonstracao. Nao representa levantamento fisico da instituicao. A classificacao de recurso restrito identifica cuidado especial e nao muda a regra geral: **toda reserva nova e toda alteracao de data/horario exigem aprovacao do Responsavel**.

## Salas

| Identificador | Tipo | Capacidade | Restrito | Aprovacao da reserva | Observacoes |
|---|---|---:|---|---|---|
| Sala 101 | Sala generica | 30 pessoas | Nao | Sim, pelo Responsavel | Estado demonstrativo inicial: `DISPONIVEL`. |
| Sala 204 | Sala generica | 24 pessoas | Nao | Sim, pelo Responsavel | Estado demonstrativo inicial: `RESERVADA`. |
| Sala 305 | Sala generica | 40 pessoas | Nao | Sim, pelo Responsavel | Estado demonstrativo inicial: `EM_MANUTENCAO`; nao aceita novas reservas enquanto nesse estado. |
| Sala 408 | Sala generica | 22 pessoas | Nao | Sim, pelo Responsavel | Estado demonstrativo inicial: `RESERVADA`. |

Capacidades e estados foram retirados dos dados de demonstracao existentes no frontend. Localizacao e outros atributos nao estao definidos.

## Docentes

Docentes sao recursos de agenda: a aplicacao deve impedir conflito de horario e permitir ao Responsavel validar sua alocacao. Os nomes abaixo sao ficticios e reutilizam identidades ja presentes nos dados demonstrativos.

| Identificacao | Tipo | Restrito | Aprovacao da reserva/alocacao | Observacoes |
|---|---|---|---|---|
| Camila Santos | Docente demonstrativa | Nao | Sim, pelo Responsavel | Identidade ficticia ja usada como Solicitante. |
| Marina Alves | Docente demonstrativa | Nao | Sim, pelo Responsavel | Identidade ficticia ja usada em uma solicitacao de demonstracao. |

Nao foram informadas competencias ou disponibilidade recorrente; nao devem ser presumidas.

## Materiais e equipamentos

Os itens abaixo sao exemplos ficticios adicionados para demonstrar cadastro, aprovacao e fluxo de retirada/devolucao exigidos pelo projeto.

| Item | Tipo | Restrito | Aprovacao da reserva | Retirada/devolucao |
|---|---|---|---|---|
| Projetor multimidia | Equipamento audiovisual | Sim | Sim, pelo Responsavel | Registrar retirada e devolucao. |
| Notebook | Equipamento de informatica | Sim | Sim, pelo Responsavel | Registrar retirada e devolucao. |
| Kit de cabos e adaptadores | Material de apoio | Nao | Sim, pelo Responsavel | Registrar retirada e devolucao quando associado a uma reserva. |

## Regras aplicaveis ao inventario

- Toda reserva, incluindo reserva que contenha apenas recursos nao restritos, inicia como `SOLICITADA` e depende de aprovacao do Responsavel.
- Uma reserva que combine varios recursos precisa ter todos disponiveis. A aprovacao confirma o conjunto de forma consistente; conflito em qualquer sala, docente ou material impede a aprovacao.
- Solicitacoes pendentes podem coincidir. Na aprovacao, disponibilidade e conflitos devem ser revalidados; no maximo uma reserva conflitante pode ser aprovada.
- Alteracao de data/horario exige aprovacao do Responsavel para qualquer recurso. A reserva original permanece valida enquanto a alteracao aguarda decisao.
- Cancelamento pelo Solicitante nao exige aprovacao, mas e permitido apenas ate duas horas antes do inicio e deve gerar auditoria com usuario e data/hora.
- Reserva nova nao decidida ate o inicio expira automaticamente, libera os recursos e gera auditoria. Alteracao pendente expira no inicio original e preserva a reserva original.
- O estado de manutencao e independente da classificacao de restricao. Manutencao bloqueia novas reservas e preserva registros existentes, com alerta ao Responsavel.

## Validacao pendente

Confirmar com equipe/docente se os dados ficticios e os exemplos de materiais sao suficientes para a demonstracao/entrega. Se forem usados recursos reais, refazer o inventario e avaliar riscos de uso (safety) com base nos itens reais.

## Referencias

- [PRD principal](../prd.md)
- [Requisitos funcionais](requisitos-funcionais.md)
- [Especificacao do projeto da disciplina](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html)
