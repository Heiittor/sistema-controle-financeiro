import { ValidationErrors, ValidatorFn } from '@angular/forms';

const SIMPLES = /^\d{1,15}([.,]\d{1,2})?$/;
// Com milhar, a vírgula decimal é obrigatória para evitar ambiguidades como "1.206".
// Grupos de três dígitos, sem zeros à esquerda e com até 15 dígitos inteiros.
const BRASILEIRO = /^[1-9]\d{0,2}(\.\d{3}){1,4},\d{1,2}$/;
const DECIMAL = /^\d{1,15}(\.\d{1,2})?$/;

function decimalDoTexto(texto: string): string | null {
  const limpo = texto.trim();
  if (SIMPLES.test(limpo)) return limpo.replace(',', '.');
  if (BRASILEIRO.test(limpo)) return limpo.replace(/\./g, '').replace(',', '.');
  return null;
}

// Normaliza somente a escrita: zeros à esquerda e casas decimais opcionais.
function normalizarDecimal(texto: string): string {
  const [inteiro, fracao = ''] = texto.split('.');
  return `${BigInt(inteiro)}.${fracao.padEnd(2, '0')}`;
}

// Só aceita quando o JSON preserva exatamente o valor decimal digitado.
export function converterValor(texto: string): number | null {
  const decimal = decimalDoTexto(texto);
  if (decimal === null) {
    return null;
  }
  const valor = Number(decimal);
  if (valor <= 0) return null;
  const serializado = JSON.stringify(valor);
  return DECIMAL.test(serializado) && normalizarDecimal(serializado) === normalizarDecimal(decimal)
    ? valor
    : null;
}

// Só orienta o usuário: o backend valida de novo (positivo, 2 casas).
export const validarValor: ValidatorFn = (controle): ValidationErrors | null => {
  const texto = String(controle.value ?? '');
  if (texto.trim() === '') {
    return null; // "obrigatório" é outra regra
  }
  const decimal = decimalDoTexto(texto);
  if (decimal === null) {
    return { formato: true };
  }
  if (Number(decimal) === 0) return { positivo: true };
  const valor = converterValor(texto);
  if (valor === null) {
    return { precisao: true };
  }
  return null;
};
