import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { beforeEach, afterEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { Transacao } from '../movimentacoes/transacao.models';
import { UltimasMovimentacoesService } from './ultimas-movimentacoes.service';

describe('Últimas movimentações do painel', () => {
  let http: HttpTestingController;
  let api: UltimasMovimentacoesService;
  const linha = (id: number, data: string, contaId = 1): Transacao => ({
    id,
    data,
    contaId,
    descricao: 'Teste',
    tipo: 'RECEITA',
    valor: 10,
    categoriaId: 1,
    categoriaNome: 'Receita',
    transferenciaId: null,
    estornoDeId: null,
    estornada: false,
  });
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: 'http://api' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    api = TestBed.inject(UltimasMovimentacoesService);
  });
  afterEach(() => http.verify());
  it('combina por data e id, mantém somente quatro e preserva os valores', () => {
    let resultado: Transacao[] = [];
    api.listar([1, 2], '2026-01-01', '2026-01-31').subscribe((r) => (resultado = r));
    const pedidos = http.match(() => true);
    expect(pedidos).toHaveLength(2);
    for (const p of pedidos) {
      expect(p.request.params.get('size')).toBe('4');
      expect(p.request.params.get('page')).toBe('0');
      expect(p.request.params.get('inicio')).toBe('2026-01-01');
    }
    pedidos[1].flush({ conteudo: [linha(9, '2026-01-20', 2), linha(6, '2026-01-18', 2)] });
    pedidos[0].flush({
      conteudo: [linha(8, '2026-01-20'), linha(5, '2026-01-17'), linha(1, '2026-01-01')],
    });
    expect(resultado.map((r) => r.id)).toEqual([9, 8, 6, 5]);
    expect(resultado.every((r) => r.valor === 10)).toBe(true);
  });
  it('não consulta quando não existem contas', () => {
    let resultado: Transacao[] | undefined;
    api.listar([], '2026-01-01', '2026-01-31').subscribe((r) => (resultado = r));
    expect(resultado).toEqual([]);
    http.expectNone(() => true);
  });
  it('limita a quatro consultas simultâneas e propaga erros', () => {
    let erro: unknown;
    api.listar([1, 2, 3, 4, 5], '2026-01-01', '2026-01-31').subscribe({ error: (e) => (erro = e) });
    const primeiros = http.match(() => true);
    expect(primeiros).toHaveLength(4);
    primeiros[0].flush({ conteudo: [] });
    const quinto = http.expectOne((r) => r.url.endsWith('/5/transacoes'));
    quinto.flush({}, { status: 500, statusText: 'erro' });
    expect(erro).toBeDefined();
    expect(primeiros.slice(1).every((p) => p.cancelled)).toBe(true);
  });
});
