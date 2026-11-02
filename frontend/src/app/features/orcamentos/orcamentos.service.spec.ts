import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, expect, it } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { OrcamentosService } from './orcamentos.service';
let http: HttpTestingController, service: OrcamentosService;
beforeEach(() => {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: API_URL, useValue: '/api' },
    ],
  });
  http = TestBed.inject(HttpTestingController);
  service = TestBed.inject(OrcamentosService);
});
afterEach(() => http.verify());
it('consulta mês e página explicitamente', () => {
  service.listar('2026-11', 2).subscribe();
  const r = http.expectOne((r) => r.url === '/api/orcamentos');
  expect(r.request.params.get('mes')).toBe('2026-11');
  expect(r.request.params.get('page')).toBe('2');
  expect(r.request.params.get('size')).toBe('10');
  r.flush({});
});
it('cria e edita somente os campos aprovados', () => {
  service.criar(5, '2026-11', 1206.35).subscribe();
  const p = http.expectOne('/api/orcamentos');
  expect(p.request.method).toBe('POST');
  expect(p.request.body).toEqual({ categoriaId: 5, mes: '2026-11', limite: 1206.35 });
  p.flush({});
  service.editar(7, 1400).subscribe();
  const e = http.expectOne('/api/orcamentos/7');
  expect(e.request.method).toBe('PATCH');
  expect(e.request.body).toEqual({ limite: 1400 });
  e.flush({});
});
