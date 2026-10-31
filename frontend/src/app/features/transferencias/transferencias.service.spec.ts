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
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: API }],
    });
    service = TestBed.inject(TransferenciasService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

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
