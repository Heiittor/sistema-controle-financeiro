import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { API_URL } from '../api/api-url';
import { IR_PARA_LOGIN } from '../sessao/ir-para-login';
import { SessaoService } from '../sessao/sessao.service';
import { erroInterceptor } from './erro.interceptor';

const API = 'http://api.teste';
const NAO_AUTORIZADO = { status: 401, statusText: 'Unauthorized' };

describe('erroInterceptor', () => {
  const irParaLogin = vi.fn();
  let http: HttpClient;
  let backend: HttpTestingController;
  let sessao: SessaoService;
  let erros: HttpErrorResponse[];

  const capturar = { error: (e: HttpErrorResponse) => erros.push(e) };

  beforeEach(() => {
    sessionStorage.clear();
    irParaLogin.mockClear();
    erros = [];

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([erroInterceptor])),
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
    sessionStorage.clear();
  });

  it('401 em rota protegida: limpa a sessão, avisa e leva ao login', () => {
    sessao.iniciar('abc', 3600);

    http.get(`${API}/contas`).subscribe(capturar);
    backend.expectOne(`${API}/contas`).flush({}, NAO_AUTORIZADO);

    expect(sessao.autenticado()).toBe(false);
    expect(sessao.sessaoExpirada()).toBe(true);
    expect(irParaLogin).toHaveBeenCalledTimes(1);
    expect(erros[0].status).toBe(401);
  });

  it('vários 401 simultâneos redirecionam uma única vez', () => {
    sessao.iniciar('abc', 3600);

    http.get(`${API}/contas`).subscribe(capturar);
    http.get(`${API}/categorias`).subscribe(capturar);
    backend.expectOne(`${API}/contas`).flush({}, NAO_AUTORIZADO);
    backend.expectOne(`${API}/categorias`).flush({}, NAO_AUTORIZADO);

    expect(irParaLogin).toHaveBeenCalledTimes(1);
    expect(erros).toHaveLength(2);
  });

  it('401 no login (senha errada) não limpa a sessão nem redireciona', () => {
    sessao.iniciar('abc', 3600);

    http.post(`${API}/auth/login`, {}).subscribe(capturar);
    backend.expectOne(`${API}/auth/login`).flush({}, NAO_AUTORIZADO);

    expect(sessao.autenticado()).toBe(true);
    expect(sessao.sessaoExpirada()).toBe(false);
    expect(irParaLogin).not.toHaveBeenCalled();
    expect(erros[0].status).toBe(401);
  });

  it('401 sem sessão não redireciona', () => {
    http.get(`${API}/contas`).subscribe(capturar);
    backend.expectOne(`${API}/contas`).flush({}, NAO_AUTORIZADO);

    expect(irParaLogin).not.toHaveBeenCalled();
    expect(sessao.sessaoExpirada()).toBe(false);
  });

  it('401 de outro domínio é ignorado', () => {
    sessao.iniciar('abc', 3600);

    http.get('http://outro.com/x').subscribe(capturar);
    backend.expectOne('http://outro.com/x').flush({}, NAO_AUTORIZADO);

    expect(sessao.autenticado()).toBe(true);
    expect(irParaLogin).not.toHaveBeenCalled();
  });

  it('429 e 422 seguem intactos, sem mexer na sessão e sem repetir a chamada', () => {
    sessao.iniciar('abc', 3600);

    http.post(`${API}/auth/login`, {}).subscribe(capturar);
    backend
      .expectOne(`${API}/auth/login`)
      .flush({}, { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '120' } });
    http.get(`${API}/contas`).subscribe(capturar);
    backend.expectOne(`${API}/contas`).flush({}, { status: 422, statusText: 'Unprocessable' });

    expect(erros.map((e) => e.status)).toEqual([429, 422]);
    expect(erros[0].headers.get('Retry-After')).toBe('120');
    expect(sessao.autenticado()).toBe(true);
    expect(irParaLogin).not.toHaveBeenCalled();
    // backend.verify() (no afterEach) falha se alguma requisição extra tiver sido feita.
  });
});
