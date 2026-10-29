import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, MockInstance, vi } from 'vitest';
import { API_URL } from '../../../core/api/api-url';
import { Cadastro } from './cadastro';

const API = 'http://api.teste';

describe('Cadastro', () => {
  let fixture: ComponentFixture<Cadastro>;
  let backend: HttpTestingController;
  let navegar: MockInstance;

  const tela = () => fixture.nativeElement as HTMLElement;

  function digitar(campo: string, valor: string): void {
    const input = tela().querySelector<HTMLInputElement>(`input[formControlName="${campo}"]`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  function preencherEEnviar(): void {
    digitar('nome', 'Ana');
    digitar('email', 'ana@teste.com');
    digitar('senha', 'senha12345');
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  const erroApi = (status: number, mensagem: string, campos: { campo: string; mensagem: string }[] = []) => ({
    corpo: { status, mensagem, timestamp: 'x', campos },
    opcoes: { status, statusText: 'erro' },
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Cadastro],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_URL, useValue: API },
      ],
    });
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Cadastro);
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  it('não envia com o formulário inválido e mostra os erros', () => {
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    backend.expectNone(`${API}/usuarios`);
    expect(tela().textContent).toContain('Informe o nome.');
    expect(tela().textContent).toContain('Informe o e-mail.');
    expect(tela().textContent).toContain('Informe a senha.');
  });

  it('envia os dados e vai para o login com o aviso de conta criada', () => {
    preencherEEnviar();

    const req = backend.expectOne(`${API}/usuarios`);
    expect(req.request.body).toEqual({ nome: 'Ana', email: 'ana@teste.com', senha: 'senha12345' });
    req.flush({ id: 1, nome: 'Ana', email: 'ana@teste.com' });

    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { cadastro: 'ok' } });
  });

  it('400 com campos mostra a mensagem embaixo do campo, sem erro geral', () => {
    preencherEEnviar();

    const { corpo, opcoes } = erroApi(400, 'Dados inválidos.', [
      { campo: 'senha', mensagem: 'A senha deve ter entre 8 e 72 caracteres.' },
    ]);
    backend.expectOne(`${API}/usuarios`).flush(corpo, opcoes);
    fixture.detectChanges();

    expect(tela().textContent).toContain('A senha deve ter entre 8 e 72 caracteres.');
    expect(tela().querySelector('.erro')).toBeNull();
    expect(navegar).not.toHaveBeenCalled();
  });

  it('409 mostra "e-mail em uso" no campo e-mail', () => {
    preencherEEnviar();

    const { corpo, opcoes } = erroApi(409, 'Este e-mail já está em uso.');
    backend.expectOne(`${API}/usuarios`).flush(corpo, opcoes);
    fixture.detectChanges();

    expect(tela().textContent).toContain('Este e-mail já está em uso.');
    expect(tela().querySelector('.erro')).toBeNull();
  });

  it('erro sem campos mostra a mensagem geral', () => {
    preencherEEnviar();

    const { corpo, opcoes } = erroApi(422, 'O e-mail é inválido.');
    backend.expectOne(`${API}/usuarios`).flush(corpo, opcoes);
    fixture.detectChanges();

    expect(tela().querySelector('.erro')!.textContent).toContain('O e-mail é inválido.');
  });
});
