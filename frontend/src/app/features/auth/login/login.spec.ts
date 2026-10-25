import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, MockInstance, vi } from 'vitest';
import { API_URL } from '../../../core/api/api-url';
import { SessaoService } from '../../../core/sessao/sessao.service';
import { Login } from './login';

const API = 'http://api.teste';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let backend: HttpTestingController;
  let sessao: SessaoService;
  let navegar: MockInstance;

  const tela = () => fixture.nativeElement as HTMLElement;

  function digitar(campo: string, valor: string): void {
    const input = tela().querySelector<HTMLInputElement>(`input[formControlName="${campo}"]`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  function enviar(): void {
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    fixture.detectChanges();
  }

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_URL, useValue: API },
      ],
    });
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    backend = TestBed.inject(HttpTestingController);
    sessao = TestBed.inject(SessaoService);
    fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
  });

  afterEach(() => {
    backend.verify();
    vi.useRealTimers();
    sessionStorage.clear();
  });

  it('não envia com o formulário inválido e mostra os erros', () => {
    enviar();

    backend.expectNone(`${API}/auth/login`);
    expect(tela().textContent).toContain('Informe o e-mail.');
    expect(tela().textContent).toContain('Informe a senha.');
  });

  it('envia as credenciais, inicia a sessão e vai para a página inicial', () => {
    digitar('email', 'ana@teste.com');
    digitar('senha', 'senha12345');
    enviar();

    const req = backend.expectOne(`${API}/auth/login`);
    expect(req.request.body).toEqual({ email: 'ana@teste.com', senha: 'senha12345' });
    req.flush({ token: 'abc', tipo: 'Bearer', expiraEmSegundos: 3600 });

    expect(sessao.autenticado()).toBe(true);
    expect(navegar).toHaveBeenCalledWith('/');
  });

  it('401 mostra a mensagem da API, mantém o formulário e não navega', () => {
    digitar('email', 'ana@teste.com');
    digitar('senha', 'errada');
    enviar();

    backend.expectOne(`${API}/auth/login`).flush(
      { status: 401, mensagem: 'E-mail ou senha inválidos.', timestamp: 'x', campos: [] },
      { status: 401, statusText: 'Unauthorized' },
    );
    fixture.detectChanges();

    expect(tela().textContent).toContain('E-mail ou senha inválidos.');
    expect(tela().querySelector<HTMLInputElement>('input[formControlName="email"]')!.value).toBe('ana@teste.com');
    expect(sessao.autenticado()).toBe(false);
    expect(navegar).not.toHaveBeenCalled();
  });

  it('429 bloqueia o botão, conta o Retry-After e não repete a tentativa', () => {
    vi.useFakeTimers();
    digitar('email', 'ana@teste.com');
    digitar('senha', 'senha12345');
    enviar();

    backend.expectOne(`${API}/auth/login`).flush(
      { status: 429, mensagem: 'Muitas tentativas de login.', timestamp: 'x', campos: [] },
      { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '120' } },
    );
    fixture.detectChanges();

    const botao = tela().querySelector<HTMLButtonElement>('button[type="submit"]')!;
    expect(tela().textContent).toContain('02:00');
    expect(botao.disabled).toBe(true);

    vi.advanceTimersByTime(1000);
    fixture.detectChanges();
    expect(tela().textContent).toContain('01:59');

    // Mesmo forçando o envio durante o bloqueio, nenhuma requisição sai.
    enviar();
    backend.expectNone(`${API}/auth/login`);
  });

  it('avisa quando a sessão expirou', () => {
    sessao.iniciar('abc', 3600);
    sessao.expirar();
    fixture.detectChanges();

    expect(tela().textContent).toContain('Sua sessão expirou');
  });
});
