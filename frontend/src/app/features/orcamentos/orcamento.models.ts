import { Pagina } from '../../core/api/pagina';
export const MES_VALIDO = /^(?!0000)\d{4}-(0[1-9]|1[0-2])$/;
export interface Orcamento {
  id: number;
  categoriaId: number;
  categoriaNome: string;
  mes: string;
  limite: number;
  gasto: number;
  restante: number;
  percentualConsumido: number;
  situacao: 'DENTRO_DO_LIMITE' | 'ATENCAO' | 'ACIMA_DO_LIMITE';
}
export interface PaginaOrcamentos extends Pagina<Orcamento> {
  resumo: { ativos: number; dentroDoPlanejado: number; acimaDoLimite: number };
}
export function formatarMes(mes: string): string {
  if (!MES_VALIDO.test(mes)) return mes;
  const nomes = [
    'jan',
    'fev',
    'mar',
    'abr',
    'mai',
    'jun',
    'jul',
    'ago',
    'set',
    'out',
    'nov',
    'dez',
  ];
  return nomes[Number(mes.slice(5)) - 1] + '/' + mes.slice(0, 4);
}
