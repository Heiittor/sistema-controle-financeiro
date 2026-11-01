# B3 — Proposta de contrato do Dashboard

Status: aprovado e implementado. Integrado pelo PR #20 (commit de integração 39ff81a).
O texto abaixo registra o contrato que orientou a implementação; a OpenAPI gerada é a referência efetiva.
Prioridade: obrigatória para entregar o Dashboard previsto no escopo.

## Objetivo e alternativa

Fornecer indicadores consolidados e uma série mensal de todas as contas do usuário autenticado.
O resumo existente é por conta. Agregá-lo no Angular violaria a decisão de manter os cálculos financeiros na API.
A alternativa sem extensão backend é manter o Dashboard pendente e, separadamente, implementar uma tela de resumo por conta.

## Contratos propostos

### GET /dashboard/resumo?inicio=2026-10-01&fim=2026-10-31

Parâmetros obrigatórios em formato AAAA-MM-DD. Intervalo inclusivo; início posterior ao fim retorna 422.
Resposta 200:

```json
{
  "inicio": "2026-10-01",
  "fim": "2026-10-31",
  "saldoTotal": 1200.00,
  "totalReceitas": 2000.00,
  "totalDespesas": 800.00,
  "saldoDoPeriodo": 1200.00,
  "porCategoria": [
    { "categoriaId": 7, "categoriaNome": "Alimentação", "tipo": "DESPESA", "total": 800.00 }
  ]
}
```

Exemplo ilustrativo, não são dados reais nem fixtures da aplicação.
`saldoTotal` é a soma dos saldos ATUAIS das contas, independente do filtro; não é saldo histórico na data final.
Receitas, despesas e saldo do período seguem o intervalo informado. `porCategoria` inclui apenas despesas, para a rosca.
Totais sem movimento são zero; `porCategoria` é uma lista vazia quando não há despesas elegíveis.

### GET /dashboard/serie-mensal?inicio=2026-05-01&fim=2026-10-31

Parâmetros obrigatórios. Retorna lista em ordem crescente:

```json
[
  { "mes": "2026-05", "receitas": 0.00, "despesas": 0.00 },
  { "mes": "2026-06", "receitas": 500.00, "despesas": 100.00 }
]
```

Exemplo abreviado: a resposta real inclui TODOS os meses do intervalo, inclusive meses com zero.
As consultas respeitam exatamente as datas inicial/final: meses nas extremidades podem ser parciais.
Proposta de janela inicial da interface: seis meses completos, terminando no mês selecionado.
Limite proposto para os dois endpoints: 24 meses civis abrangidos, inclusive as extremidades; excesso retorna 422.

## Regras preservadas

- Autenticação JWT e isolamento por dono em todas as consultas.
- BigDecimal e agregações no banco. O Angular apenas apresenta os resultados.
- Excluir transferências, estornos e originais já estornadas dos totais e da série, seguindo o resumo atual.
- O estorno exclui a original mesmo quando ocorreu fora do período consultado, conforme a semântica existente.
- `saldoTotal` usa o saldo persistido das contas; não deriva do histórico filtrado.
- Erros no formato existente; 400 para parâmetros ausentes/malformados e 401 para autenticação inválida.

## Arquivos e impacto previstos

- Criar `DashboardController`, `DashboardService` e DTOs de resposta no padrão atual.
- Acrescentar consultas agregadas restritas por usuário em `ContaRepository` e `TransacaoRepository`.
- Acrescentar testes de integração, isolamento entre usuários (BOLA), contratos e documentação OpenAPI.
- Não requer nova tabela ou migration: consulta dados existentes.
- Não altera entidades, saldo, transferências, estornos ou endpoints existentes.
- Orçamentos (B4) são uma aprovação separada, com persistência própria.

## Critérios de aceite

- Duas contas do mesmo dono consolidadas corretamente; dados de outro usuário ausentes.
- Usuário sem contas/movimentações recebe zeros e lista de categorias vazia.
- Série ordenada, sem lacunas, inclusive virada de ano e meses sem movimento.
- Períodos parciais, inválidos e acima do limite cobertos.
- Transferências, estornos e originais estornadas respeitam as regras atuais.
- Suíte backend e contratos existentes continuam passando; novos endpoints aparecem na OpenAPI.

## Execução após aprovação

O guia exige trabalho backend na branch `back`. O frontend tem alterações locais ainda sem commit.
Antes da implementação, combinar um checkout separado para o backend, preservando integralmente esse trabalho.
Não fazer troca de branch no workspace atual, commit, merge ou push automaticamente.
