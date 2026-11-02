# Frontend — Sistema de Controle Financeiro

Interface Angular para cadastro, autenticação e operações financeiras pessoais.
Os saldos, a autorização e os cálculos financeiros são responsabilidade da API.

## Pré-requisitos

- Node compatível com `^22.22.3 || ^24.15.0 || >=26.0.0`. Na linha 24, use pelo menos `24.15.0`.
- npm (o `package.json` declara `11.19.0`). Use o lockfile existente para instalar as dependências.
- API em execução. Consulte o [README da raiz](../README.md) para preparar o backend e o PostgreSQL com Docker.

A IDE pode priorizar seu runtime interno. Confira `node --version` no terminal usado para rodar o Angular.
No Windows, se a instalação compatível estiver em `C:\Program Files\nodejs`, selecione-a para a sessão atual:

```powershell
$env:PATH = 'C:\Program Files\nodejs;' + $env:PATH
node --version
```

## Executar localmente

Na pasta `frontend`:

```sh
npm ci
npm start
```

Abra `http://localhost:4200`. A API local usa `http://localhost:8080` e publica o contrato em `/v3/api-docs` e o Swagger em `/swagger-ui/index.html`.
Cadastre um usuário pela tela de cadastro, entre e crie contas e categorias para registrar movimentações.

As URLs da API ficam em `src/environments/environment.development.ts` e `src/environments/environment.ts`.
A configuração de produção ainda usa localhost e precisa ser ajustada ao destino antes de publicar.
Não coloque segredos nesses arquivos: eles são incluídos no JavaScript entregue ao navegador.
O backend deve permitir a origem do frontend no CORS; a configuração local já contempla a porta 4200.

## Funcionalidades e limites atuais

| Área | Estado |
|---|---|
| Cadastro, login e logout | Implementados, com tratamento de sessão expirada e limite de tentativas |
| Contas e categorias | Listagem, criação e filtro de categorias por tipo |
| Movimentações | Receitas, despesas, histórico por conta e período, paginação, edição de descrição/categoria e estorno |
| Transferências | Envio único, saldos recarregados pela API e histórico persistido paginado, com filtro de conta e direção origem → destino |
| Navegação | Sidebar recolhível no desktop, menu móvel, título por rota e atalho para o conteúdo |
| Dashboard | Indicadores B3, série dos últimos seis meses, despesas por categoria, filtro mensal, alternativas textuais dos gráficos e acessos rápidos para receita, despesa, transferência e orçamentos |
| Orçamentos | B4 integrado: mês, criação e edição do limite, consumo e resumo calculados pela API, alertas de 80%/100% e paginação |
| Resumo por conta | Indicadores em Movimentações, com conta e período aplicados, recebidos da API |

O [contrato B3](docs/DASHBOARD_B3_PROPOSTA.md) foi implementado e integrado pelo PR #20.
Execute a versão atual do backend para disponibilizar os endpoints do Dashboard.
Os gráficos usam Chart.js, carregado junto da rota; as instâncias são destruídas ao trocar dados ou sair da tela.

## Sessão e segurança

A sessão armazena apenas token e vencimento em `sessionStorage`, para sobreviver à atualização da página.
Esse armazenamento é acessível ao JavaScript e não protege contra XSS. Não registre tokens ou senhas em logs.
Não há refresh token: após expiração ou `401` de uma rota protegida, o usuário precisa entrar novamente.
O `401` do login permanece um erro de credenciais. O `429` respeita `Retry-After` sem retentativas automáticas.
O guard orienta a navegação; a autorização real é aplicada pelo backend a cada requisição.

## Valores e datas

O frontend só envia um valor quando sua serialização JSON preserva o decimal digitado.
Valores que perderiam centavos ao converter para `number` são bloqueados com mensagem no formulário.
Não há cálculo local de saldos, resumo consolidado ou consumo de orçamento.
Datas de negócio usam `AAAA-MM-DD`; o mês padrão segue `America/Sao_Paulo`.
Depois de uma mutação, as listas são consultadas novamente na API.

## Testes e build

Na pasta `frontend`:

```sh
npm test -- --no-watch
npm run build
```

Os testes usam Vitest, jsdom e `HttpTestingController`. Para executar somente uma área:

```sh
npm test -- --no-watch --include=src/app/features/categorias/categorias.spec.ts
```

O build de produção é gerado em `dist/frontend`. A CI executa instalação pelo lockfile, testes e build do frontend, além da suíte backend.
Ainda não há suíte E2E nem ESLint configurado. Testes em jsdom não comprovam layout, contraste ou integração real no navegador.

## Conferência manual antes da entrega

- Verificar menu aberto/recolhido em desktop e menu sobreposto em celular (por exemplo, 1440, 900 e 360 px), sem transbordamento horizontal.
- Navegar com Tab, usar “Pular para o conteúdo”, abrir/fechar menu e diálogos, verificar foco e leitura dos rótulos.
- Confirmar cadastro, login inválido, expiração, logout e `429`/`Retry-After` com a API real em ambiente de teste.
- Criar contas/categorias, registrar receita/despesa, editar, estornar e transferir; confirmar a atualização a partir da API.
- Conferir carregamento, erro, lista vazia, filtros e paginação, inclusive com respostas lentas.
- Conferir ícones e fontes, que são carregados do Google Fonts, e possíveis erros no console.

## Organização

- `core`: API, autenticação, interceptors e sessão.
- `layout`: shell e navegação.
- `features`: componentes e serviços por funcionalidade.
- `shared`: formulários, datas, moeda, estilos e componentes comuns.

O projeto usa componentes standalone, Signals, `OnPush` e Reactive Forms. Preserve os contratos reais da API e o fluxo de trabalho documentado: alterações backend exigem aprovação prévia.
