import { describe, expect, it } from 'vitest';
import { formatarData, hojeNegocio, primeiroDiaDoMes, ultimoDiaDoMes } from './datas';

describe('datas', () => {
  it('hoje usa o fuso de negócio: 23h em Brasília ainda é o dia anterior ao UTC', () => {
    // 02:00 UTC do dia 22 = 23:00 do dia 21 em São Paulo (UTC-3).
    expect(hojeNegocio(new Date('2026-10-22T02:00:00Z'))).toBe('2026-10-21');
  });

  it('hoje vira o dia à meia-noite de Brasília', () => {
    expect(hojeNegocio(new Date('2026-10-22T03:00:00Z'))).toBe('2026-10-22');
  });

  it('hoje respeita a virada de ano', () => {
    expect(hojeNegocio(new Date('2027-01-01T02:59:00Z'))).toBe('2026-12-31');
  });

  it('primeiro dia do mês', () => {
    expect(primeiroDiaDoMes('2026-10-22')).toBe('2026-10-01');
  });

  it('último dia do mês, inclusive fevereiro e ano bissexto', () => {
    expect(ultimoDiaDoMes('2026-10-05')).toBe('2026-10-31');
    expect(ultimoDiaDoMes('2026-11-05')).toBe('2026-11-30');
    expect(ultimoDiaDoMes('2026-02-10')).toBe('2026-02-28');
    expect(ultimoDiaDoMes('2028-02-10')).toBe('2028-02-29');
    expect(ultimoDiaDoMes('2026-12-31')).toBe('2026-12-31');
  });

  it('formata para o padrão brasileiro sem passar por Date', () => {
    expect(formatarData('2026-10-05')).toBe('05/10/2026');
  });
});
