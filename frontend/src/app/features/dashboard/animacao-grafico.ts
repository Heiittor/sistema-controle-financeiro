import { ChartConfiguration, Plugin } from 'chart.js';
import { DURACAO_ENTRADA } from './movimento';

export function comAnimacao(config: ChartConfiguration, reduzir: boolean): ChartConfiguration {
  if (reduzir) return { ...config, options: { ...config.options, animation: false } };
  let progresso = 0;
  const revelar: Plugin = {
    id: 'entrada-progressiva',
    beforeUpdate(_chart, args) {
      if (args.mode === 'none' || args.mode === 'resize') progresso = 1;
    },
    beforeDatasetsDraw(chart) {
      if (!chart.chartArea) return;
      const { left, top, width, height } = chart.chartArea;
      chart.ctx.save();
      chart.ctx.beginPath();
      chart.ctx.rect(left - 6, top - 6, (width + 12) * progresso, height + 12);
      chart.ctx.clip();
    },
    afterDatasetsDraw(chart) {
      if (chart.chartArea) chart.ctx.restore();
    },
  };
  const linha = config.type === 'line';
  return {
    ...config,
    plugins: [...(config.plugins ?? []), ...(linha ? [revelar] : [])],
    options: {
      ...config.options,
      animation: {
        duration: DURACAO_ENTRADA,
        easing: 'easeOutCubic',
        onProgress(context) {
          const fracao = context.numSteps ? context.currentStep / context.numSteps : 1;
          progresso = 1 - Math.pow(1 - fracao, 3);
        },
        onComplete(context) {
          progresso = 1;
          if (linha) context.chart.draw();
        },
      },
      ...(linha
        ? {
            animations: {
              x: { duration: 0 },
              y: { duration: 0 },
              radius: { from: 0, duration: DURACAO_ENTRADA, easing: 'easeOutCubic' },
              borderWidth: { from: 0, duration: DURACAO_ENTRADA, easing: 'easeOutCubic' },
            },
          }
        : {}),
      transitions: {
        active: { animation: { duration: 0 } },
        resize: { animation: { duration: 0 } },
      },
    },
  };
}
