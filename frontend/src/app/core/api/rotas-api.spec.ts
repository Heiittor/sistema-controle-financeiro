import { HttpRequest } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { ehDaApi, ehRotaPublica } from './rotas-api';

const API = 'http://api.teste';

describe('ehDaApi', () => {
  it('aceita só URLs que começam com a API seguida de barra', () => {
    expect(ehDaApi('http://api.teste/contas', API)).toBe(true);
    expect(ehDaApi('http://api.teste.evil.com/contas', API)).toBe(false);
    expect(ehDaApi('http://outro.com/contas', API)).toBe(false);
  });
});

describe('ehRotaPublica', () => {
  it('login e cadastro (POST) são públicos', () => {
    expect(ehRotaPublica(new HttpRequest('POST', `${API}/auth/login`, {}), API)).toBe(true);
    expect(ehRotaPublica(new HttpRequest('POST', `${API}/usuarios`, {}), API)).toBe(true);
  });

  it('outras rotas e outros métodos não são públicos', () => {
    expect(ehRotaPublica(new HttpRequest('GET', `${API}/contas`), API)).toBe(false);
    expect(ehRotaPublica(new HttpRequest('GET', `${API}/usuarios`), API)).toBe(false);
    expect(ehRotaPublica(new HttpRequest('POST', `${API}/contas`, {}), API)).toBe(false);
  });

  it('rota com o mesmo caminho em outro domínio não é pública', () => {
    expect(ehRotaPublica(new HttpRequest('POST', 'http://outro.com/auth/login', {}), API)).toBe(false);
  });
});
