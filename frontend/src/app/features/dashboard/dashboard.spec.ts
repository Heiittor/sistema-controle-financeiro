import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SessaoService } from '../../core/sessao/sessao.service';
import { Dashboard } from './dashboard';

describe('Dashboard (provisório)', () => {
  let fixture: ComponentFixture<Dashboard>;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideHttpClient(), provideRouter([])],
    });
    fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();
  });

  it('mostra o cabeçalho neutro', () => {
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Minha conta');
  });

  it('sair encerra a sessão e leva ao login', () => {
    const sessao = TestBed.inject(SessaoService);
    sessao.iniciar('abc', 3600);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    fixture.nativeElement.querySelector('button').click();

    expect(sessao.autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith('/login');
  });
});
