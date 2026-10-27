// Contrato de GET /categorias e POST /categorias.
export type TipoCategoria = 'RECEITA' | 'DESPESA';

export interface Categoria {
  id: number;
  nome: string;
  tipo: TipoCategoria;
}

export const TIPOS_CATEGORIA: readonly { valor: TipoCategoria; rotulo: string }[] = [
  { valor: 'RECEITA', rotulo: 'Receita' },
  { valor: 'DESPESA', rotulo: 'Despesa' },
];

export const ROTULO_TIPO: Record<TipoCategoria, string> = {
  RECEITA: 'Receita',
  DESPESA: 'Despesa',
};
