import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { API_URL } from '../api/api-url';
import { IR_PARA_LOGIN } from '../sessao/ir-para-login';
import { SessaoService } from '../sessao/sessao.service';
import { tokenInterceptor } from './token.interceptor';

const API = 'http://api.teste';

describe('tokenInterceptor', () => {
  const irParaLogin = vi.fn();
  let http: HttpClient;
  let backend: HttpTestingController;
  let sessao: SessaoService;

  beforeEach(() => {
    sessionStorage.clear();
    irParaLogin.mockClear();
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-10-22T10:00:00Z'));

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([tokenInterceptor])),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: API },
        { provide: IR_PARA_LOGIN, useValue: irParaLogin },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    sessao = TestBed.inject(SessaoService);
  });

  afterEach(() => {
    backend.verify();
    vi.useRealTimers();
    sessionStorage.clear();
  });

  it('anexa o token nas rotas protegidas da API', () => {
    sessao.iniciar('abc', 3600);

    http.get(`${API}/contas`).subscribe();

    const req = backend.expectOne(`${API}/contas`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc');
  });

  it('não envia o token para outros domínios, nem para um que imite a API', () => {
    sessao.iniciar('abc', 3600);

    http.get('http://outro.com/x').subscribe();
    http.get('http://api.teste.evil.com/x').subscribe();

    expect(backend.expectOne('http://outro.com/x').request.headers.has('Authorization')).toBe(false);
    expect(backend.expectOne('http://api.teste.evil.com/x').request.headers.has('Authorization')).toBe(false);
  });

  it('não envia o token no login nem no cadastro', () => {
    sessao.iniciar('abc', 3600);

    http.post(`${API}/auth/login`, {}).subscribe();
    http.post(`${API}/usuarios`, {}).subscribe();

    expect(backend.expectOne(`${API}/auth/login`).request.headers.has('Authorization')).toBe(false);
    expect(backend.expectOne(`${API}/usuarios`).request.headers.has('Authorization')).toBe(false);
  });

  it('sem sessão, envia sem cabeçalho e não redireciona', () => {
    http.get(`${API}/contas`).subscribe();

    const req = backend.expectOne(`${API}/contas`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    expect(irParaLogin).not.toHaveBeenCalled();
  });

  it('token vencido: avisa, redireciona uma única vez e não envia o cabeçalho', () => {
    sessao.iniciar('abc', 60);
    vi.advanceTimersByTime(61_000);

    http.get(`${API}/contas`).subscribe();
    http.get(`${API}/categorias`).subscribe();

    expect(backend.expectOne(`${API}/contas`).request.headers.has('Authorization')).toBe(false);
    backend.expectOne(`${API}/categorias`);
    expect(irParaLogin).toHaveBeenCalledTimes(1);
    expect(sessao.sessaoExpirada()).toBe(true);
  });
});
