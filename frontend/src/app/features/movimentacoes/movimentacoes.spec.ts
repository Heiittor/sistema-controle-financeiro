import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { Movimentacoes } from './movimentacoes';

const API = 'http://api.teste';
const CONTAS = [
  { id: 1, nome: 'Nubank', saldo: 0 },
  { id: 2, nome: 'Itaú', saldo: 0 },
];

const pagina = (conteudo: unknown[], extra: object = {}) => ({
  conteudo,
  pagina: 0,
  tamanho: 20,
  totalElementos: conteudo.length,
  totalPaginas: conteudo.length > 0 ? 1 : 0,
  ...extra,
});

const linha = (extra: object = {}) => ({
  id: 1,
  descricao: 'Mercado',
  valor: 100,
  data: '2026-10-05',
  tipo: 'DESPESA',
  contaId: 1,
  categoriaId: 2,
  categoriaNome: 'Alimentação',
  transferenciaId: null,
  estornoDeId: null,
  estornada: false,
  ...extra,
});

describe('Movimentacoes', () => {
  let fixture: ComponentFixture<Movimentacoes>;
  let backend: HttpTestingController;

  const tela = () => fixture.nativeElement as HTMLElement;
  // O Intl usa espaço não separável entre "R$" e o número. Normalizamos para comparar.
  const texto = () => tela().textContent!.replace(/\s/g, ' ');
  const linhas = () => Array.from(tela().querySelectorAll('ul.lista li')).map((li) => li.textContent!.replace(/\s/g, ' '));
  const historico = (contaId = 1) => backend.expectOne((r) => r.url === `${API}/contas/${contaId}/transacoes`);

  function preencher(campo: string, valor: string, evento = 'input'): void {
    const el = tela().querySelector<HTMLInputElement | HTMLSelectElement>(`[formControlName="${campo}"]`)!;
    el.value = valor;
    el.dispatchEvent(new Event(evento));
  }

  function aplicar(): void {
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  // O GET das contas sai no construtor. Depois dele a tela pede o histórico da primeira conta.
  function abrirComHistorico(resposta: object): void {
    backend.expectOne(`${API}/contas`).flush(CONTAS);
    fixture.detectChanges();
    historico().flush(resposta);
    fixture.detectChanges();
  }

  beforeEach(() => {
    // 15:00 UTC de 22/10/2026 = 12:00 em São Paulo: o mês atual é outubro de 2026.
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-10-22T15:00:00Z'));
    TestBed.configureTestingModule({
      imports: [Movimentacoes],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_URL, useValue: API },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Movimentacoes);
    fixture.detectChanges();
  });

  afterEach(() => {
    backend.verify();
    vi.useRealTimers();
  });

  it('sem contas, orienta a criar uma e não consulta o histórico', () => {
    backend.expectOne(`${API}/contas`).flush([]);
    fixture.detectChanges();

    expect(texto()).toContain('Você ainda não tem contas');
    expect(tela().querySelector('a[href="/contas"]')).not.toBeNull();
  });

  it('consulta a primeira conta com o mês atual e mostra o período aplicado', () => {
    backend.expectOne(`${API}/contas`).flush(CONTAS);
    fixture.detectChanges();

    const req = historico(1);
    expect(req.request.params.get('inicio')).toBe('2026-10-01');
    expect(req.request.params.get('fim')).toBe('2026-10-31');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(pagina([]));
    fixture.detectChanges();

    expect(texto()).toContain('Nubank');
    expect(texto()).toContain('01/10/2026 a 31/10/2026');
  });

  it('mostra tipo escrito, sinal, valor formatado, estornada e a referência do estorno', () => {
    abrirComHistorico(
      pagina([
        linha({ id: 1, estornada: true }),
        linha({ id: 2, tipo: 'ESTORNO_DESPESA', descricao: 'Estorno: Mercado', categoriaId: null, categoriaNome: null, estornoDeId: 1 }),
        linha({ id: 3, tipo: 'TRANSFERENCIA_SAIDA', descricao: 'Reserva', valor: 50, categoriaId: null, categoriaNome: null, transferenciaId: 'abc' }),
      ]),
    );

    expect(linhas()).toHaveLength(3);
    expect(linhas()[0]).toContain('Despesa');
    expect(linhas()[0]).toContain('Estornada');
    expect(linhas()[0]).toContain('− R$ 100,00');
    expect(linhas()[1]).toContain('Estorno de despesa');
    expect(linhas()[1]).toContain('+ R$ 100,00');
    expect(linhas()[1]).toContain('nº 1');
    expect(linhas()[2]).toContain('Transferência enviada');
    expect(linhas()[2]).toContain('Sem categoria');
  });

  it('avisa quando não há movimentações no período', () => {
    abrirComHistorico(pagina([]));

    expect(texto()).toContain('Nenhuma movimentação neste período.');
  });

  it('pagina mantendo a conta e o período aplicados', () => {
    abrirComHistorico(pagina([linha()], { totalElementos: 45, totalPaginas: 3 }));

    expect(texto()).toContain('Página 1 de 3');
    expect(texto()).toContain('45 movimentações');
    expect(tela().querySelector<HTMLButtonElement>('button.anterior')!.disabled).toBe(true);

    tela().querySelector<HTMLButtonElement>('button.proxima')!.click();
    const req = historico(1);
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('inicio')).toBe('2026-10-01');
    expect(req.request.params.get('fim')).toBe('2026-10-31');
    req.flush(pagina([linha({ id: 2 })], { pagina: 1, totalElementos: 45, totalPaginas: 3 }));
    fixture.detectChanges();

    expect(texto()).toContain('Página 2 de 3');
  });

  it('aplicar um novo filtro volta à página 0 e usa a conta e as datas escolhidas', () => {
    abrirComHistorico(pagina([linha()]));

    preencher('conta', '2', 'change');
    preencher('inicio', '2026-09-01');
    preencher('fim', '2026-09-30');
    aplicar();

    const req = historico(2);
    expect(req.request.params.get('inicio')).toBe('2026-09-01');
    expect(req.request.params.get('fim')).toBe('2026-09-30');
    expect(req.request.params.get('page')).toBe('0');
    req.flush(pagina([]));
    fixture.detectChanges();

    expect(texto()).toContain('Itaú');
    expect(texto()).toContain('01/09/2026 a 30/09/2026');
  });

  it('bloqueia período invertido no cliente, sem consultar a API', () => {
    abrirComHistorico(pagina([linha()]));

    preencher('inicio', '2026-11-01');
    preencher('fim', '2026-10-01');
    aplicar();

    backend.expectNone((r) => r.url.includes('/transacoes'));
    expect(texto()).toContain('A data inicial não pode ser posterior à data final.');
  });

  it('em erro mostra a mensagem da API e permite tentar novamente', () => {
    backend.expectOne(`${API}/contas`).flush(CONTAS);
    fixture.detectChanges();
    historico().flush(
      { status: 500, mensagem: 'Erro interno. Tente novamente mais tarde.', timestamp: 'x', campos: [] },
      { status: 500, statusText: 'erro' },
    );
    fixture.detectChanges();
    expect(texto()).toContain('Erro interno. Tente novamente mais tarde.');

    tela().querySelector<HTMLButtonElement>('button.tentar')!.click();
    historico().flush(pagina([]));
    fixture.detectChanges();

    expect(texto()).toContain('Nenhuma movimentação neste período.');
  });

  it('ignora a resposta atrasada de uma consulta antiga', () => {
    abrirComHistorico(pagina([]));

    preencher('inicio', '2026-08-01');
    preencher('fim', '2026-08-31');
    aplicar();
    const antiga = historico();

    preencher('inicio', '2026-09-01');
    preencher('fim', '2026-09-30');
    aplicar();
    const nova = historico();

    // A mais nova responde primeiro; a antiga chega depois e não pode sobrescrevê-la.
    nova.flush(pagina([linha({ id: 9, descricao: 'Resposta nova' })]));
    antiga.flush(pagina([linha({ id: 8, descricao: 'Resposta antiga' })]));
    fixture.detectChanges();

    expect(texto()).toContain('Resposta nova');
    expect(texto()).not.toContain('Resposta antiga');
  });
});
