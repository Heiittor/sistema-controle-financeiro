import { FormControl } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { converterValor, validarValor } from './valor';

describe('converterValor', () => {
  it('aceita inteiro, vírgula e ponto', () => {
    expect(converterValor('10')).toBe(10);
    expect(converterValor('10,5')).toBe(10.5);
    expect(converterValor('10.50')).toBe(10.5);
    expect(converterValor('0,01')).toBe(0.01);
  });

  it('ignora espaços nas pontas', () => {
    expect(converterValor('  25,90 ')).toBe(25.9);
  });

  it('recusa texto, sinal negativo e notação científica', () => {
    for (const texto of ['', 'abc', '-5', '1e3', '10,', ',5', '10 5']) {
      expect(converterValor(texto)).toBeNull();
    }
  });

  it('recusa mais de 2 casas e separador de milhar (ambíguo)', () => {
    for (const texto of ['10,555', '10.000', '1.000,50', '1,000.50']) {
      expect(converterValor(texto)).toBeNull();
    }
  });

  it('o validador diferencia formato inválido, zero e vazio', () => {
    expect(validarValor(new FormControl('10,555'))).toEqual({ formato: true });
    expect(validarValor(new FormControl('0'))).toEqual({ positivo: true });
    expect(validarValor(new FormControl('0,00'))).toEqual({ positivo: true });
    expect(validarValor(new FormControl(''))).toBeNull();
    expect(validarValor(new FormControl('12,30'))).toBeNull();
  });
});
