# Validação de fechamento — 07/11/2026

## Estado do escopo

As funcionalidades previstas estão implementadas: autenticação, contas, categorias,
movimentações, edição limitada e estorno, transferências com histórico, Dashboard B3,
acessos rápidos, resumo por conta e orçamentos B4. Os antigos placeholders de Dashboard
e Orçamentos citados no relatório de transição não representam mais o código atual.

O escopo funcional está implementado. A revisão visual final e eventuais retoques
foram adiados pelo usuário em 25/11/2026. O fechamento interno desta remessa está
registrado na atualização ao final deste documento.

## Verificações executadas

| Verificação | Resultado |
|---|---|
| ESLint de TypeScript e templates | Aprovado, com avisos tratados como falhas |
| Testes frontend | 240 aprovados em 40 arquivos |
| Testes backend, com Docker/Testcontainers | 148 aprovados; nenhuma falha, erro ou teste ignorado |
| Build de produção | Aprovado, mantendo os limites de CSS de 4/8 kB |
| Auditoria de dependências da remessa | Zero vulnerabilidades na instalação limpa verificada |
| Revisão automatizada de acessibilidade | 34 cenários iniciais; correções verificadas por retestes dirigidos |
| Reteste final de Dashboard e Movimentações | 11 cenários sem violações detectadas nem erros de JavaScript |
| Navegação por teclado | Atalho para conteúdo, autocomplete, legenda, diálogos, menu móvel e tabela do Dashboard verificados |
| Larguras verificadas na integração | 1440, 900, 360 e 320 px, sem transbordamento horizontal da página |

Os testes unitários completos foram executados após os ajustes de semântica e contraste.
Os últimos refinamentos de foco, rótulos de regiões e cor das etiquetas de receita foram
verificados no navegador e pelo build. A revisão de acessibilidade usou Chrome e
axe-core 4.14.0, com regras WCAG A/AA e boas práticas.

## Integração com API real

Foi usado um projeto Docker Compose temporário separado, com a imagem local existente
da API e PostgreSQL em memória temporária (`tmpfs`). O frontend de validação usou uma
cópia isolada, com URL da API ajustada apenas nessa cópia. O backend não foi alterado.
O banco habitual e as contas do usuário não receberam os dados de teste.

Fluxos confirmados pela interface e por respostas reais da API:

- Cadastro, login válido e inválido, Dashboard vazio e logout com limpeza da sessão.
- Conta com banco selecionado pelo teclado e conta com nome personalizado.
- Categorias de receita e despesa e lançamentos pelos acessos rápidos, incluindo `1.206,35`.
- Indicadores, série de seis meses, legenda selecionável pelo teclado e alternativas textuais.
- Transferência com histórico persistido e saldos consultados novamente na API.
- Criação e edição de orçamento e estados de atenção/excesso em 80%/100%.
- Edição de descrição e estorno, mantendo o histórico e atualizando saldo/consumo pela API.
- Paginação com mais de 20 movimentações e navegação responsiva pelo menu móvel.
- Acesso sem token (`401`), bloqueio ao acessar conta de outro usuário (`404`), sessão expirada
  e limite de tentativas de login (`429` com `Retry-After`).

As verificações funcionais passaram. A análise automática durante esse roteiro identificou
contraste insuficiente em duas etiquetas de receita de Movimentações; a cor foi corrigida
e revalidada posteriormente, incluindo os diálogos de edição e estorno.

## Ajustes desta revisão

- Corrigida a estrutura das listas de descrição dos cartões em Dashboard, Categorias,
  Movimentações e Orçamentos, mantendo os pares de título/valor e a apresentação.
- Corrigida a hierarquia de títulos das seções do Dashboard.
- Identificada semanticamente a área do menu lateral.
- Ajustado o contraste de etiquetas e estados financeiros, reutilizando as cores do projeto.
- Tabelas do Dashboard receberam foco e nomes acessíveis; a rolagem horizontal foi
  confirmada pelo teclado, com contorno visível dentro dos cartões.

## Limites e pendências

- Resultados automáticos sem violações não certificam conformidade integral com WCAG.
  Permanecem recomendadas a revisão humana com leitor de tela e a conferência visual de
  contraste sobre gradientes/imagens, que podem aparecer como resultados inconclusivos.
- A avaliação visual final e eventuais retoques ficam para uma etapa futura,
  conforme solicitado pelo usuário em 25/11/2026.
- A publicação e os resultados da CI devem ser conferidos no GitHub pelo commit
  entregue. O workflow inclui lint, testes e build, além do job backend.
- Não foi introduzida uma suíte E2E permanente; ela continua opcional nos documentos.

Referências: [contraste mínimo WCAG](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html)
e [limites da análise automática do axe-core](https://github.com/dequelabs/axe-core).

Para repetir as verificações locais: `npm run lint`, `npm test -- --no-watch` e
`npm run build` em `frontend`; `./mvnw test` (ou `mvnw.cmd test`) em `backend/api`,
com JDK 24 e Docker disponíveis. Para a revisão no navegador, use o roteiro de
conferência manual do README e dados de uma conta de teste em banco isolado.

## Fechamento interno — 25/11/2026

As alterações locais foram revisadas para integração com a versão já entregue:
extração do menu, dos gráficos e dos acessos rápidos em componentes de apresentação;
estilos compartilhados delimitados pelas classes de host; ajustes semânticos e de
acessibilidade; configuração do ESLint e inclusão do lint na CI. Os contratos da API,
as regras financeiras, as configurações de ambiente e os testes existentes foram
preservados. A revisão visual final continua adiada por solicitação do usuário.

Verificações repetidas nesta data:

| Verificação | Resultado |
|---|---|
| Instalação pelo lockfile (`npm ci`) | Aprovada, sem alterar o manifesto ou o lockfile |
| Árvore de dependências (`npm ls --all`) | Aprovada |
| ESLint de TypeScript e templates | Aprovado |
| Testes frontend | 240 aprovados em 40 arquivos |
| Build de produção | Aprovado, dentro dos limites de tamanho existentes |
| Auditoria npm da instalação | Zero vulnerabilidades reportadas |
| Testes backend com Docker/Testcontainers | 148 aprovados, sem falhas, erros ou testes ignorados |
| API em execução | Saúde `UP`; contratos de Dashboard, Orçamentos e Transferências publicados |

O backend foi testado com PostgreSQL isolado por Testcontainers. As consultas à API
habitual foram somente de leitura, sem inserir dados no banco do usuário.

A configuração `.github/dependabot.yml` foi removida desta remessa. Os PRs #25 a #31
foram conferidos e encerrados sem merge: atualização das imagens Java para 25,
Maven Wrapper e versões de pacotes Angular. Nenhuma dessas atualizações foi
incorporada; as dependências e os overrides já existentes na árvore local foram
mantidos. A API do GitHub confirmou os alertas de vulnerabilidades ativos e a
criação automática de PRs de segurança desativada, sem necessidade de alterar
essas configurações. Atualizações de dependências passam a ser avaliadas manualmente.

Ficam para outra etapa a avaliação visual das telas, os retoques que dela surgirem
e a preparação de hospedagem/URL da API/CORS para uma demonstração pública.
O complemento de indicadores do painel continua como proposta separada.
