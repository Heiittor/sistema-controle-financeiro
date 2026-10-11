# Sistema de Controle Financeiro

API REST de controle financeiro pessoal (portfólio), desenvolvida com foco em backend, DDD e segurança.

## Stack

Java 24, Spring Boot 4, Spring MVC, Spring Data JPA/Hibernate, PostgreSQL 16, Flyway, Docker, Maven, JUnit e Testcontainers.

## Funcionalidades atuais

- Cadastro de usuários (e-mail normalizado, senha nunca devolvida na API)
- Contas (saldo protegido pelo domínio, controle de concorrência com `@Version`)
- Categorias por usuário (receita ou despesa)
- Receitas e despesas (atômicas, com saldo e transação gravados juntos)
- Histórico paginado por período
- Erros padronizados (`400`, `404`, `409`, `422`, `500`)

## Aviso importante: autenticação provisória

**Ainda não há login.** O usuário é identificado pelo cabeçalho `X-Usuario-Id`, que permite se passar por qualquer usuário. É uma ponte **temporária** até o Spring Security + JWT (Fase 7). **Não use em produção.**

## Como executar

Requisitos: JDK 24 e Docker Desktop.

```bash
docker compose up -d
cd backend/api
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. O banco fica na porta `5433`, e o Flyway cria as tabelas sozinho.

## Testes

Com o Docker aberto (os testes de integração usam PostgreSQL real via Testcontainers):

```bash
cd backend/api
./mvnw test
```

Inclui testes de domínio, fluxo financeiro e de BOLA (um usuário não acessa dados de outro).

## Arquitetura

Camadas com DDD: `presentation` (controllers e DTOs) → `application` (services) → `domain` (entidades, Value Object `Dinheiro`, exceções) → `infrastructure` (repositories). O domínio não conhece as camadas externas.

## Roadmap

Spring Security + JWT, transferências entre contas, Swagger/OpenAPI, CI com GitHub Actions, Docker multi-stage.