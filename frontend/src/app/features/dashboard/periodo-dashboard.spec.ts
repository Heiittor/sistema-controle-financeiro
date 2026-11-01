import { describe, it, expect } from 'vitest';
import { periodoDashboard, rotuloMes } from './periodo-dashboard';

describe('Período do Dashboard', () => {
  it('usa seis meses e cruza a virada do ano', () => {
    expect(periodoDashboard('2026-01')).toEqual({
      inicio: '2026-01-01',
      fim: '2026-01-31',
      inicioSerie: '2025-08-01',
    });
  });
  it('considera fevereiro bissexto', () => {
    expect(periodoDashboard('2024-02')?.fim).toBe('2024-02-29');
  });
  it('recusa mês incompleto, inválido ou fora do calendário suportado', () => {
    for (const mes of ['', '2026-13', '2026-00', '2026-1', 'abc', '0000-01'])
      expect(periodoDashboard(mes)).toBeNull();
  });
  it('formata o mês sem converter para timestamp', () => {
    expect(rotuloMes('2026-01')).toBe('jan/2026');
  });
});
