import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { ContasService } from './contas.service';

const API = 'http://api.teste';

describe('ContasService', () => {
  let service: ContasService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: API }],
    });
    service = TestBed.inject(ContasService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('listar busca as contas na API', () => {
    let nomes: string[] = [];
    service.listar().subscribe((contas) => (nomes = contas.map((c) => c.nome)));

    const req = backend.expectOne(`${API}/contas`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, nome: 'Nubank', saldo: 0 }]);

    expect(nomes).toEqual(['Nubank']);
  });

  it('criar envia só o nome', () => {
    service.criar('Nubank').subscribe();

    const req = backend.expectOne(`${API}/contas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nome: 'Nubank' });
    req.flush({ id: 1, nome: 'Nubank', saldo: 0 });
  });
});
