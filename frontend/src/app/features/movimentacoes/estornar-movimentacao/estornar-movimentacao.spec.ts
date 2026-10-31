import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { API_URL } from '../../../core/api/api-url';
import { EstornarMovimentacao } from './estornar-movimentacao';

const API = 'http://api.teste';
const TRANSACAO = {
  id: 10,
  descricao: 'Mercado',
  valor: 100,
  data: '2026-10-05',
  tipo: 'DESPESA',
  contaId: 1,
  categoriaId: 5,
  categoriaNome: 'Alimentação',
  transferenciaId: null,
  estornoDeId: null,
  estornada: false,
};

const erroApi = (status: number, mensagem: string) => ({
  corpo: { status, mensagem, timestamp: 'x', campos: [] },
  opcoes: { status, statusText: 'erro' },
});

describe('EstornarMovimentacao', () => {
  let fixture: ComponentFixture<EstornarMovimentacao>;
  let backend: HttpTestingController;
  const ref = { close: vi.fn() };

  const tela = () => fixture.nativeElement as HTMLElement;
  const texto = () => tela().textContent!.replace(/\s/g, ' ');
  const botao = (classe: string) => tela().querySelector<HTMLButtonElement>(`button.${classe}`)!;

  beforeEach(() => {
    ref.close.mockClear();
    TestBed.configureTestingModule({
      imports: [EstornarMovimentacao],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: API },
        { provide: MAT_DIALOG_DATA, useValue: { contaId: 1, transacao: TRANSACAO } },
        { provide: MatDialogRef, useValue: ref },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EstornarMovimentacao);
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  it('mostra a movimentação e explica o efeito do estorno', () => {
    expect(texto()).toContain('Mercado');
    expect(texto()).toContain('Despesa');
    expect(texto()).toContain('05/10/2026');
    expect(texto()).toContain('R$ 100,00');
    expect(texto()).toContain('lançamento inverso');
    expect(texto()).toContain('Nada é apagado');
  });

  it('confirmar envia o POST sem corpo e fecha com "ok"', () => {
    botao('confirmar').click();

    const req = backend.expectOne(`${API}/contas/1/transacoes/10/estorno`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush({});

    expect(ref.close).toHaveBeenCalledWith('ok');
  });

  it('422 (saldo insuficiente) aparece no diálogo, que continua aberto', () => {
    botao('confirmar').click();
    const { corpo, opcoes } = erroApi(422, 'Saldo insuficiente.');
    backend.expectOne(`${API}/contas/1/transacoes/10/estorno`).flush(corpo, opcoes);
    fixture.detectChanges();

    expect(tela().querySelector('.erro')!.textContent).toContain('Saldo insuficiente.');
    expect(ref.close).not.toHaveBeenCalled();
    expect(botao('confirmar').disabled).toBe(false);
  });

  it('409 (já estornada) bloqueia nova tentativa e, ao fechar, pede para recarregar a lista', () => {
    botao('confirmar').click();
    const { corpo, opcoes } = erroApi(409, 'Esta transação já foi estornada.');
    backend.expectOne(`${API}/contas/1/transacoes/10/estorno`).flush(corpo, opcoes);
    fixture.detectChanges();

    expect(texto()).toContain('Esta transação já foi estornada.');
    expect(botao('confirmar').disabled).toBe(true);
    expect(botao('cancelar').textContent).toContain('Fechar');

    botao('cancelar').click();
    expect(ref.close).toHaveBeenCalledWith('recarregar');
  });

  it('cancelar fecha sem resultado e sem chamar a API', () => {
    botao('cancelar').click();

    expect(ref.close).toHaveBeenCalledWith(undefined);
    backend.expectNone((r) => r.method === 'POST');
  });
});
