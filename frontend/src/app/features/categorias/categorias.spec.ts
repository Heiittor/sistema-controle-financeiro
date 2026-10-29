import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { Categorias } from './categorias';

const API = 'http://api.teste';

const corpoDeErro = (status: number, mensagem: string) => ({
  corpo: { status, mensagem, timestamp: 'x', campos: [] },
  opcoes: { status, statusText: 'erro' },
});

describe('Categorias', () => {
  let fixture: ComponentFixture<Categorias>;
  let backend: HttpTestingController;

  const tela = () => fixture.nativeElement as HTMLElement;
  const texto = () => tela().textContent!.replace(/\s/g, ' ');
  const itens = () => Array.from(tela().querySelectorAll('ul.categorias li')).map((li) => li.textContent!);

  function digitarNome(valor: string): void {
    const input = tela().querySelector<HTMLInputElement>('input')!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  function escolher(seletor: string, valor: string): void {
    const select = tela().querySelector<HTMLSelectElement>(seletor)!;
    select.value = valor;
    select.dispatchEvent(new Event('change'));
  }

  function enviar(): void {
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  // O GET da lista sai no construtor, então já está pendente quando o componente é criado.
  function responderLista(categorias: unknown[], consulta = ''): void {
    backend.expectOne(`${API}/categorias${consulta}`).flush(categorias);
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Categorias],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: API }],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Categorias);
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  it('mostra o estado de carregamento antes da resposta', () => {
    expect(texto()).toContain('Carregando categorias');
    responderLista([]);
  });

  it('mostra as categorias com o tipo escrito', () => {
    responderLista([
      { id: 1, nome: 'Salário', tipo: 'RECEITA' },
      { id: 2, nome: 'Mercado', tipo: 'DESPESA' },
    ]);

    expect(itens()).toHaveLength(2);
    expect(itens()[0]).toContain('Salário');
    expect(itens()[0]).toContain('Receita');
    expect(itens()[1]).toContain('Mercado');
    expect(itens()[1]).toContain('Despesa');
  });

  it('mostra o estado vazio quando não há categorias', () => {
    responderLista([]);

    expect(texto()).toContain('Você ainda não tem categorias');
  });

  it('o filtro consulta a API pelo tipo e avisa quando não há nenhuma', () => {
    responderLista([{ id: 1, nome: 'Salário', tipo: 'RECEITA' }]);

    escolher('select.filtro', 'DESPESA');
    responderLista([], '?tipo=DESPESA');

    expect(texto()).toContain('Nenhuma categoria deste tipo.');
  });

  it('em erro mostra a mensagem da API e permite tentar novamente', () => {
    const { corpo, opcoes } = corpoDeErro(500, 'Erro interno. Tente novamente mais tarde.');
    backend.expectOne(`${API}/categorias`).flush(corpo, opcoes);
    fixture.detectChanges();
    expect(texto()).toContain('Erro interno. Tente novamente mais tarde.');

    tela().querySelector<HTMLButtonElement>('button.tentar')!.click();
    responderLista([]);

    expect(texto()).toContain('Você ainda não tem categorias');
  });

  it('não envia o formulário vazio e mostra os erros', () => {
    responderLista([]);

    enviar();

    backend.expectNone(`${API}/categorias`);
    expect(texto()).toContain('Informe o nome da categoria.');
    expect(texto()).toContain('Escolha o tipo.');
  });

  it('criar envia nome e tipo, avisa o sucesso e recarrega a lista pela API', () => {
    responderLista([]);

    digitarNome('Salário');
    escolher('select[formControlName="tipo"]', 'RECEITA');
    enviar();
    const post = backend.expectOne(`${API}/categorias`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ nome: 'Salário', tipo: 'RECEITA' });
    post.flush({ id: 1, nome: 'Salário', tipo: 'RECEITA' });

    // A categoria só aparece quando a lista volta da API.
    responderLista([{ id: 1, nome: 'Salário', tipo: 'RECEITA' }]);

    expect(texto()).toContain('Categoria criada.');
    expect(itens()[0]).toContain('Salário');
  });

  it('409 mostra a mensagem no campo nome e não recarrega a lista', () => {
    responderLista([]);

    digitarNome('Salário');
    escolher('select[formControlName="tipo"]', 'RECEITA');
    enviar();
    const { corpo, opcoes } = corpoDeErro(409, 'Você já tem uma categoria com esse nome e tipo.');
    backend.expectOne(`${API}/categorias`).flush(corpo, opcoes);
    fixture.detectChanges();

    expect(texto()).toContain('Você já tem uma categoria com esse nome e tipo.');
    backend.expectNone(`${API}/categorias`);
  });
});
