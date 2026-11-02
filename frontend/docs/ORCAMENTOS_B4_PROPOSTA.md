# B4 — Orçamentos mensais por categoria

Status: autorizado em 02/11/2026 e implementado no worktree da branch `back`,
com frontend na `front`. Este documento registra a proposta aprovada e o contrato entregue.

## Estado confirmado antes da implementação

- A rota Angular `/orcamentos` carrega `EmConstrucao`.
- Não há entidade, tabela, repositório ou endpoint de orçamento no backend,
  inclusive no worktree da branch `back`.
- O guia de continuidade e o escopo frontend exigem extensão B4 aprovada,
  persistência própria e consumo calculado no servidor.
- A autorização anterior de backend foi para o histórico de transferências;
  esta proposta trata de outra funcionalidade.

## Resultado visual pretendido

- Mesma escala, largura de conteúdo e sidebar das telas aprovadas.
- Título “Orçamentos” e subtítulo da referência.
- Botão “Novo orçamento”, com formulário acessível para categoria de despesa,
  mês e limite, usando o conversor monetário compartilhado.
- Três indicadores: orçamentos ativos no mês, dentro do planejado e acima do limite.
- Card “Meus orçamentos” com seletor de mês, tabela de categoria, período,
  limite, gasto, progresso, situação e edição do limite.
- Paginação, carregamento, vazio, falha e nova tentativa; layout responsivo.
- Ícones apenas decorativos; valores e indicadores fornecidos pela API.

## Contratos implementados

- `POST /orcamentos`: `{categoriaId, mes: "AAAA-MM", limite}`; 201 com o recurso criado.
- `PATCH /orcamentos/{id}`: `{limite}`; edita somente o limite; 200.
- `GET /orcamentos?mes=AAAA-MM&page=0&size=10`: lista o mês explicitamente informado,
  com paginação limitada aos padrões do projeto e ordenação por nome da categoria e ID.
- JWT obrigatório; dono sempre extraído do token, nunca aceito no corpo.
- Item de resposta: `id`, `categoriaId`, `categoriaNome`, `mes`, `limite`,
  `gasto`, `restante`, `percentualConsumido` e `situacao`.
- GET preserva os campos usuais de página (`conteudo`, `pagina`, `tamanho`,
  `totalElementos`, `totalPaginas`) e acrescenta `resumo` com
  `ativos`, `dentroDoPlanejado` e `acimaDoLimite` para TODO o mês consultado,
  independentemente da página exibida.
- Nenhuma rota DELETE proposta nesta etapa. O ícone de lixeira da imagem não
  será transformado em ação sem uma decisão funcional específica.

## Regras e semântica propostas

- Um orçamento por usuário, categoria de despesa e mês, com unicidade no banco.
- Categoria obrigatoriamente do usuário autenticado e do tipo DESPESA.
- Limite estritamente positivo e com no máximo duas casas decimais, validado
  no domínio com os tipos monetários existentes.
- Categoria e mês não são alterados pelo PATCH; eles identificam o planejamento.
- Gasto: despesas elegíveis daquela categoria e usuário durante o mês civil,
  considerando todas as contas. Transferências, estornos e despesas originais
  estornadas ficam fora, seguindo a semântica dos resumos existentes.
- Um estorno posterior ao mês também exclui a despesa original, conforme a
  regra atual dos resumos; não introduzir outra interpretação contábil.
- Restante e percentual são calculados no backend usando BigDecimal. O saldo
  restante pode ser negativo quando houver excesso. O percentual não é limitado
  a 100%; apenas a representação visual da barra ocupa no máximo sua largura.
- `DENTRO_DO_LIMITE`: gasto abaixo de 80%; `ATENCAO`: de 80% até 100%, inclusive;
  `ACIMA_DO_LIMITE`: gasto maior que o limite.
- Comparar gasto e limite diretamente em BigDecimal para decidir a situação,
  antes de arredondar o percentual de exibição.
- “Dentro do planejado” conta orçamentos que não excederam o limite, inclusive
  os em atenção. “Acima do limite” conta os que o excederam. “Ativos” significa
  cadastrados para o mês selecionado, não implica um novo estado de ativação.
- Criar ou editar orçamento não gera lançamento, débito, crédito nem mudança de saldo.
- Conflitos de unicidade: 409; recurso/categoria inexistente ou alheio: mesmo 404;
  demais falhas seguem o tratamento global e as validações existentes.

## Impacto técnico

- Domain: agregado `Orcamento` com invariantes próprias, sem modificar regras de Conta.
- Application: casos de uso de criação, edição e consulta, com verificação do dono,
  agregação do consumo e transações nos padrões atuais.
- Infrastructure: repositório restrito por dono e consultas paginadas/agregadas,
  sem consultas adicionais por linha nem carregamento integral do histórico.
- Presentation: controller e DTOs dedicados, validação de entrada e OpenAPI.
- Flyway: uma migration para a tabela necessária de orçamentos, FKs, restrições
  e índices coerentes. Nenhuma tabela de consumo/saldo derivado.
