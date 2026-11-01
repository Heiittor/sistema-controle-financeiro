export interface DespesaCategoria {
  categoriaId: number;
  categoriaNome: string;
  tipo: 'DESPESA';
  total: number;
}
export interface ResumoDashboard {
  inicio: string;
  fim: string;
  saldoTotal: number;
  totalReceitas: number;
  totalDespesas: number;
  saldoDoPeriodo: number;
  porCategoria: DespesaCategoria[];
}
export interface MesDashboard {
  mes: string;
  receitas: number;
  despesas: number;
}
