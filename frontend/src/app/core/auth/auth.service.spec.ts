import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_URL } from '../api/api-url';
import { SessaoService } from '../sessao/sessao.service';
import { AuthService } from './auth.service';

const API = 'http://api.teste';

describe('AuthService', () => {
  let auth: AuthService;
  let backend: HttpTestingController;
  let sessao: SessaoService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: API },
      ],
    });
    auth = TestBed.inject(AuthService);
    backend = TestBed.inject(HttpTestingController);
    sessao = TestBed.inject(SessaoService);
  });

  afterEach(() => {
    backend.verify();
    sessionStorage.clear();
  });

  it('login envia as credenciais e inicia a sessão', () => {
    auth.login({ email: 'ana@teste.com', senha: 'senha12345' }).subscribe();

    const req = backend.expectOne(`${API}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'ana@teste.com', senha: 'senha12345' });
    req.flush({ token: 'abc', tipo: 'Bearer', expiraEmSegundos: 3600 });

    expect(sessao.token()).toBe('abc');
    expect(sessao.autenticado()).toBe(true);
  });

  it('login com erro não inicia a sessão e devolve o erro intacto', () => {
    let status = 0;
    auth.login({ email: 'ana@teste.com', senha: 'errada' }).subscribe({
      error: (e: { status: number }) => (status = e.status),
    });

    backend
      .expectOne(`${API}/auth/login`)
      .flush({ mensagem: 'E-mail ou senha inválidos.' }, { status: 401, statusText: 'Unauthorized' });

    expect(status).toBe(401);
    expect(sessao.autenticado()).toBe(false);
  });

  it('cadastrar envia os dados, devolve o usuário e não inicia sessão', () => {
    let nome = '';
    auth.cadastrar({ nome: 'Ana', email: 'ana@teste.com', senha: 'senha12345' }).subscribe((u) => (nome = u.nome));

    const req = backend.expectOne(`${API}/usuarios`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, nome: 'Ana', email: 'ana@teste.com' });

    expect(nome).toBe('Ana');
    expect(sessao.autenticado()).toBe(false);
  });

  it('sair encerra a sessão sem marcá-la como expirada', () => {
    sessao.iniciar('abc', 3600);

    auth.sair();

    expect(sessao.autenticado()).toBe(false);
    expect(sessao.sessaoExpirada()).toBe(false);
  });
});
