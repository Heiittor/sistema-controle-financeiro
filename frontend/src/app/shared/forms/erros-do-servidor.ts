import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ehErroApi } from '../../core/api/erro-api';
import { mensagemDeErro } from '../../core/api/mensagem-erro';

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

// Tratamento padrão de uma falha de envio:
//  - 409 (conflito, ex.: nome repetido) vai para o campo indicado;
//  - 400 com campos[] vai para cada campo;
//  - qualquer outro caso devolve a mensagem geral para a tela exibir.
// Devolve a mensagem geral, ou null quando o erro já foi colocado em algum campo.
export function tratarErroDeEnvio(form: FormGroup, erro: unknown, campoDeConflito: string): string | null {
  if (erro instanceof HttpErrorResponse && erro.status === 409) {
    const controle = form.get(campoDeConflito);
    if (controle !== null) {
      marcarErroDoServidor(controle, mensagemDeErro(erro));
      return null;
    }
  }
  return aplicarErrosDoServidor(form, erro) ? null : mensagemDeErro(erro);
}
