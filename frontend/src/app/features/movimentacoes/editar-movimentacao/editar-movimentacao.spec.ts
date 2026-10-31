import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { API_URL } from '../../../core/api/api-url';
import { EditarMovimentacao } from './editar-movimentacao';

const API = 'http://api.teste';
const TRANSACAO = {
  id: 10,
  descricao: 'Mercado',
  valor: 100,
  data: '2026-10-05',
  tipo: 'DESPESA',
  contaId: 1,
  categoriaId: 6,
  categoriaNome: 'Lazer',
  transferenciaId: null,
  estornoDeId: null,
  estornada: false,
};
const CATEGORIAS = [
  { id: 5, nome: 'Alimentação', tipo: 'DESPESA' },
  { id: 6, nome: 'Lazer', tipo: 'DESPESA' },
];

describe('EditarMovimentacao', () => {
  let fixture: ComponentFixture<EditarMovimentacao>;
  let backend: HttpTestingController;
  const ref = { close: vi.fn() };

  const tela = () => fixture.nativeElement as HTMLElement;
  const texto = () => tela().textContent!.replace(/\s/g, ' ');
  const campo = <T extends HTMLElement>(nome: string) => tela().querySelector<T>(`[formControlName="${nome}"]`)!;

  function preencher(nome: string, valor: string, evento = 'input'): void {
    const el = campo<HTMLInputElement | HTMLSelectElement>(nome);
    el.value = valor;
    el.dispatchEvent(new Event(evento));
  }

  function salvar(): void {
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function responderCategorias(): void {
    backend.expectOne(`${API}/categorias?tipo=DESPESA`).flush(CATEGORIAS);
    fixture.detectChanges();
  }

  beforeEach(() => {
    ref.close.mockClear();
    TestBed.configureTestingModule({
      imports: [EditarMovimentacao],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: API },
        { provide: MAT_DIALOG_DATA, useValue: { contaId: 1, transacao: TRANSACAO } },
        { provide: MatDialogRef, useValue: ref },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EditarMovimentacao);
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  it('carrega as categorias do tipo da transação e pré-seleciona a atual', () => {
    responderCategorias();

    expect(campo<HTMLSelectElement>('categoriaId').value).toBe('6');
    expect(campo<HTMLInputElement>('descricao').value).toBe('Mercado');
  });

  it('mostra valor, data e tipo só para leitura, sem campos para alterá-los', () => {
    responderCategorias();

    expect(texto()).toContain('R$ 100,00');
    expect(texto()).toContain('05/10/2026');
    expect(texto()).toContain('Despesa');
    expect(texto()).toContain('estorne esta movimentação');
    for (const nome of ['valor', 'data', 'tipo', 'conta']) {
      expect(tela().querySelector(`[formControlName="${nome}"]`)).toBeNull();
    }
  });

  it('salvar envia só categoria e descrição e fecha informando a alteração', () => {
    responderCategorias();

    preencher('categoriaId', '5', 'change');
    preencher('descricao', 'Novo texto');
    salvar();

    const req = backend.expectOne(`${API}/contas/1/transacoes/10`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ categoriaId: 5, descricao: 'Novo texto' });
    req.flush({});

    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('não envia com a descrição vazia', () => {
    responderCategorias();

    preencher('descricao', '');
    salvar();

    backend.expectNone((r) => r.method === 'PATCH');
    expect(texto()).toContain('Informe a descrição.');
  });

  it('400 com campos aponta a mensagem para o campo certo', () => {
    responderCategorias();

    salvar();
    backend.expectOne(`${API}/contas/1/transacoes/10`).flush(
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

  it('422 aparece como mensagem geral e o diálogo continua aberto', () => {
    responderCategorias();

    salvar();
    backend.expectOne(`${API}/contas/1/transacoes/10`).flush(
      { status: 422, mensagem: 'A categoria deve ser do mesmo tipo da transação.', timestamp: 'x', campos: [] },
      { status: 422, statusText: 'erro' },
    );
    fixture.detectChanges();

    expect(tela().querySelector('.erro')!.textContent).toContain('mesmo tipo da transação');
    expect(ref.close).not.toHaveBeenCalled();
  });

  it('cancelar fecha sem alterar nada', () => {
    responderCategorias();

    tela().querySelector<HTMLButtonElement>('button.cancelar')!.click();

    expect(ref.close).toHaveBeenCalledWith();
    backend.expectNone((r) => r.method === 'PATCH');
  });
});
