import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { aplicarErrosDoServidor, tratarErroDeEnvio } from './erros-do-servidor';

function erroApi(status: number, mensagem: string, campos: { campo: string; mensagem: string }[] = []) {
  return new HttpErrorResponse({
    status,
    error: { status, mensagem, timestamp: 'x', campos },
  });
}

describe('aplicarErrosDoServidor', () => {
  it('coloca a mensagem no campo correspondente', () => {
    const form = new FormGroup({ senha: new FormControl('') });

    const aplicou = aplicarErrosDoServidor(
      form,
      erroApi(400, 'Dados inválidos.', [{ campo: 'senha', mensagem: 'A senha deve ter entre 8 e 72 caracteres.' }]),
    );

    expect(aplicou).toBe(true);
    expect(form.controls.senha.getError('servidor')).toBe('A senha deve ter entre 8 e 72 caracteres.');
    expect(form.controls.senha.touched).toBe(true);
  });

  it('ignora campos que o formulário não tem e devolve false', () => {
    const form = new FormGroup({ senha: new FormControl('') });

    expect(aplicarErrosDoServidor(form, erroApi(400, 'x', [{ campo: 'outro', mensagem: 'x' }]))).toBe(false);
    expect(form.controls.senha.errors).toBeNull();
  });

  it('devolve false para erros que não seguem o formato da API', () => {
    const form = new FormGroup({ senha: new FormControl('') });

    expect(aplicarErrosDoServidor(form, new HttpErrorResponse({ status: 500, error: 'x' }))).toBe(false);
    expect(aplicarErrosDoServidor(form, new Error('x'))).toBe(false);
  });

  it('o erro do servidor some quando o usuário edita o campo', () => {
    const form = new FormGroup({ senha: new FormControl('') });
    aplicarErrosDoServidor(form, erroApi(400, 'x', [{ campo: 'senha', mensagem: 'ruim' }]));

    form.controls.senha.setValue('nova senha');

    expect(form.controls.senha.hasError('servidor')).toBe(false);
  });
});

describe('tratarErroDeEnvio', () => {
  const novoForm = () => new FormGroup({ nome: new FormControl(''), email: new FormControl('') });

  it('409 vai para o campo de conflito e não gera mensagem geral', () => {
    const form = novoForm();

    const geral = tratarErroDeEnvio(form, erroApi(409, 'Este e-mail já está em uso.'), 'email');

    expect(geral).toBeNull();
    expect(form.controls.email.getError('servidor')).toBe('Este e-mail já está em uso.');
  });

  it('400 distribui campos[] e não gera mensagem geral', () => {
    const form = novoForm();

    const geral = tratarErroDeEnvio(
      form,
      erroApi(400, 'Dados inválidos.', [{ campo: 'nome', mensagem: 'O nome é obrigatório.' }]),
      'email',
    );

    expect(geral).toBeNull();
    expect(form.controls.nome.getError('servidor')).toBe('O nome é obrigatório.');
  });

  it('devolve a mensagem geral quando nenhum campo foi atendido', () => {
    const geral = tratarErroDeEnvio(novoForm(), erroApi(422, 'Saldo insuficiente.'), 'email');

    expect(geral).toBe('Saldo insuficiente.');
  });

  it('409 sem o campo no formulário cai na mensagem geral', () => {
    const form = new FormGroup({ nome: new FormControl('') });

    const geral = tratarErroDeEnvio(form, erroApi(409, 'Este e-mail já está em uso.'), 'email');

    expect(geral).toBe('Este e-mail já está em uso.');
  });
});
