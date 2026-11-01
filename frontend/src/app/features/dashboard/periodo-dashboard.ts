import { ultimoDiaDoMes } from '../../shared/datas/datas';

// Aritmética apenas de calendário; não altera valores financeiros.
export function periodoDashboard(mes: string) {
  if (!/^[0-9]{4}-(0[1-9]|1[0-2])$/.test(mes) || mes < '0001-06') return null;
  const [ano, numero] = mes.split('-').map(Number);
  const indice = ano * 12 + numero - 1 - 5;
  const inicioSerie =
    String(Math.floor(indice / 12)).padStart(4, '0') +
    '-' +
    String((indice % 12) + 1).padStart(2, '0') +
    '-01';
  return { inicio: mes + '-01', fim: ultimoDiaDoMes(mes + '-01'), inicioSerie };
}
export function rotuloMes(mes: string): string {
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
  return nomes[Number(mes.slice(5, 7)) - 1] + '/' + mes.slice(0, 4);
}
