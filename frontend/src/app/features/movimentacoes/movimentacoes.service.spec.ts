import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { MovimentacoesService } from './movimentacoes.service';

const API = 'http://api.teste';

describe('MovimentacoesService', () => {
  let service: MovimentacoesService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: API }],
    });
    service = TestBed.inject(MovimentacoesService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('consulta a conta com período e paginação explícitos', () => {
    service.listar(7, { inicio: '2026-10-01', fim: '2026-10-31', pagina: 2, tamanho: 20 }).subscribe();

    const req = backend.expectOne((r) => r.url === `${API}/contas/7/transacoes`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('inicio')).toBe('2026-10-01');
    expect(req.request.params.get('fim')).toBe('2026-10-31');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ conteudo: [], pagina: 2, tamanho: 20, totalElementos: 0, totalPaginas: 0 });
  });

  it('devolve a página exatamente como veio da API', () => {
    let totalPaginas = -1;
    service
      .listar(1, { inicio: '2026-10-01', fim: '2026-10-31', pagina: 0, tamanho: 20 })
      .subscribe((p) => (totalPaginas = p.totalPaginas));

    backend
      .expectOne((r) => r.url === `${API}/contas/1/transacoes`)
      .flush({ conteudo: [], pagina: 0, tamanho: 20, totalElementos: 45, totalPaginas: 3 });

    expect(totalPaginas).toBe(3);
  });

  it('estornar envia POST sem corpo para a rota de estorno da transação', () => {
    service.estornar(7, 42).subscribe();

    const req = backend.expectOne(`${API}/contas/7/transacoes/42/estorno`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush({});
  });

  it('editar envia PATCH só com categoria e descrição', () => {
    service.editar(7, 42, { categoriaId: 3, descricao: 'Novo texto' }).subscribe();

    const req = backend.expectOne(`${API}/contas/7/transacoes/42`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ categoriaId: 3, descricao: 'Novo texto' });
    req.flush({});
  });

  it('registrar uma receita usa a rota de receitas e envia o corpo sem o tipo', () => {
    const dados = { categoriaId: 3, descricao: 'Salário', valor: 1000.5, data: '2026-10-05' };

    service.registrar(7, 'RECEITA', dados).subscribe();

    const req = backend.expectOne(`${API}/contas/7/receitas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dados);
    req.flush({});
  });

  it('registrar uma despesa usa a rota de despesas', () => {
    service
      .registrar(7, 'DESPESA', { categoriaId: 4, descricao: 'Mercado', valor: 20, data: '2026-10-05' })
      .subscribe();

    const req = backend.expectOne(`${API}/contas/7/despesas`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
