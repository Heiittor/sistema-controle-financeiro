import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SessaoService } from '../sessao/sessao.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let sessao: SessaoService;
  let router: Router;

  const executar = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as boolean | UrlTree;

  beforeEach(() => {
    sessionStorage.clear();
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-10-22T10:00:00Z'));
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    sessao = TestBed.inject(SessaoService);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    vi.useRealTimers();
    sessionStorage.clear();
  });

  it('sem sessão, leva ao login', () => {
    const resultado = executar();

    expect(router.serializeUrl(resultado as UrlTree)).toBe('/login');
  });

  it('com sessão válida, libera a rota', () => {
    sessao.iniciar('abc', 3600);

    expect(executar()).toBe(true);
  });

  it('com sessão vencida, leva ao login e marca a sessão como expirada', () => {
    sessao.iniciar('abc', 60);
    vi.advanceTimersByTime(61_000);

    const resultado = executar();

    expect(router.serializeUrl(resultado as UrlTree)).toBe('/login');
    expect(sessao.sessaoExpirada()).toBe(true);
  });
});
