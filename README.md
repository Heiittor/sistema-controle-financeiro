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

- **111 testes**, entre unitários e de integração com PostgreSQL real via Testcontainers.
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

## Limitações e próximos passos

Sem refresh token ou revogação individual de JWT; rate limit limitado a uma instância; sem roles; transferências restritas a contas do mesmo usuário; imutabilidade da auditoria garantida no código (no banco exigiria permissões ou trigger); algumas dependências transitivas têm CVEs sem correção publicada na versão atual do Spring Boot (monitoradas via Dependabot).

Próximas etapas: frontend na branch `front`.