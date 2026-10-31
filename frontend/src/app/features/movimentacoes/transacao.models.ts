// Contrato de GET /contas/{id}/transacoes. O valor é sempre positivo; o sinal vem do tipo.
export type TipoTransacao =
  | 'RECEITA'
  | 'DESPESA'
  | 'TRANSFERENCIA_SAIDA'
  | 'TRANSFERENCIA_ENTRADA'
  | 'ESTORNO_RECEITA'
  | 'ESTORNO_DESPESA';

export interface Transacao {
  id: number;
  descricao: string;
  valor: number;
  data: string;
  tipo: TipoTransacao;
  contaId: number;
  // Nulos em transferências e estornos, que não têm categoria.
  categoriaId: number | null;
  categoriaNome: string | null;
  transferenciaId: string | null;
  // Só no estorno: o id da transação original que ele desfaz.
  estornoDeId: number | null;
  // true só na receita ou despesa que já foi estornada.
  estornada: boolean;
}

// Só apresentação: como cada tipo é escrito e se aumenta ou diminui o saldo da conta.
export const INFO_TIPO: Record<TipoTransacao, { rotulo: string; sentido: 'entrada' | 'saida' }> = {
  RECEITA: { rotulo: 'Receita', sentido: 'entrada' },
  DESPESA: { rotulo: 'Despesa', sentido: 'saida' },
  TRANSFERENCIA_ENTRADA: { rotulo: 'Transferência recebida', sentido: 'entrada' },
  TRANSFERENCIA_SAIDA: { rotulo: 'Transferência enviada', sentido: 'saida' },
  ESTORNO_DESPESA: { rotulo: 'Estorno de despesa', sentido: 'entrada' },
  ESTORNO_RECEITA: { rotulo: 'Estorno de receita', sentido: 'saida' },
};

// Corpo de PATCH /contas/{id}/transacoes/{tid}. Valor, data, tipo e conta não mudam (para corrigir, estorna).
export interface EdicaoTransacao {
  categoriaId: number;
  descricao: string;
}

export interface NovaTransacao {
  categoriaId: number;
  descricao: string;
  valor: number;
  data: string;
}
