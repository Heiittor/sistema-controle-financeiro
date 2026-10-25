import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { aplicarErrosDoServidor } from './erros-do-servidor';

function erro400(campos: { campo: string; mensagem: string }[]): HttpErrorResponse {
  return new HttpErrorResponse({
    status: 400,
    error: { status: 400, mensagem: 'Dados inválidos.', timestamp: 'x', campos },
  });
}

describe('aplicarErrosDoServidor', () => {
  it('coloca a mensagem no campo correspondente', () => {
    const form = new FormGroup({ senha: new FormControl('') });

    const aplicou = aplicarErrosDoServidor(
      form,
      erro400([{ campo: 'senha', mensagem: 'A senha deve ter entre 8 e 72 caracteres.' }]),
    );

    expect(aplicou).toBe(true);
    expect(form.controls.senha.getError('servidor')).toBe('A senha deve ter entre 8 e 72 caracteres.');
    expect(form.controls.senha.touched).toBe(true);
  });

  it('ignora campos que o formulário não tem e devolve false', () => {
    const form = new FormGroup({ senha: new FormControl('') });

    expect(aplicarErrosDoServidor(form, erro400([{ campo: 'outro', mensagem: 'x' }]))).toBe(false);
    expect(form.controls.senha.errors).toBeNull();
  });

  it('devolve false para erros que não seguem o formato da API', () => {
    const form = new FormGroup({ senha: new FormControl('') });

    expect(aplicarErrosDoServidor(form, new HttpErrorResponse({ status: 500, error: 'x' }))).toBe(false);
    expect(aplicarErrosDoServidor(form, new Error('x'))).toBe(false);
  });

  it('o erro do servidor some quando o usuário edita o campo', () => {
    const form = new FormGroup({ senha: new FormControl('') });
    aplicarErrosDoServidor(form, erro400([{ campo: 'senha', mensagem: 'ruim' }]));

    form.controls.senha.setValue('nova senha');

    expect(form.controls.senha.hasError('servidor')).toBe(false);
  });
});
