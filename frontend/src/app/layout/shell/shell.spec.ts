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

    const rotulos = Array.from(tela.querySelectorAll('nav a.item span')).map((s) =>
      s.textContent?.trim(),
    );
    expect(rotulos).toEqual([
      'Visão geral',
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

  it('recolhe e expande o menu preservando nomes acessíveis e a rota ativa', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/contas');
    const tela = harness.routeNativeElement!;
    tela.querySelector<HTMLButtonElement>('button[aria-label="Recolher menu"]')!.click();
    harness.detectChanges();

    expect(tela.querySelector('.sidebar')!.classList.contains('recolhida')).toBe(true);
    expect(
      tela.querySelector('button[aria-label="Expandir menu"]')!.getAttribute('aria-expanded'),
    ).toBe('false');
    expect(tela.querySelector('a[href="/contas"]')!.getAttribute('aria-label')).toBe('Contas');
    expect(tela.querySelector('a[href="/contas"]')!.getAttribute('aria-current')).toBe('page');

    tela.querySelector<HTMLButtonElement>('button[aria-label="Expandir menu"]')!.click();
    harness.detectChanges();
    expect(tela.querySelector('.sidebar')!.classList.contains('recolhida')).toBe(false);
  });

  it('o atalho de teclado transfere o foco ao conteúdo principal', async () => {
    const tela = await abrir('/');
    const principal = tela.querySelector<HTMLElement>('main')!;
    const focar = vi.spyOn(principal, 'focus');
    tela.querySelector<HTMLAnchorElement>('.pular')!.click();
    expect(focar).toHaveBeenCalledOnce();
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

  it('abre e fecha o menu móvel com controles acessíveis', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/');
    const tela = harness.routeNativeElement!;
    const abrirMenu = tela.querySelector<HTMLButtonElement>('button[aria-label="Abrir menu"]')!;
    expect(abrirMenu.getAttribute('aria-expanded')).toBe('false');
    expect(tela.querySelector('.recolher')).toBeNull();

    abrirMenu.click();
    harness.detectChanges();
    expect(abrirMenu.getAttribute('aria-expanded')).toBe('true');
    tela.querySelector<HTMLButtonElement>('button[aria-label="Fechar menu"]')!.click();
    harness.detectChanges();
    expect(abrirMenu.getAttribute('aria-expanded')).toBe('false');
  });
});
