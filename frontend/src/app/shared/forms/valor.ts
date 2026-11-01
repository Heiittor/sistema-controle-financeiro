import { ValidationErrors, ValidatorFn } from '@angular/forms';

// Aceita só: dígitos, com um separador decimal (vírgula ou ponto) e 1 ou 2 casas.
// NÃO aceita separador de milhar: "10.000" seria ambíguo (dez? dez mil?), então é recusado.
const FORMATO = /^\d{1,15}([.,]\d{1,2})?$/;

// Normaliza somente a escrita: zeros à esquerda e casas decimais opcionais.
function normalizarDecimal(texto: string): string {
  const [inteiro, fracao = ''] = texto.split('.');
  return `${BigInt(inteiro)}.${fracao.padEnd(2, '0')}`;
}

// Só aceita quando o JSON preserva exatamente o valor decimal digitado.
export function converterValor(texto: string): number | null {
  const limpo = texto.trim();
  if (!FORMATO.test(limpo)) {
    return null;
  }
  const decimal = limpo.replace(',', '.');
  const valor = Number(decimal);
  const serializado = JSON.stringify(valor);
  return FORMATO.test(serializado) && normalizarDecimal(serializado) === normalizarDecimal(decimal)
    ? valor
    : null;
}

// Só orienta o usuário: o backend valida de novo (positivo, 2 casas).
export const validarValor: ValidatorFn = (controle): ValidationErrors | null => {
  const texto = String(controle.value ?? '');
  if (texto.trim() === '') {
    return null; // "obrigatório" é outra regra
  }
  if (!FORMATO.test(texto.trim())) {
    return { formato: true };
  }
  const valor = converterValor(texto);
  if (valor === null) {
    return { precisao: true };
  }
  return valor > 0 ? null : { positivo: true };
};
