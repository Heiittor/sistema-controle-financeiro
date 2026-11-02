import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { TransferenciasService } from './transferencias.service';

const API = 'http://api.teste';
const DADOS = {
  contaOrigemId: 1,
  contaDestinoId: 2,
  descricao: 'Reserva',
  valor: 100.5,
  data: '2026-10-22',
};

describe('TransferenciasService', () => {
  let service: TransferenciasService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: API },
      ],
    });
    service = TestBed.inject(TransferenciasService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('consulta todas as transferências com paginação, sem corte de período', () => {
    service.listar(null, 2).subscribe();
    const req = backend.expectOne((r) => r.url === `${API}/transferencias`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().sort()).toEqual(['page', 'size']);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('5');
    req.flush({ conteudo: [], pagina: 2, tamanho: 5, totalElementos: 0, totalPaginas: 0 });
  });

  it('envia o filtro de conta sem mudar o contrato do POST', () => {
    service.listar(7).subscribe();
    const req = backend.expectOne((r) => r.url === `${API}/transferencias`);
    expect(req.request.params.get('contaId')).toBe('7');
    req.flush({ conteudo: [], pagina: 0, tamanho: 5, totalElementos: 0, totalPaginas: 0 });
  });

  it('envia uma única requisição POST com os dados da transferência', () => {
    service.transferir(DADOS).subscribe();

    const req = backend.expectOne(`${API}/transferencias`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(DADOS);
    req.flush({ transferenciaId: 'abc', ...DADOS });
  });

  it('devolve a resposta exatamente como veio da API', () => {
    let id = '';
    service.transferir(DADOS).subscribe((t) => (id = t.transferenciaId));

    backend.expectOne(`${API}/transferencias`).flush({ transferenciaId: 'abc-123', ...DADOS });

    expect(id).toBe('abc-123');
  });
});
