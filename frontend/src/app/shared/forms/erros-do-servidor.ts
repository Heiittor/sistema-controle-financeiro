import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ehErroApi } from '../../core/api/erro-api';

// O erro do servidor vive no próprio controle e desaparece quando o usuário edita o valor.
export function marcarErroDoServidor(controle: AbstractControl, mensagem: string): void {
  controle.setErrors({ servidor: mensagem });
  controle.markAsTouched();
}

// Distribui o campos[] do ErroApi pelos controles do formulário.
// Devolve true se algum campo foi atendido; false = a tela deve mostrar a mensagem geral.
export function aplicarErrosDoServidor(form: FormGroup, erro: unknown): boolean {
  if (!(erro instanceof HttpErrorResponse) || !ehErroApi(erro.error)) {
    return false;
  }
  let aplicou = false;
  for (const { campo, mensagem } of erro.error.campos) {
    const controle = form.get(campo);
    if (controle !== null) {
      marcarErroDoServidor(controle, mensagem);
      aplicou = true;
    }
  }
  return aplicou;
}
