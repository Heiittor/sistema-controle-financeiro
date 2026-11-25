# Sistema de Controle Financeiro

Um sistema para organizar as finanças pessoais: cadastrar contas e categorias,
registrar receitas e despesas, fazer transferências e acompanhar o dinheiro pelo
dashboard e pelos orçamentos mensais. Cada usuário acessa apenas os próprios dados.
Os lançamentos mantêm o histórico; correções podem ser feitas por edição de descrição
e categoria ou por estorno.

O frontend usa Angular e a API usa Java 24, Spring Boot e PostgreSQL, com autenticação
JWT. As decisões de arquitetura estão em [DECISOES.md](DECISOES.md).

## Rodar localmente

Você precisa do Docker Desktop em execução e do Node.js 24.15 ou superior na linha 24.

Na raiz do projeto, copie o arquivo de configuração:

```sh
cp .env.example .env
```

Edite o `.env` com o usuário e a senha do banco e um `JWT_SECRET` de pelo menos
32 caracteres. Depois, inicie a API e o banco:

```sh
docker compose up -d --build
```

Em outro terminal, inicie a interface:

```sh
cd frontend
npm ci
npm start
```

Abra [localhost:4200](http://localhost:4200), crie seu usuário e entre no sistema.
A API fica em [localhost:8080](http://localhost:8080), e os endpoints estão no
[Swagger](http://localhost:8080/swagger-ui/index.html).

## Verificar o projeto

No frontend, execute `npm run lint`, `npm test -- --no-watch` e `npm run build`.
Para testar o backend, use `./mvnw test` em `backend/api`, com JDK 24 e Docker ativos
(`mvnw.cmd test` no Windows). Os testes de integração usam um PostgreSQL isolado.
A CI executa as verificações de frontend e backend no GitHub.

Mais detalhes estão no [README do frontend](frontend/README.md) e no
[registro de validação](frontend/docs/FECHAMENTO_VALIDACAO.md).
A revisão visual das telas ficou para uma próxima etapa. Para publicar online,
ainda é preciso configurar a URL da API, o CORS e a hospedagem.
