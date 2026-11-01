# Complemento B3 para fidelidade integral à referência do painel

Status: proposta, não implementada nem autorizada.

O frontend reproduz a composição visual com os dados atuais. Faltam contratos para
as minisséries de saldo/economia e para as quatro comparações com o mês anterior.
Nome de usuário, Relatórios e Configurações continuam fora do contrato e não são
simulados. A lista recente utiliza os históricos existentes, com concorrência limitada.

## Extensão mínima proposta

- Acrescentar à série mensal os campos `saldoTotalNoFimDoMes` e `saldoDoPeriodo`.
- Acrescentar ao resumo um objeto `comparacaoMesAnterior` com dados para cada
  indicador: base anterior e percentual de variação, calculados no backend.
- Quando a base anterior for zero, percentual deve ser nulo e a interface deve
  informar ausência de base comparável, em vez de inventar uma porcentagem.
- As comparações devem usar o mesmo critério de datas do mês selecionado e o
  mês civil anterior. Valores futuros devem manter a semântica já definida pela API.

## Decisão financeira que precisa ser aprovada

O saldo do card atual é o saldo persistido, independente do filtro. Para a minissérie,
a proposta é apresentar o saldo histórico ao fim de cada mês, respeitando datas
civis dos lançamentos e incluindo os efeitos de transferências e estornos.
O tooltip deve dizer que a série é histórica e o card é atual.

Economia significa resultado do período e pode ser negativa. A série e a comparação
devem seguir o resumo atual, que exclui transferências, estornos e originais estornadas.
Não é correto calcular essa série ou o percentual de variação financeira no Angular.

## Impacto técnico

- DashboardService, DTOs B3 e consultas agregadas em TransacaoRepository/ContaRepository.
- Sem alteração de entidades, saldo persistido, escrita financeira ou migrations.
- Segurança por dono, BigDecimal, documentação OpenAPI e testes de integração/BOLA.
- Testar base anterior zero, resultado negativo, virada de ano, datas futuras e estornos
  fora do mês original. Preservar respostas e campos atuais.
- Implementação somente na back, após aprovação explícita; integração via PR/main/front.

Até essa aprovação, não inserir curvas de saldo/economia ou variações fictícias.
