import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { Contas } from './contas';

const API = 'http://api.teste';

const corpoDeErro = (status: number, mensagem: string, campos: { campo: string; mensagem: string }[] = []) => ({
  corpo: { status, mensagem, timestamp: 'x', campos },
  opcoes: { status, statusText: 'erro' },
});

describe('Contas', () => {
  let fixture: ComponentFixture<Contas>;
  let backend: HttpTestingController;

  // O Intl usa espaço não separável entre "R$" e o número. Normalizamos para comparar.
  const texto = () => (fixture.nativeElement as HTMLElement).textContent!.replace(/\s/g, ' ');

  function digitarNome(valor: string): void {
    const input = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('input')!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  function enviar(): void {
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  // O GET da lista sai no construtor, então já está pendente quando o componente é criado.
  function responderLista(contas: unknown[]): void {
    backend.expectOne(`${API}/contas`).flush(contas);
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Contas],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: API }],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Contas);
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  it('mostra o estado de carregamento antes da resposta', () => {
    expect(texto()).toContain('Carregando contas');
    responderLista([]);
  });

  it('mostra as contas com o saldo formatado', () => {
    responderLista([
      { id: 1, nome: 'Nubank', saldo: 1234.5 },
      { id: 2, nome: 'Itaú', saldo: 0 },
    ]);

    expect(texto()).toContain('Nubank');
    expect(texto()).toContain('R$ 1.234,50');
    expect(texto()).toContain('R$ 0,00');
  });

  it('mostra o estado vazio quando não há contas', () => {
    responderLista([]);

    expect(texto()).toContain('Você ainda não tem contas');
  });

  it('em erro mostra a mensagem da API e permite tentar novamente', () => {
    const { corpo, opcoes } = corpoDeErro(500, 'Erro interno. Tente novamente mais tarde.');
    backend.expectOne(`${API}/contas`).flush(corpo, opcoes);
    fixture.detectChanges();
    expect(texto()).toContain('Erro interno. Tente novamente mais tarde.');

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.mat-mdc-outlined-button, button[type="button"]')!.click();
    responderLista([]);

    expect(texto()).toContain('Você ainda não tem contas');
  });

  it('não envia com o nome vazio', () => {
    responderLista([]);

    enviar();

    backend.expectNone(`${API}/contas`);
    expect(texto()).toContain('Informe o nome da conta.');
  });

  it('criar envia o nome, avisa o sucesso e recarrega a lista pela API', () => {
    responderLista([]);

    digitarNome('Nubank');
    enviar();
    const post = backend.expectOne(`${API}/contas`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ nome: 'Nubank' });
    post.flush({ id: 1, nome: 'Nubank', saldo: 0 });

    // A conta só aparece quando a lista volta da API.
    expect(texto()).not.toContain('Nubank');
    responderLista([{ id: 1, nome: 'Nubank', saldo: 0 }]);

    expect(texto()).toContain('Conta criada.');
    expect(texto()).toContain('Nubank');
    expect(texto()).not.toContain('Informe o nome da conta.');
  });

  it('409 mostra a mensagem no campo e não recarrega a lista', () => {
    responderLista([]);

    digitarNome('Nubank');
    enviar();
    const { corpo, opcoes } = corpoDeErro(409, 'Você já tem uma conta com esse nome.');
    backend.expectOne(`${API}/contas`).flush(corpo, opcoes);
    fixture.detectChanges();

    expect(texto()).toContain('Você já tem uma conta com esse nome.');
    backend.expectNone(`${API}/contas`);
  });
});
