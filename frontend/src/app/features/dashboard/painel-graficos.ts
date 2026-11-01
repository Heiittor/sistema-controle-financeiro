import { ChartConfiguration } from 'chart.js';
import { MesDashboard } from './dashboard.models';
export function minigrafico(
  serie: MesDashboard[],
  campo: 'receitas' | 'despesas',
): ChartConfiguration {
  const cor = campo === 'receitas' ? '#00a46c' : '#ff394f';
  return {
    type: 'line',
    data: {
      labels: serie.map((m) => m.mes),
      datasets: [
        {
          data: serie.map((m) => m[campo]),
          borderColor: cor,
          backgroundColor: campo === 'receitas' ? '#00b67a18' : '#ff394f15',
          fill: true,
          pointRadius: 0,
          borderWidth: 1.8,
          tension: 0.25,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      scales: { x: { display: false }, y: { display: false } },
      plugins: { legend: { display: false }, tooltip: { enabled: false } },
    },
  };
}
