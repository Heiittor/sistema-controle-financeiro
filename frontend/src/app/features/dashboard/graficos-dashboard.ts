import { ChartConfiguration } from 'chart.js';
import { DespesaCategoria, MesDashboard } from './dashboard.models';
import { rotuloMes } from './periodo-dashboard';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
export const CORES_CATEGORIAS = ['#ff982b', '#247bff', '#00a77b', '#8950da', '#949caf', '#db5794'];

export function graficoLinha(serie: MesDashboard[]): ChartConfiguration {
  return {
    type: 'line',
    data: {
      labels: serie.map((m) => rotuloMes(m.mes)),
      datasets: [
        {
          label: 'Receitas',
          data: serie.map((m) => m.receitas),
          borderColor: '#005cff',
          backgroundColor: '#005cff14',
          fill: true,
          pointRadius: 4,
          pointBorderColor: '#fff',
          pointBorderWidth: 1.5,
          tension: 0.2,
        },
        {
          label: 'Despesas',
          data: serie.map((m) => m.despesas),
          borderColor: '#ff394f',
          backgroundColor: '#ff394f0a',
          fill: true,
          pointRadius: 4,
          pointBorderColor: '#fff',
          pointBorderWidth: 1.5,

          tension: 0.2,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      locale: 'pt-BR',
      scales: {
        y: {
          beginAtZero: true,
          grace: '10%',
          ticks: {
            maxTicksLimit: 5,
            color: '#53638b',
            callback: (valor) => moeda.format(Number(valor)),
          },
        },
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (item) => item.dataset.label + ': ' + moeda.format(Number(item.raw)),
          },
        },
      },
    },
  };
}
export function graficoRosca(categorias: DespesaCategoria[]): ChartConfiguration {
  return {
    type: 'doughnut',
    data: {
      labels: categorias.map((c) => c.categoriaNome),
      datasets: [
        {
          data: categorias.map((c) => c.total),
          backgroundColor: categorias.map((_, i) => CORES_CATEGORIAS[i % CORES_CATEGORIAS.length]),
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      locale: 'pt-BR',
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: { label: (item) => item.label + ': ' + moeda.format(Number(item.raw)) },
        },
      },
    },
  };
}
