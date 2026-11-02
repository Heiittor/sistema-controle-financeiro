import { Component, Input } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, afterEach, it, expect, describe, vi } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { Dashboard } from './dashboard';
import { Grafico } from './grafico';
import { ChartConfiguration } from 'chart.js';
import { of, Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import {
  NovaMovimentacao,
  MovimentacaoRegistrada,
} from '../movimentacoes/nova-movimentacao/nova-movimentacao';
import { UltimasMovimentacoesService } from './ultimas-movimentacoes.service';

@Component({ selector: 'app-grafico-dashboard', template: '' })
class GraficoTeste {
  @Input() configuracao!: ChartConfiguration;
  @Input() descricao = '';
  @Input() destaque: number | null = null;
  @Input() altura = 288;
}

describe('Dashboard', () => {
  let fixture: ComponentFixture<Dashboard>;
  let http: HttpTestingController;
  const resumo = {
    inicio: '2026-01-01',
    fim: '2026-01-31',
    saldoTotal: 987.65,
    totalReceitas: 100,
    totalDespesas: 25,
    saldoDoPeriodo: 75,
    porCategoria: [{ categoriaId: 1, categoriaNome: 'Mercado', tipo: 'DESPESA', total: 25 }],
  };
  const serie = [
    { mes: '2025-08', receitas: 0, despesas: 0 },
    { mes: '2026-01', receitas: 100, despesas: 25 },
  ];
  const tela = () => fixture.nativeElement as HTMLElement;
  const texto = () => tela().textContent!.replace(/\s/g, ' ');
  function responder(semContas = false, semMovimento = false) {
    http
      .expectOne('http://api/contas')
      .flush(semContas ? [] : [{ id: 1, nome: 'Conta', saldo: 1 }]);
    const r = http.expectOne((r) => r.url.endsWith('/dashboard/resumo'));
    expect(r.request.method).toBe('GET');
    expect(r.request.params.get('inicio')).toBe('2026-01-01');
    expect(r.request.params.get('fim')).toBe('2026-01-31');
    r.flush(
      semMovimento
        ? { ...resumo, totalReceitas: 0, totalDespesas: 0, saldoDoPeriodo: 0, porCategoria: [] }
        : resumo,
    );
    const s = http.expectOne((r) => r.url.endsWith('/dashboard/serie-mensal'));
    expect(s.request.params.get('inicio')).toBe('2025-08-01');
    expect(s.request.params.get('fim')).toBe('2026-01-31');
    s.flush(serie);
    fixture.detectChanges();
  }
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-20T15:00:00Z'));
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_URL, useValue: 'http://api' },
        { provide: UltimasMovimentacoesService, useValue: { listar: () => of([]) } },
      ],
    }).overrideComponent(Dashboard, {
      remove: { imports: [Grafico] },
      add: { imports: [GraficoTeste] },
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();
  });
  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  it('mostra carregamento e usa os indicadores recebidos, sem somar saldos das contas', () => {
    expect(texto()).toContain('Carregando seu panorama');
    responder();
    expect(texto()).toContain('R$ 987,65');
    expect(texto()).toContain('R$ 75,00');
    expect(texto()).toContain('01/08/2025 a 31/01/2026');
    expect(tela().querySelectorAll('table tbody tr')).toHaveLength(2);
  });
  it('orienta a criar conta quando não há contas', () => {
    responder(true);
    expect(texto()).toContain('Comece pela sua primeira conta');
    expect(tela().querySelector('a[href="/contas"]')).not.toBeNull();
    expect(tela().querySelector('.indicadores')).toBeNull();
  });
  it('distingue ausência de movimentações e ausência de despesas', () => {
    responder(false, true);
    expect(texto()).toContain('Nenhuma receita ou despesa não estornada');
    expect(texto()).toContain('Nenhuma despesa não estornada');
    expect(tela().querySelector('.legenda')).toBeNull();
  });
  it('destaca e desmarca categoria pela legenda acessível', () => {
    responder();
    const botao = tela().querySelector<HTMLButtonElement>('.legenda button')!;
    botao.click();
    fixture.detectChanges();
    expect(botao.getAttribute('aria-pressed')).toBe('true');
    botao.click();
    fixture.detectChanges();
    expect(botao.getAttribute('aria-pressed')).toBe('false');
  });
  it('mostra falha sem números antigos e permite nova consulta', () => {
    const pendentes = http.match(() => true);
    pendentes[0].flush({}, { status: 500, statusText: 'erro' });
    fixture.detectChanges();
    expect(tela().querySelector('[role="alert"]')).not.toBeNull();
    expect(tela().querySelector('.indicadores')).toBeNull();
    Array.from(tela().querySelectorAll('button'))
      .find((b) => b.textContent?.includes('Tentar novamente'))!
      .click();
    responder();
  });
  it('cancela consultas anteriores ao aplicar outro mês', () => {
    const antigas = http.match(() => true);
    const input = tela().querySelector<HTMLInputElement>('input')!;
    input.value = '2026-02';
    input.dispatchEvent(new Event('input'));
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    expect(antigas.every((r) => r.cancelled)).toBe(true);
    const novas = http.match(() => true);
    expect(novas.find((r) => r.request.url.endsWith('/resumo'))!.request.params.get('fim')).toBe(
      '2026-02-28',
    );
    expect(
      novas.find((r) => r.request.url.endsWith('/serie-mensal'))!.request.params.get('inicio'),
    ).toBe('2025-09-01');
    fixture.destroy();
    expect(novas.every((r) => r.cancelled)).toBe(true);
  });
  it('não consulta com mês vazio', () => {
    responder();
    const input = tela().querySelector<HTMLInputElement>('input')!;
    input.value = '';
    input.dispatchEvent(new Event('input'));
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    http.expectNone(() => true);
    expect(texto()).toContain('Selecione um mês válido');
  });

  it('oferece os quatro acessos rápidos e abre o formulário com o tipo escolhido', () => {
    responder();
    const abrir = vi
      .spyOn(TestBed.inject(MatDialog), 'open')
      .mockReturnValue({ afterClosed: () => of(undefined) } as never);
    expect(tela().querySelector('.acessos-rapidos a[href="/transferencias"]')).not.toBeNull();
    expect(tela().querySelector('.acessos-rapidos a[href="/orcamentos"]')).not.toBeNull();
    tela().querySelector<HTMLButtonElement>('.atalho.receita')!.click();
    expect(abrir.mock.calls[0][0]).toBe(NovaMovimentacao);
    expect((abrir.mock.calls[0][1] as { data: unknown }).data).toEqual({
      contas: [{ id: 1, nome: 'Conta', saldo: 1 }],
      contaIdInicial: 1,
      tipoInicial: 'RECEITA',
    });
    tela().querySelector<HTMLButtonElement>('.atalho.despesa')!.click();
    expect((abrir.mock.calls[1][1] as { data: { tipoInicial: string } }).data.tipoInicial).toBe(
      'DESPESA',
    );
    http.expectNone(() => true);
  });

  it('desabilita lançamentos enquanto carrega ou não há contas e mantém os links de navegação', () => {
    expect(tela().querySelector<HTMLButtonElement>('.atalho.receita')!.disabled).toBe(true);
    responder(true);
    expect(tela().querySelector<HTMLButtonElement>('.atalho.despesa')!.disabled).toBe(true);
    expect(tela().querySelector('.acessos-rapidos a[href="/transferencias"]')).not.toBeNull();
  });

  it('não abre dois diálogos e recarrega o painel pela API após salvar fora do período', () => {
    responder();
    const fechado = new Subject<MovimentacaoRegistrada | undefined>();
    const abrir = vi
      .spyOn(TestBed.inject(MatDialog), 'open')
      .mockReturnValue({ afterClosed: () => fechado } as never);
    const receita = tela().querySelector<HTMLButtonElement>('.atalho.receita')!;
    receita.click();
    receita.click();
    expect(abrir).toHaveBeenCalledTimes(1);
    fechado.next({ contaId: 1, data: '2026-02-10' });
    fechado.complete();
    responder();
    expect(texto()).toContain('fora do período exibido');
    expect(texto()).toContain('R$ 987,65');
  });

  it('retoma o mês aplicado para atualizar após registro quando o campo de mês está vazio', () => {
    responder();
    const input = tela().querySelector<HTMLInputElement>('input')!;
    input.value = '';
    input.dispatchEvent(new Event('input'));
    vi.spyOn(TestBed.inject(MatDialog), 'open').mockReturnValue({
      afterClosed: () => of({ contaId: 1, data: '2026-01-20' }),
    } as never);
    tela().querySelector<HTMLButtonElement>('.atalho.receita')!.click();
    responder();
    expect(texto()).toContain('Movimentação registrada.');
    expect(input.value).toBe('2026-01');
  });
});
