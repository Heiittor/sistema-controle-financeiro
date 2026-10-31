import { ValidationErrors, ValidatorFn } from '@angular/forms';

// Aceita só: dígitos, com um separador decimal (vírgula ou ponto) e 1 ou 2 casas.
// NÃO aceita separador de milhar: "10.000" seria ambíguo (dez? dez mil?), então é recusado.
const FORMATO = /^\d{1,15}([.,]\d{1,2})?$/;

// Devolve o número, ou null se o texto não segue o formato. Sem arredondar nada.
export function converterValor(texto: string): number | null {
  const limpo = texto.trim();
  if (!FORMATO.test(limpo)) {
    return null;
  }
  return Number(limpo.replace(',', '.'));
}

// Só orienta o usuário: o backend valida de novo (positivo, 2 casas).
export const validarValor: ValidatorFn = (controle): ValidationErrors | null => {
  const texto = String(controle.value ?? '');
  if (texto.trim() === '') {
    return null; // "obrigatório" é outra regra
  }
  const valor = converterValor(texto);
  if (valor === null) {
    return { formato: true };
  }
  return valor > 0 ? null : { positivo: true };
};
