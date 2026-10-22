import { describe, expect, it } from 'vitest';
import { MoedaPipe } from './moeda-pipe';

// O Intl usa espaço não separável entre "R$" e o número. Normalizamos para comparar.
const normalizar = (texto: string) => texto.replace(/\s/g, ' ');

describe('MoedaPipe', () => {
  const pipe = new MoedaPipe();

  it('formata em reais com separadores brasileiros', () => {
    expect(normalizar(pipe.transform(1234.5))).toBe('R$ 1.234,50');
  });

  it('sempre mostra duas casas decimais', () => {
    expect(normalizar(pipe.transform(10))).toBe('R$ 10,00');
  });

  it('formata zero como valor real', () => {
    expect(normalizar(pipe.transform(0))).toBe('R$ 0,00');
  });

  it('formata valores negativos', () => {
    expect(normalizar(pipe.transform(-50.25))).toBe('-R$ 50,25');
  });

  it('devolve vazio para dado ausente', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
