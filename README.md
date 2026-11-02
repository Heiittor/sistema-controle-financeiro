# Sistema de Controle Financeiro

API REST para controle financeiro pessoal, desenvolvida como projeto de portfólio com foco em **DDD, SOLID, Clean Code, segurança e qualidade de software**.

![CI](https://github.com/Heiittor/sistema-controle-financeiro/actions/workflows/ci.yml/badge.svg)

**Stack:** Java 24 · Spring Boot 4 · Spring Security · JWT · PostgreSQL · JPA/Hibernate · Flyway · Docker · Testcontainers · GitHub Actions · OpenAPI

## Arquitetura

```text
presentation → application → domain ← infrastructure
```

Arquitetura em camadas com regra de dependência apontando para dentro: o domínio não conhece as camadas externas e concentra as regras de negócio.

- **Presentation:** controllers, DTOs e tratamento padronizado de erros.
- **Application:** coordenação dos casos de uso e das transações.
- **Domain:** entidades, Value Objects e regras de negócio.
- **Infrastructure:** persistência, segurança e integrações técnicas.

## Boas práticas aplicadas

- **DDD:** `Conta` como Aggregate Root (transações só nascem por `registrarReceita`, `registrarDespesa`, `transferirPara` e `estornar`) e `Dinheiro` como Value Object imutável, que não aceita valor negativo nem mais de 2 casas decimais.
- **Modelo rico:** sem setters nas regras; a regra "saldo insuficiente" mora dentro da `Conta`, e não nos controllers ou services.
- **SOLID e Clean Code:** controller traduz HTTP, service coordena, domínio decide, repository persiste; injeção por construtor; regras de segurança centralizadas (busca de conta com dono em um único componente); sem abstrações "por precaução".
- **Consistência financeira:** operações atômicas com `@Transactional`, concorrência controlada com `@Version` e constraints no PostgreSQL como última barreira.
- **Histórico imutável:** transações não são apagadas; correções são feitas por **estorno** (lançamento inverso), e o resumo financeiro mostra o efeito líquido.
- **Auditoria transacional:** ações com efeito são registradas na mesma transação da operação (commit grava os dois, rollback desfaz os dois), sem dados sensíveis.
- **Segurança:** JWT stateless, BCrypt, proteção contra enumeração de usuários e *timing attack* no login, rate limit (`429`), CORS explícito e autorização por dono (BOLA) testada: "não existe" e "é de outro usuário" dão a mesma resposta (`404`).
- **Observabilidade:** perfis `dev`/`prod`, logs de segurança sem dados pessoais, Actuator com `health` público e `metrics` autenticado.
- **API REST:** DTOs separam o contrato das entidades, validação, erros em formato único e documentação OpenAPI/Swagger.

## Testes e integração contínua

- **148 testes**, entre unitários e de integração com PostgreSQL real via Testcontainers.
- Cobrem segurança, autorização entre usuários, transferências, estornos, auditoria e atomicidade.
- GitHub Actions executa a suíte a cada push e Pull Request.

## Como executar

Requisito: Docker Desktop.

```bash
cp .env.example .env
docker compose up -d --build
```

Preencha o `.env` antes (ao menos `DB_USER`, `DB_PASSWORD` e `JWT_SECRET` com 32+ caracteres).

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html

Testes (requerem Docker):

```bash
cd backend/api
./mvnw test
```

## Principais endpoints

| Método | Rota | Acesso |
|---|---|---|
| POST | `/usuarios` | Público |
| POST | `/auth/login` | Público |
| POST/GET | `/contas`, `/categorias` | Autenticado |
| POST | `/contas/{id}/receitas` | Autenticado |
| POST | `/contas/{id}/despesas` | Autenticado |
| GET | `/contas/{id}/transacoes` | Autenticado |
| PATCH | `/contas/{id}/transacoes/{id}` | Autenticado |
| POST | `/contas/{id}/transacoes/{id}/estorno` | Autenticado |
| GET | `/contas/{id}/resumo` | Autenticado |
| POST | `/transferencias` | Autenticado |
| POST/GET | `/orcamentos` | Autenticado |
| PATCH | `/orcamentos/{id}` | Autenticado |
| GET | `/transferencias?contaId&page&size` | Autenticado |
| GET | `/dashboard/resumo?inicio&fim` | Autenticado |
| GET | `/dashboard/serie-mensal?inicio&fim` | Autenticado |

## Orçamentos mensais (B4)

- POST recebe `{categoriaId, mes:"AAAA-MM", limite}`; uma categoria de despesa
  do dono e um limite positivo de até duas casas. Duplicidade por dono/categoria/mês: 409.
- PATCH recebe somente `{limite}`. Categoria e mês permanecem fixos.
- GET exige `mes=AAAA-MM` e aceita `page` e `size` (10 por padrão, máximo 100;
  tamanho não positivo usa 20 e página negativa usa zero).
- Resposta paginada com `resumo` do mês inteiro: `ativos`, `dentroDoPlanejado`,
  `acimaDoLimite`. Cada linha contém limite, gasto, restante, percentual e situação.
- Consumo soma despesas elegíveis de todas as contas do dono. Ignora transferências,
  estornos e originais estornadas, inclusive quando o estorno ocorre fora do mês.
- Abaixo de 80%: DENTRO_DO_LIMITE; de 80% a 100%: ATENCAO; acima de 100%:
  ACIMA_DO_LIMITE. Comparação exata antes do percentual arredondado para exibição.
- Limites não alteram saldos nem geram movimentações. Alterações são auditadas.
- Flyway V8 cria somente a tabela necessária de orçamentos. Não há endpoint DELETE.
- Conta/categoria/orçamento de outro dono não é exposto; consulta e edição passam pelo JWT.

## Histórico de transferências

`GET /transferencias` lista uma operação por UUID, com origem e destino (IDs e nomes),
descrição, valor positivo e data. Inclui somente pares cujas duas contas pertencem ao
usuário do JWT. `contaId` é opcional e considera entradas e saídas; conta alheia ou
inexistente retorna o mesmo 404. Não há corte automático de período.

Retorna `PaginaResponse` (`conteudo`, `pagina`, `tamanho`, `totalElementos`,
`totalPaginas`), ordenado por data e ID da saída decrescentes.
`page` começa em zero e valores negativos usam zero. `size` padrão é 5, máximo
100; valores não positivos usam 20, seguindo a normalização da listagem de transações.
Parâmetros malformados retornam 400. O POST e as regras financeiras permanecem iguais.
A consulta usa o índice existente do UUID e projeção escalar, sem N+1.

## Dashboard consolidado (B3)

Os dois endpoints exigem `inicio` e `fim` no formato `AAAA-MM-DD`, com intervalo
inclusivo de até 24 meses civis. Parâmetros ausentes/malformados retornam 400;
período invertido ou acima do limite retorna 422. Todas as consultas usam o dono do JWT.

- `/dashboard/resumo`: devolve `inicio`, `fim`, `saldoTotal`, `totalReceitas`,
  `totalDespesas`, `saldoDoPeriodo` e `porCategoria` (somente despesas).
  `saldoTotal` representa os saldos **atuais** das contas, independente do período.
- `/dashboard/serie-mensal`: devolve itens `{mes, receitas, despesas}`, em ordem
  crescente, com meses sem movimento preenchidos com zero. As datas exatas são
  respeitadas, inclusive meses parciais nas extremidades.
- Transferências, estornos e originais já estornadas não entram nos totais do período
  nem na série. O estorno exclui a original mesmo quando realizado fora do período consultado.
- As agregações usam `BigDecimal` e consultas ao banco; nenhum cálculo consolidado
  fica a cargo do frontend. Não há nova tabela ou migration.

Exemplo: `GET /dashboard/serie-mensal?inicio=2026-05-01&fim=2026-10-31`.
O contrato completo, schemas e respostas de erro estão na OpenAPI gerada.
Teste direcionado: `./mvnw -Dtest=DashboardTest test`, em `backend/api`, com Docker disponível.

## Limitações e próximos passos

Sem refresh token ou revogação individual de JWT; rate limit limitado a uma instância; sem roles; transferências restritas a contas do mesmo usuário; imutabilidade da auditoria garantida no código (no banco exigiria permissões ou trigger); algumas dependências transitivas têm CVEs sem correção publicada na versão atual do Spring Boot (monitoradas via Dependabot).

O frontend Angular está em [frontend/README.md](frontend/README.md).
Após integrar alterações das branches de trabalho, execute os testes e a CI
na versão reunida antes de publicar uma nova entrega.
