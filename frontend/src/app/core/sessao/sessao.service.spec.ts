import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SessaoService } from './sessao.service';

describe('SessaoService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-10-22T10:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
    sessionStorage.clear();
  });

  it('começa sem sessão', () => {
    const sessao = new SessaoService();

    expect(sessao.token()).toBeNull();
    expect(sessao.autenticado()).toBe(false);
  });

  it('guarda o token ao iniciar', () => {
    const sessao = new SessaoService();

    sessao.iniciar('abc', 3600);

    expect(sessao.token()).toBe('abc');
    expect(sessao.autenticado()).toBe(true);
  });

  it('sobrevive ao F5 (nova instância lê o sessionStorage)', () => {
    new SessaoService().iniciar('abc', 3600);

    const aposF5 = new SessaoService();

    expect(aposF5.token()).toBe('abc');
    expect(aposF5.autenticado()).toBe(true);
  });

  it('descarta o token vencido e limpa o armazenamento', () => {
    const sessao = new SessaoService();
    sessao.iniciar('abc', 60);

    vi.advanceTimersByTime(61_000);

    expect(sessao.token()).toBeNull();
    expect(sessao.autenticado()).toBe(false);
    expect(sessionStorage.getItem('sessao')).toBeNull();
  });

  it('não restaura uma sessão que venceu enquanto a página estava fechada', () => {
    new SessaoService().iniciar('abc', 60);

    vi.advanceTimersByTime(120_000);

    const aposF5 = new SessaoService();
    expect(aposF5.token()).toBeNull();
    expect(aposF5.autenticado()).toBe(false);
  });

  it('encerra a sessão e apaga o armazenamento', () => {
    const sessao = new SessaoService();
    sessao.iniciar('abc', 3600);

    sessao.encerrar();

    expect(sessao.token()).toBeNull();
    expect(sessao.autenticado()).toBe(false);
    expect(sessionStorage.getItem('sessao')).toBeNull();
  });

  it('ignora conteúdo corrompido no armazenamento', () => {
    sessionStorage.setItem('sessao', '{isto não é json');

    const sessao = new SessaoService();

    expect(sessao.token()).toBeNull();
    expect(sessao.autenticado()).toBe(false);
  });

  it('ignora conteúdo com formato inesperado', () => {
    sessionStorage.setItem('sessao', JSON.stringify({ token: 123 }));

    expect(new SessaoService().token()).toBeNull();
  });
  it('expirar devolve true e marca a sessão como expirada quando havia sessão', () => {
    const sessao = new SessaoService();
    sessao.iniciar('abc', 3600);

    expect(sessao.expirar()).toBe(true);
    expect(sessao.sessaoExpirada()).toBe(true);
    expect(sessao.autenticado()).toBe(false);
  });

  it('expirar devolve false e não marca nada quando não havia sessão', () => {
    const sessao = new SessaoService();

    expect(sessao.expirar()).toBe(false);
    expect(sessao.sessaoExpirada()).toBe(false);
  });

  it('o vencimento marca a sessão como expirada', () => {
    const sessao = new SessaoService();
    sessao.iniciar('abc', 60);

    vi.advanceTimersByTime(61_000);
    sessao.token();

    expect(sessao.sessaoExpirada()).toBe(true);
  });

  it('um novo login limpa o aviso de expiração', () => {
    const sessao = new SessaoService();
    sessao.iniciar('abc', 3600);
    sessao.expirar();

    sessao.iniciar('def', 3600);

    expect(sessao.sessaoExpirada()).toBe(false);
  });

  it('o logout do usuário não marca a sessão como expirada', () => {
    const sessao = new SessaoService();
    sessao.iniciar('abc', 3600);

    sessao.encerrar();

    expect(sessao.sessaoExpirada()).toBe(false);
  });
});
