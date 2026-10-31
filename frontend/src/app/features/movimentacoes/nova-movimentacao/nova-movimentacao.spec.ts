import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { API_URL } from '../../../core/api/api-url';
import { NovaMovimentacao } from './nova-movimentacao';

const API = 'http://api.teste';
const CONTAS = [
  { id: 1, nome: 'Nubank', saldo: 0 },
  { id: 2, nome: 'Itaú', saldo: 0 },
];
const DESPESAS = [{ id: 5, nome: 'Mercado', tipo: 'DESPESA' }];
const RECEITAS = [{ id: 8, nome: 'Salário', tipo: 'RECEITA' }];

describe('NovaMovimentacao', () => {
  let fixture: ComponentFixture<NovaMovimentacao>;
  let backend: HttpTestingController;
  const ref = { close: vi.fn() };

  const tela = () => fixture.nativeElement as HTMLElement;
  const texto = () => tela().textContent!.replace(/\s/g, ' ');

  function preencher(campo: string, valor: string, evento = 'input'): void {
    const el = tela().querySelector<HTMLInputElement | HTMLSelectElement>(`[formControlName="${campo}"]`)!;
    el.value = valor;
    el.dispatchEvent(new Event(evento));
  }

  function enviar(): void {
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function responderCategorias(tipo: string, lista: unknown[]): void {
    backend.expectOne(`${API}/categorias?tipo=${tipo}`).flush(lista);
    fixture.detectChanges();
  }

  function preencherValido(valor = '10,50'): void {
    preencher('categoriaId', '5', 'change');
    preencher('descricao', 'Mercado');
    preencher('valor', valor);
  }

  beforeEach(() => {
    ref.close.mockClear();
    // 15:00 UTC de 22/10/2026 = 12:00 em São Paulo.
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-10-22T15:00:00Z'));
    TestBed.configureTestingModule({
      imports: [NovaMovimentacao],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_URL, useValue: API },
        { provide: MAT_DIALOG_DATA, useValue: { contas: CONTAS, contaIdInicial: 1 } },
        { provide: MatDialogRef, useValue: ref },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(NovaMovimentacao);
    fixture.detectChanges();
  });

  afterEach(() => {
    backend.verify();
    vi.useRealTimers();
  });

  it('começa em despesa, carrega as categorias do tipo e usa a data de hoje', () => {
    responderCategorias('DESPESA', DESPESAS);

    expect(texto()).toContain('Mercado');
    expect(tela().querySelector<HTMLInputElement>('input[formControlName="data"]')!.value).toBe('2026-10-22');
  });

  it('trocar o tipo recarrega as categorias do tipo escolhido', () => {
    responderCategorias('DESPESA', DESPESAS);

    preencher('tipo', 'RECEITA', 'change');
    responderCategorias('RECEITA', RECEITAS);

    expect(texto()).toContain('Salário');
    expect(texto()).not.toContain('Mercado');
  });

  it('sem categorias do tipo, orienta a criar e bloqueia o registro', () => {
    responderCategorias('DESPESA', []);

    expect(texto()).toContain('Você ainda não tem categorias deste tipo.');
    expect(tela().querySelector('a[href="/categorias"]')).not.toBeNull();
    expect(tela().querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
  });

  it('não envia com campos vazios e mostra os erros', () => {
    responderCategorias('DESPESA', DESPESAS);

    enviar();

    backend.expectNone((r) => r.method === 'POST');
    expect(texto()).toContain('Escolha a categoria.');
    expect(texto()).toContain('Informe a descrição.');
    expect(texto()).toContain('Informe o valor.');
  });

  it('recusa valor com mais de 2 casas e valor zero, sem chamar a API', () => {
    responderCategorias('DESPESA', DESPESAS);

    preencherValido('10,555');
    enviar();
    expect(texto()).toContain('Use apenas números');

    preencher('valor', '0');
    enviar();
    expect(texto()).toContain('O valor deve ser maior que zero.');
    backend.expectNone((r) => r.method === 'POST');
  });

  it('registra a despesa enviando o valor convertido e fecha informando conta e data', () => {
    responderCategorias('DESPESA', DESPESAS);

    preencherValido('10,50');
    enviar();

    const req = backend.expectOne(`${API}/contas/1/despesas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ categoriaId: 5, descricao: 'Mercado', valor: 10.5, data: '2026-10-22' });
    req.flush({});

    expect(ref.close).toHaveBeenCalledWith({ contaId: 1, data: '2026-10-22' });
  });

  it('registra uma receita na rota de receitas', () => {
    responderCategorias('DESPESA', DESPESAS);
    preencher('tipo', 'RECEITA', 'change');
    responderCategorias('RECEITA', RECEITAS);

    preencher('categoriaId', '8', 'change');
    preencher('descricao', 'Salário');
    preencher('valor', '1500');
    enviar();

    const req = backend.expectOne(`${API}/contas/1/receitas`);
    expect(req.request.body).toEqual({ categoriaId: 8, descricao: 'Salário', valor: 1500, data: '2026-10-22' });
    req.flush({});
  });

  it('422 (saldo insuficiente) aparece no diálogo, que continua aberto', () => {
    responderCategorias('DESPESA', DESPESAS);

    preencherValido('99999');
    enviar();
    backend.expectOne(`${API}/contas/1/despesas`).flush(
      { status: 422, mensagem: 'Saldo insuficiente.', timestamp: 'x', campos: [] },
      { status: 422, statusText: 'erro' },
    );
    fixture.detectChanges();

    expect(tela().querySelector('.erro')!.textContent).toContain('Saldo insuficiente.');
    expect(ref.close).not.toHaveBeenCalled();
  });

  it('400 com campos aponta a mensagem para o campo certo', () => {
    responderCategorias('DESPESA', DESPESAS);

    preencherValido();
    enviar();
    backend.expectOne(`${API}/contas/1/despesas`).flush(
      {
        status: 400,
        mensagem: 'Dados inválidos.',
        timestamp: 'x',
        campos: [{ campo: 'descricao', mensagem: 'A descrição é obrigatória.' }],
      },
      { status: 400, statusText: 'erro' },
    );
    fixture.detectChanges();

    expect(texto()).toContain('A descrição é obrigatória.');
    expect(tela().querySelector('.erro')).toBeNull();
  });

  it('cancelar fecha sem resultado', () => {
    responderCategorias('DESPESA', DESPESAS);

    tela().querySelector<HTMLButtonElement>('button.cancelar')!.click();

    expect(ref.close).toHaveBeenCalledWith();
  });
});
