import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { API_URL } from '../../../core/api/api-url';
import { NovaMovimentacao } from './nova-movimentacao';

describe('Formulário aberto pelos acessos rápidos', () => {
  let http: HttpTestingController;
  afterEach(() => http.verify());
  it.each(['RECEITA', 'DESPESA'] as const)(
    'inicia em %s, consulta categorias compatíveis e envia à rota correta',
    (tipo) => {
      TestBed.configureTestingModule({
        imports: [NovaMovimentacao],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter([]),
          { provide: API_URL, useValue: '/api' },
          {
            provide: MAT_DIALOG_DATA,
            useValue: {
              contas: [{ id: 7, nome: 'Conta', saldo: 2000 }],
              contaIdInicial: 7,
              tipoInicial: tipo,
            },
          },
          { provide: MatDialogRef, useValue: { close: vi.fn() } },
        ],
      });
      http = TestBed.inject(HttpTestingController);
      const fixture = TestBed.createComponent(NovaMovimentacao);
      fixture.detectChanges();
      http.expectOne('/api/categorias?tipo=' + tipo).flush([{ id: 5, nome: 'Categoria', tipo }]);
      fixture.detectChanges();
      const tela = fixture.nativeElement as HTMLElement;
      expect(tela.querySelector<HTMLSelectElement>('[formControlName="tipo"]')!.value).toBe(tipo);
      for (const [campo, valor] of [
        ['categoriaId', '5'],
        ['descricao', 'Lançamento pelo painel'],
        ['valor', '1.206,35'],
      ]) {
        const el = tela.querySelector<HTMLInputElement | HTMLSelectElement>(
          '[formControlName="' + campo + '"]',
        )!;
        el.value = valor;
        el.dispatchEvent(new Event(el.tagName === 'SELECT' ? 'change' : 'input'));
      }
      tela.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();
      const r = http.expectOne('/api/contas/7/' + (tipo === 'RECEITA' ? 'receitas' : 'despesas'));
      expect(r.request.method).toBe('POST');
      expect(r.request.body).toEqual(
        expect.objectContaining({
          categoriaId: 5,
          descricao: 'Lançamento pelo painel',
          valor: 1206.35,
        }),
      );
      r.flush({});
    },
  );
});
