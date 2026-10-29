import { Pipe, PipeTransform } from '@angular/core';

// Só apresentação: o valor enviado à API nunca passa por aqui.
const formatador = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
});

@Pipe({
  name: 'moeda',
})
export class MoedaPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    // Dado ausente não vira "R$ 0,00": isso pareceria um saldo real.
    if (valor === null || valor === undefined) {
      return '';
    }
    return formatador.format(valor);
  }
}
