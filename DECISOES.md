# Decisões de arquitetura

| Decisão | Escolha | Motivo |
|---|---|---|
| Saldo | Armazenado na conta, com `@Version` e `CHECK (saldo >= 0)` | Leitura rápida e regra de saldo dentro da `Conta`; o banco é a última barreira |
| Dinheiro | Value Object `Dinheiro` sobre `BigDecimal` (2 casas, imutável) | Valor inválido não consegue existir; `equals` consistente |
| Aggregate | `Conta` é a raiz, sem lista de transações | Saldo e histórico nunca divergem; consulta de histórico vai direto ao repository |
| Domínio e JPA | Entidades anotadas com JPA, sem setters nas regras | Evita duplicar classes; disciplina em vez de camada extra |
| Repositories | Sem `findById`/`findAll` nos dados com dono | Previne BOLA por construção |
| Categoria | Por usuário, com tipo; `TipoCategoria` separado de `TipoTransacao` | Impede "categoria de transferência" em tempo de compilação |
| Transferência | Duas transações ligadas por `transferenciaId`, só entre contas do mesmo usuário | Mantém `saldo == soma das transações` |
| Correção de lançamentos | Estorno em vez de apagar; edição só de descrição e categoria | Histórico imutável e auditável |
| Resumo | Soma no banco; ignora estornos e originais estornadas | Efeito líquido, sem carregar dados em memória |
| Autenticação | JWT stateless (HS256), BCrypt, mensagem única e tempo igualado no login | Anti-enumeração e anti-timing |
| Rate limit | Em memória, por e-mail e IP | Suficiente para uma instância; em produção, gateway ou Redis |
| Auditoria | Tabela na mesma transação (`MANDATORY`); login só em log | Nunca descreve algo que não aconteceu |
| Erros | Exceções de domínio traduzidas em um só handler, formato único | Domínio não conhece HTTP |
| Configuração | Segredos só em variáveis de ambiente; perfis `dev`/`prod` | Nenhum segredo no repositório |
| Roles | Não implementadas | Todo usuário tem o mesmo papel; seria overengineering |

## Melhorias registradas

- `Periodo` como Value Object (duplicado entre histórico e resumo)
- Detalhes da auditoria montados como texto
- Helpers de teste repetidos
- Refresh token e revogação de JWT