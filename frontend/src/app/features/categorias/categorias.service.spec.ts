import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { CategoriasService } from './categorias.service';

const API = 'http://api.teste';

describe('CategoriasService', () => {
  let service: CategoriasService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: API }],
    });
    service = TestBed.inject(CategoriasService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('listar sem filtro não envia o parâmetro tipo', () => {
    service.listar().subscribe();

    const req = backend.expectOne(`${API}/categorias`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('listar com filtro envia ?tipo=', () => {
    service.listar('DESPESA').subscribe();

    backend.expectOne(`${API}/categorias?tipo=DESPESA`).flush([]);
  });

  it('criar envia nome e tipo', () => {
    service.criar('Salário', 'RECEITA').subscribe();

    const req = backend.expectOne(`${API}/categorias`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nome: 'Salário', tipo: 'RECEITA' });
    req.flush({ id: 1, nome: 'Salário', tipo: 'RECEITA' });
  });
});
