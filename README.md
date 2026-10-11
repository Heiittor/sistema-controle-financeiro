# Sistema de Controle Financeiro

API REST de controle financeiro pessoal, desenvolvida como projeto de portfólio com foco em backend, DDD, segurança e testes.

## Stack

Java 24, Spring Boot 4, Spring MVC, Spring Data JPA/Hibernate, Spring Security (JWT), PostgreSQL 16, Flyway, Docker, Maven, JUnit 5 e Testcontainers.

## Funcionalidades

- Cadastro de usuários (e-mail normalizado, senha com hash BCrypt, senha nunca devolvida pela API)
- Login com JWT (stateless, HS256, validade de 60 minutos)
- Contas (saldo protegido pelo domínio, controle de concorrência com `@Version`)
- Categorias por usuário (receita ou despesa)
- Receitas e despesas (atômicas: saldo e transação são gravados juntos ou nenhum dos dois)
- Histórico de transações paginado, com filtro por período
- Erros padronizados (`400`, `401`, `404`, `409`, `422`, `500`)

## Como executar

Requisitos: JDK 24 e Docker Desktop.

**1. Subir o banco**

```bash
docker compose up -d
```

PostgreSQL na porta `5433` do host. O Flyway cria as tabelas na primeira execução.

**2. Definir o segredo do JWT**

Gere um segredo (PowerShell):

```powershell
$b = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Defina a variável de ambiente:

```powershell
$env:JWT_SECRET="valor-gerado"
```

No IntelliJ: *Edit Configurations* → *Environment variables* → `JWT_SECRET=valor-gerado`.

**3. Rodar a aplicação**

```bash
cd backend/api
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

## Variáveis de ambiente

| Variável | Descrição |
|---|---|
| `JWT_SECRET` | Segredo de assinatura do JWT, mínimo 32 caracteres. Sem valor padrão: a aplicação não sobe sem ele. |

## Autenticação

1. `POST /usuarios` cadastra o usuário
2. `POST /auth/login` devolve `{token, tipo, expiraEmSegundos}`
3. As demais rotas exigem o cabeçalho `Authorization: Bearer <token>`

Login com e-mail inexistente e com senha errada devolve exatamente a mesma resposta (`401`), para não revelar quais e-mails existem.

## Endpoints

| Método | Rota | Descrição | Autenticação |
|---|---|---|---|
| POST | `/usuarios` | Cadastra usuário | Pública |
| POST | `/auth/login` | Autentica e devolve o token | Pública |
| POST | `/contas` | Cria conta (nasce com saldo zero) | Token |
| GET | `/contas` | Lista as contas do usuário | Token |
| POST | `/categorias` | Cria categoria (`RECEITA` ou `DESPESA`) | Token |
| GET | `/categorias?tipo=` | Lista categorias, com filtro opcional por tipo | Token |
| POST | `/contas/{contaId}/receitas` | Registra receita | Token |
| POST | `/contas/{contaId}/despesas` | Registra despesa | Token |
| GET | `/contas/{contaId}/transacoes` | Histórico paginado (`inicio`, `fim`, `page`, `size`) | Token |

Padrões: período padrão do histórico é o mês atual, `size` máximo de 100, ordenação por data decrescente, `page` começa em 0.

## Formato de erro

```json
{
  "status": 422,
  "mensagem": "Saldo insuficiente.",
  "timestamp": "2026-10-11T14:30:00Z",
  "campos": []
}
```

Em erros de validação, `campos` lista cada campo inválido com sua mensagem.

| Status | Quando |
|---|---|
| 400 | Formato inválido (campo vazio, JSON ilegível, parâmetro com tipo errado) |
| 401 | Sem token, token inválido ou expirado, login incorreto |
| 404 | Recurso inexistente **ou de outro usuário** (mesma resposta, de propósito) |
| 409 | Conflito (e-mail, conta ou categoria já existente) |
| 422 | Regra de negócio violada (saldo insuficiente, categoria de tipo errado) |
| 500 | Erro inesperado (mensagem genérica; o detalhe fica só no log) |

## Testes

Com o Docker aberto (os testes de integração usam PostgreSQL real via Testcontainers):

```bash
cd backend/api
./mvnw test
```

A suíte tem 39 testes:

- **Domínio:** `Dinheiro` e `Conta` (regras de valor e saldo)
- **Integração:** cadastro, fluxo financeiro (saldo, atomicidade, histórico paginado)
- **Segurança:** token ausente, inválido e adulterado; login; hash de senha
- **BOLA:** um usuário não lê nem altera dados de outro

Os testes não usam o banco de desenvolvimento: sobem um PostgreSQL descartável. O segredo do JWT nos testes é um valor falso e fixo.

## Arquitetura

Camadas com DDD, seguindo a regra de dependência (as dependências apontam para dentro):

```text
presentation  →  application  →  domain
infrastructure →  domain
```

- `presentation`: controllers, DTOs e tratamento global de erros
- `application`: services (coordenam o fluxo e as transações)
- `domain`: entidades, Value Object `Dinheiro` e exceções de domínio (as regras moram aqui)
- `infrastructure`: repositories e configuração de segurança

## Decisões de design

- **Aggregate:** `Conta` é a raiz. Transações só são criadas por `Conta.registrarReceita/registrarDespesa`, então o saldo nunca diverge das transações.
- **Dinheiro:** Value Object imutável sobre `BigDecimal`, com 2 casas, que rejeita valores nulos, negativos e com mais de 2 casas.
- **Saldo:** armazenado na conta, protegido por `@Version` (lock otimista) e por `CHECK (saldo >= 0)` no banco como última barreira.
- **Autorização por dono:** os repositories de `Conta`, `Categoria` e `Transacao` não expõem `findById`/`findAll`; toda busca inclui o dono.
- **DTOs de entrada e saída:** a API nunca expõe entidades, então a senha não vaza.
- **Migrations:** Flyway controla o esquema, e `ddl-auto=validate` faz o Hibernate conferir entidade contra tabela.
- **Domínio com JPA:** entidades anotadas com `jakarta.persistence`, sem setters nas regras e com construtor `protected`.

## Banco de dados

Migrations em `backend/api/src/main/resources/db/migration`:

| Versão | Conteúdo |
|---|---|
| V1 | `usuarios` |
| V2 | `categorias` |
|