import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { DadosOrcamento, FormularioOrcamento } from './formulario-orcamento';
let fixture: ComponentFixture<FormularioOrcamento>, http: HttpTestingController;
let ref: { close: ReturnType<typeof vi.fn>; disableClose: boolean };
function abrir(dados: DadosOrcamento = { mes: '2026-11' }) {
  TestBed.overrideProvider(MAT_DIALOG_DATA, { useValue: dados });
  fixture = TestBed.createComponent(FormularioOrcamento);
  http = TestBed.inject(HttpTestingController);
  fixture.detectChanges();
}
const tela = () => fixture.nativeElement as HTMLElement;
function preencher(campo: string, valor: string) {
  const el = tela().querySelector<HTMLInputElement | HTMLSelectElement>(
    '[formControlName="' + campo + '"]',
  )!;
  el.value = valor;
  el.dispatchEvent(new Event(el.tagName === 'SELECT' ? 'change' : 'input'));
}
function enviar() {
  tela().querySelector('form')!.dispatchEvent(new Event('submit'));
  fixture.detectChanges();
}
beforeEach(() => {
  ref = { close: vi.fn(), disableClose: false };
  TestBed.configureTestingModule({
    imports: [FormularioOrcamento],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([]),
      { provide: API_URL, useValue: '/api' },
      { provide: MAT_DIALOG_DATA, useValue: {} },
      { provide: MatDialogRef, useValue: ref },
    ],
  });
});
afterEach(() => http.verify());
function categorias(lista = [{ id: 2, nome: 'Mercado', tipo: 'DESPESA' }]) {
  http.expectOne('/api/categorias?tipo=DESPESA').flush(lista);
  fixture.detectChanges();
}
it('valida campos e cria com moeda brasileira sem envio duplicado', () => {
  abrir();
  categorias();
  enviar();
  http.expectNone('/api/orcamentos');
  preencher('categoriaId', '2');
  preencher('limite', '1.206,35');
  enviar();
  enviar();
  const r = http.expectOne('/api/orcamentos');
  expect(r.request.body).toEqual({ categoriaId: 2, mes: '2026-11', limite: 1206.35 });
  expect(ref.disableClose).toBe(true);
  r.flush({});
  expect(ref.close).toHaveBeenCalledWith(true);
  expect(ref.disableClose).toBe(false);
});
it('edita somente o limite, sem consultar categorias nem permitir trocar mês ou categoria', () => {
  abrir({
    mes: '2026-11',
    orcamento: {
      id: 3,
      categoriaId: 2,
      categoriaNome: 'Mercado',
      mes: '2026-11',
      limite: 100,
      gasto: 50,
      restante: 50,
      percentualConsumido: 50,
      situacao: 'DENTRO_DO_LIMITE',
    },
  });
  http.expectNone((r) => r.url.includes('categorias'));
  expect(tela().querySelector('[formControlName=mes]')).toBeNull();
  preencher('limite', '300,00');
  enviar();
  const r = http.expectOne('/api/orcamentos/3');
  expect(r.request.method).toBe('PATCH');
  expect(r.request.body).toEqual({ limite: 300 });
  r.flush({});
  expect(ref.close).toHaveBeenCalledWith(true);
});
it('sem categorias orienta a cadastrar e bloqueia envio', () => {
  abrir();
  categorias([]);
  expect(tela().querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(true);
  expect(tela().querySelector('a[href="/categorias"]')).not.toBeNull();
});
it('mantém o formulário em conflito de unicidade e permite corrigir', () => {
  abrir();
  categorias();
  preencher('categoriaId', '2');
  preencher('limite', '100');
  enviar();
  http
    .expectOne('/api/orcamentos')
    .flush(
      { status: 409, mensagem: 'Já existe orçamento.', timestamp: 'x', campos: [] },
      { status: 409, statusText: 'erro' },
    );
  fixture.detectChanges();
  expect(tela().textContent).toContain('Já existe orçamento.');
  expect(ref.close).not.toHaveBeenCalled();
  expect(ref.disableClose).toBe(false);
});
