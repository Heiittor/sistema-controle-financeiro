import { BreakpointObserver } from '@angular/cdk/layout';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, Routes } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SessaoService } from '../../core/sessao/sessao.service';
import { EmConstrucao } from '../../shared/componentes/em-construcao/em-construcao';
import { Shell } from './shell';

const rotas: Routes = [
  {
    path: '',
    component: Shell,
    children: [
      { path: '', pathMatch: 'full', title: 'Dashboard', component: EmConstrucao },
      { path: 'contas', title: 'Contas', component: EmConstrucao },
    ],
  },
];

async function abrir(url: string): Promise<HTMLElement> {
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  harness.detectChanges();
  return harness.routeNativeElement as HTMLElement;
}

describe('Shell (tela larga)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter(rotas)] });
  });

  it('lista os seis itens de navegação', async () => {
    const tela = await abrir('/');

    const rotulos = Array.from(tela.querySelectorAll('nav a.item span')).map((s) => s.textContent?.trim());
    expect(rotulos).toEqual([
      'Dashboard',
      'Contas',
      'Movimentações',
      'Categorias',
      'Transferências',
      'Orçamentos',
    ]);
  });

  it('mostra no topo o título da página atual', async () => {
    const tela = await abrir('/contas');

    expect(tela.querySelector('header h1')?.textContent?.trim()).toBe('Contas');
  });

  it('marca só o item da página atual com aria-current', async () => {
    const tela = await abrir('/contas');

    expect(tela.querySelector('a.item[href="/contas"]')?.getAttribute('aria-current')).toBe('page');
    expect(tela.querySelector('a.item[href="/"]')?.getAttribute('aria-current')).toBeNull();
  });

  it('não mostra o botão de menu em tela larga', async () => {
    const tela = await abrir('/');

    expect(tela.querySelector('button[aria-label="Abrir menu"]')).toBeNull();
  });

  it('sair encerra a sessão e leva ao login', async () => {
    const tela = await abrir('/');
    const sessao = TestBed.inject(SessaoService);
    sessao.iniciar('abc', 3600);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    tela.querySelector<HTMLButtonElement>('button.sair')!.click();

    expect(sessao.autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith('/login');
  });
});

describe('Shell (tela compacta)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideRouter(rotas),
        {
          provide: BreakpointObserver,
          useValue: { observe: () => of({ matches: true, breakpoints: {} }) },
        },
      ],
    });
  });

  it('mostra o botão que abre o menu', async () => {
    const tela = await abrir('/');

    expect(tela.querySelector('button[aria-label="Abrir menu"]')).not.toBeNull();
  });
});
