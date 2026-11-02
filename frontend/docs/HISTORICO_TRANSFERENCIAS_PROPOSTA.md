# Histórico de transferências

Status: autorizado em 02/11/2026 e implementado no worktree da branch `back`,
com integração frontend na `front`. Este documento registra o contrato aprovado;
a especificação OpenAPI gerada é a referência efetiva.

## Diagnóstico

`TransferenciaController` oferece somente `POST /transferencias`.
`GET /contas/{id}/transacoes` devolve cada ponta separadamente, com
`transferenciaId`, sem identificar a outra conta. Reconstruir todo o histórico
no Angular exigiria percorrer todas as páginas de todas as contas e juntar os
pares. A proposta abaixo permite consultar somente transferências, com paginação
na API e uma linha por operação.

## Contrato implementado

`GET /transferencias?page=0&size=5`

- JWT obrigatório; somente transferências do usuário autenticado.
- `contaId` opcional: inclui entradas e saídas dessa conta. Ausente: todas as contas.
- Sem corte automático de período: todas as transferências ficam acessíveis pelas páginas.
- Ordenação fixa por data decrescente e ID da transação de saída decrescente para desempate.
- Envelope `PaginaResponse` existente: `conteudo`, `pagina`, `tamanho`,
  `totalElementos`, `totalPaginas`.
- Cada item: `transferenciaId`, `contaOrigemId`, `contaOrigemNome`,
  `contaDestinoId`, `contaDestinoNome`, `descricao`, `valor`, `data`.
- Valor positivo recebido da API, sem cálculo de saldo no cliente.
- Conta inexistente ou de outro dono: 404 com a mesma mensagem.
- Parâmetros inválidos seguem o tratamento e os limites de paginação do projeto.

## Implementação backend

- Acrescentar GET em `TransferenciaController`, preservando o POST.
- Criar serviço de consulta e DTO próprios para o histórico.
- Acrescentar consulta e contagem em `TransacaoRepository`, relacionando saída
  e entrada pelo UUID e restringindo ambas as contas ao dono autenticado.
- Paginar o conjunto de operações, sem duplicar saída e entrada.
- Sem nova tabela ou migration prevista: os pares já estão persistidos.
- Preservar regras de transferência, auditoria de escrita, saldos e estornos.
- Implementar na branch `back`, em seu worktree separado, preservando as
  alterações frontend ainda sem commit na `front`.

## Interface

- Trocar “Última transferência” por “Histórico de transferências”.
- Preservar o padrão visual e mostrar origem → destino, data, descrição e valor.
- Carregar o histórico ao entrar, com filtro de conta independente do formulário.
- Oferecer anterior/próxima e total real; todas as páginas permanecem acessíveis.
- Estados de carregamento, vazio e erro com nova tentativa.
- Após uma transferência confirmada, consultar novamente o histórico e os saldos.
- Não usar memória da sessão ou dados fictícios como fonte do histórico.

## Verificações previstas

- Autenticação e isolamento entre usuários, inclusive filtro com conta alheia.
- Direção correta, uma linha por UUID, total correto e paginação com desempate estável.
- Transferências antigas e dos dois sentidos; excluir receitas, despesas e estornos.
- Usuário sem operações e parâmetros de paginação inválidos.
- Regressão do POST, domínio financeiro, frontend, erros e atualização após envio.
- Build, OpenAPI e avaliação visual responsiva.

## Validação da entrega

- Backend: 135 testes aprovados, sem falhas ou testes ignorados, incluindo PostgreSQL
  em Testcontainers, isolamento por dono, direção, contagem e ordenação.
- Teste de eficiência: duas consultas para uma página cheia sem filtro
  (dados + contagem), sem N+1.
- Frontend: 223 testes aprovados com dois workers. A execução anterior com
  concorrência padrão teve timeouts de inicialização durante o build Docker;
  nenhum timeout foi aumentado e nenhum teste foi ignorado.
- Build de produção aprovado; permanecem avisos do limite de tamanho de CSS.
- Navegador com API simulada apenas no teste: cinco larguras, sem rolagem horizontal;
  paginação, operação de 2020, filtro, vazio, erro/nova tentativa e reabertura.
- API local reconstruída a partir da branch `back`: saúde UP, GET e POST presentes
  na OpenAPI; GET sem token retorna 401. Banco e volume preservados.
- Nenhuma migration ou alteração nas regras financeiras.

## Arquivos desta entrega

Backend, no worktree da branch `back`:

- `README.md`
- `backend/api/src/main/java/com/controlefinanceiro/api/application/HistoricoTransferenciasService.java`
- `backend/api/src/main/java/com/controlefinanceiro/api/infrastructure/TransacaoRepository.java`
- `backend/api/src/main/java/com/controlefinanceiro/api/presentation/controller/TransferenciaController.java`
- `backend/api/src/main/java/com/controlefinanceiro/api/presentation/dto/HistoricoTransferenciaResponse.java`
- `backend/api/src/test/java/com/controlefinanceiro/api/application/HistoricoTransferenciasServiceTest.java`
- `backend/api/src/test/java/com/controlefinanceiro/api/presentation/TransferenciaApiTest.java`
- `backend/api/src/test/java/com/controlefinanceiro/api/presentation/DocumentacaoTest.java`

Frontend, na branch `front`:

- `frontend/src/app/features/transferencias/transferencia.models.ts`
- `frontend/src/app/features/transferencias/transferencias.service.ts`
- `frontend/src/app/features/transferencias/transferencias.service.spec.ts`
- `frontend/src/app/features/transferencias/transferencias.ts`
- `frontend/src/app/features/transferencias/transferencias.html`
- `frontend/src/app/features/transferencias/transferencias.scss`
- `frontend/src/app/features/transferencias/transferencias.spec.ts`
- `frontend/README.md`
- `frontend/docs/HISTORICO_TRANSFERENCIAS_PROPOSTA.md`

As alterações visuais anteriores e a validação monetária compartilhada foram preservadas.
