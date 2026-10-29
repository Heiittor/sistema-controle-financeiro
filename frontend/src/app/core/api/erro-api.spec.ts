import { describe, expect, it } from 'vitest';
import { ehErroApi } from './erro-api';

describe('ehErroApi', () => {
  it('reconhece o formato de erro da API', () => {
    const corpo = {
      status: 422,
      mensagem: 'Saldo insuficiente.',
      timestamp: '2026-10-22T10:00:00Z',
      campos: [],
    };
    expect(ehErroApi(corpo)).toBe(true);
  });

  it('rejeita corpos que não seguem o formato', () => {
    expect(ehErroApi(null)).toBe(false);
    expect(ehErroApi('erro')).toBe(false);
    expect(ehErroApi({ status: 500 })).toBe(false);
    expect(ehErroApi({ status: 500, mensagem: 'x' })).toBe(false);
  });
});
