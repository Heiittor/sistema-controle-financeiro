import { FormControl } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { converterValor, validarValor } from './valor';

describe('converterValor', () => {
  it('recusa valores cujo JSON perderia centavos', () => {
    for (const texto of [
      '999999999999999.99',
      '999999999999999,99',
      '100000000000000.01',
      '999.999.999.999.999,99',
      '100.000.000.000.000,01',
    ]) {
      expect(converterValor(texto)).toBeNull();
      expect(validarValor(new FormControl(texto))).toEqual({ precisao: true });
    }
  });

  it('preserva valores representáveis no JSON, inclusive zeros opcionais', () => {
    for (const [texto, json] of [
      ['00010,50', '10.5'],
      ['0,29', '0.29'],
      ['9999999999999.99', '9999999999999.99'],
      ['999999999999999', '999999999999999'],
    ]) {
      expect(JSON.stringify(converterValor(texto))).toBe(json);
    }
  });

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

  it('recusa mais de 2 casas, milhar sem vírgula decimal e formato americano', () => {
    for (const texto of [
      '10,555',
      '10.000',
      '1.206',
      '1.206.350',
      '1.000,505',
      '1,000.50',
      '1,206,350.50',
    ]) {
      expect(converterValor(texto)).toBeNull();
      expect(validarValor(new FormControl(texto))).toEqual({ formato: true });
    }
  });

  it('converte estritamente os formatos brasileiros e simples para o mesmo número', () => {
    for (const [texto, esperado] of [
      ['1.206,35', 1206.35],
      ['1.206.350,50', 1206350.5],
      ['1206,35', 1206.35],
      ['1206.35', 1206.35],
      ['1.000,5', 1000.5],
      ['12.345,00', 12345],
      ['123.456.789,01', 123456789.01],
      ['  1.206,35  ', 1206.35],
      ['999.999.999.999.999,00', 999999999999999],
    ] as const) {
      expect(converterValor(texto)).toBe(esperado);
      expect(validarValor(new FormControl(texto))).toBeNull();
    }
  });

  it('não remove pontuação nem espaços de agrupamentos incorretos', () => {
    for (const texto of [
      '12.06,35',
      '1.20.350,50',
      '1234.567,89',
      '01.206,35',
      '0.000,01',
      '1..206,35',
      '.206,35',
      '1.206,,35',
      '1.206,',
      '1 206,35',
      '1\u00a0206,35',
      'R$ 1.206,35',
      '+1206,35',
      '-1.206,35',
      'NaN',
      'Infinity',
      '0x10',
      '1e3',
      '1000000000000000',
      '1.000.000.000.000.000,00',
    ]) {
      expect(converterValor(texto)).toBeNull();
      expect(validarValor(new FormControl(texto))).toEqual({ formato: true });
    }
  });

  it('recusa zero também no conversor, sem arredondar frações inválidas', () => {
    for (const texto of ['0', '00', '0,0', '000,00', '0.00']) {
      expect(converterValor(texto)).toBeNull();
      expect(validarValor(new FormControl(texto))).toEqual({ positivo: true });
    }
    for (const texto of ['0,001', '0.009', '1.206,359']) expect(converterValor(texto)).toBeNull();
    expect(validarValor(new FormControl(null))).toBeNull();
  });

  it('o validador diferencia formato inválido, zero e vazio', () => {
    expect(validarValor(new FormControl('10,555'))).toEqual({ formato: true });
    expect(validarValor(new FormControl('0'))).toEqual({ positivo: true });
    expect(validarValor(new FormControl('0,00'))).toEqual({ positivo: true });
    expect(validarValor(new FormControl(''))).toBeNull();
    expect(validarValor(new FormControl('12,30'))).toBeNull();
  });
});