- Frontend: feature standalone com Signals, OnPush, Reactive Forms e serviço próprio;
  integração somente com o contrato efetivamente implementado.
- Backend no worktree da `back`; frontend na `front`, preservando alterações anteriores.

## Divergências da ilustração

- A imagem mostra 80% em verde. O requisito funcional exige alerta desde 80%; usar
  destaque de atenção nessa situação sem alterar o restante do estilo.
- Minigráficos dos indicadores não têm série definida no B4 e não serão simulados.
- Não incluir exclusão só porque há uma lixeira no exemplo visual.

## Validações previstas

- Domínio: limite válido, tipo/dono da categoria e unicidade por mês.
- Integração PostgreSQL: persistência, edição, concorrência da unicidade,
  paginação, resumo independente da página e consultas eficientes.
- Consumo com múltiplas contas; despesas fora do mês; transferências e estornos;
  pontos de fronteira de 80% e 100%; ausência de movimentações.
- BOLA em consulta, criação e edição; contrato uniforme de erros e autenticação.
- Regressão de saldos, movimentações, transferências e resumos existentes.
- Frontend: formulários, moeda brasileira, filtro mensal, estados e paginação;
  build de produção e avaliação visual em desktop e celular.

## Alternativa sem extensão backend

Manter a área identificada como pendente. Um layout com números de exemplo,
consumo calculado localmente ou controles sem ação não atende ao escopo funcional.

## Entrega e verificações

- Backend na branch `back`: agregado Orcamento, Value Object ConsumoOrcamento,
  serviço de aplicação, repositório, controller, DTOs, auditoria e migration V8.
- Frontend na `front`: feature `features/orcamentos/`, rota, integração visual
  do shell, formulário de criação/edição e testes.
- 148 testes backend aprovados, sem falhas ou testes ignorados, com PostgreSQL
  em Testcontainers. Incluem concorrência de criação, BOLA, saldos preservados,
  estornos, fronteiras de percentual e paginação com resumo do mês inteiro.
- Listagem de página cheia: três consultas fixas (dados, contagem, resumo), sem N+1.
- 234 testes frontend aprovados, com dois workers; build de produção aprovado,
  com avisos de tamanho de CSS, sem ampliar os budgets.
- Validação visual em navegador com dados isolados de teste em seis larguras;
  sem rolagem horizontal da página. A tabela mantém rolagem própria em telas estreitas.
- Os fluxos de criação e edição foram verificados com valor brasileiro e payloads
  numéricos; categoria e mês ficam fixos ao editar. Erro e vazio também verificados.
- API local atualizada a partir do worktree `back`, com banco e volume preservados.
  Flyway confirmou a aplicação da V8. Saúde UP; GET/POST/PATCH presentes na OpenAPI;
  consulta sem token retorna 401. A avaliação visual usou dados isolados de teste,
  sem inserir orçamentos fictícios na aplicação ou no banco local.

### Arquivos principais

Backend (caminhos relativos ao worktree `back`, dentro de `backend/api/`):

- `src/main/java/com/controlefinanceiro/api/domain/Orcamento.java`
- `src/main/java/com/controlefinanceiro/api/domain/ConsumoOrcamento.java`
- `src/main/java/com/controlefinanceiro/api/domain/AcaoAuditoria.java`
- `src/main/java/com/controlefinanceiro/api/domain/EntidadeAuditada.java`
- `src/main/java/com/controlefinanceiro/api/application/OrcamentoService.java`
- `src/main/java/com/controlefinanceiro/api/infrastructure/OrcamentoRepository.java`
- `src/main/java/com/controlefinanceiro/api/presentation/controller/OrcamentoController.java`
- `src/main/java/com/controlefinanceiro/api/presentation/dto/CriarOrcamentoRequest.java`
- `src/main/java/com/controlefinanceiro/api/presentation/dto/EditarOrcamentoRequest.java`
- `src/main/java/com/controlefinanceiro/api/presentation/dto/OrcamentoResponse.java`
- `src/main/java/com/controlefinanceiro/api/presentation/dto/OrcamentosResponse.java`
- `src/main/java/com/controlefinanceiro/api/presentation/handler/GlobalExceptionHandler.java`
- `src/main/resources/db/migration/V8__orcamentos.sql`
- `src/test/java/com/controlefinanceiro/api/domain/OrcamentoTest.java`
- `src/test/java/com/controlefinanceiro/api/presentation/OrcamentoApiTest.java`
- `src/test/java/com/controlefinanceiro/api/presentation/DocumentacaoTest.java`
- `src/test/java/com/controlefinanceiro/api/ContextoIntegracaoTest.java`
- Documentação em `README.md` na raiz do worktree.

Frontend:

- `frontend/src/app/features/orcamentos/`: modelos, serviço, tela, formulário e testes.
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/layout/shell/shell.html`
- `frontend/src/app/layout/shell/shell.scss`
- `frontend/README.md`
- Este relatório.
