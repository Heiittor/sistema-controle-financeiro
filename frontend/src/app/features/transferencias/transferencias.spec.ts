import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { Transferencias } from './transferencias';
import { TransferenciasService } from './transferencias.service';
import { TransferenciaHistorico } from './transferencia.models';
import { Pagina } from '../../core/api/pagina';
import { of, Subject, throwError } from 'rxjs';

const API = 'http://api.teste';
const CONTAS = [
  { id: 1, nome: 'Nubank', saldo: 1000 },
  { id: 2, nome: 'Itaú', saldo: 0 },
];
const paginaHistorico = (
  conteudo: TransferenciaHistorico[] = [],
  pagina = 0,
  total = conteudo.length,
): Pagina<TransferenciaHistorico> => ({
  conteudo,
  pagina,
  tamanho: 5,
  totalElementos: total,
  totalPaginas: Math.ceil(total / 5),
});
const transferenciaHistorica: TransferenciaHistorico = {
  transferenciaId: 'historica',
  contaOrigemId: 1,
  contaOrigemNome: 'Nome da origem na API',
  contaDestinoId: 2,
  contaDestinoNome: 'Nome do destino na API',
  descricao: 'Reserva antiga',
  valor: 1206.35,
  data: '2020-01-01',
};

describe('Transferencias', () => {
  let fixture: ComponentFixture<Transferencias>;
  let backend: HttpTestingController;

  const tela = () => fixture.nativeElement as HTMLElement;
  // O Intl usa espaço não separável entre "R$" e o número. Normalizamos para comparar.
  const texto = () => tela().textContent!.replace(/\s/g, ' ');
  const campo = <T extends HTMLElement>(nome: string) =>
    tela().querySelector<T>(`[formControlName="${nome}"]`)!;

  function preencher(nome: string, valor: string, evento = 'input'): void {
    const el = campo<HTMLInputElement | HTMLSelectElement>(nome);
    el.value = valor;
    el.dispatchEvent(new Event(evento));
  }

  function enviar(): void {
    tela().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function responderContas(contas: unknown[]): void {
    backend.expectOne(`${API}/contas`).flush(contas);
    fixture.detectChanges();
  }

  function preencherValido(valor = '100,50'): void {
    preencher('contaOrigemId', '1', 'change');
    preencher('contaDestinoId', '2', 'change');
    preencher('descricao', 'Reserva');
    preencher('valor', valor);
  }

  const erroApi = (
    status: number,
    mensagem: string,
    campos: { campo: string; mensagem: string }[] = [],
  ) => ({
    corpo: { status, mensagem, timestamp: 'x', campos },
    opcoes: { status, statusText: 'erro' },
  });

  beforeEach(() => {
    // 15:00 UTC de 22/10/2026 = 12:00 em São Paulo.
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-10-22T15:00:00Z'));
    TestBed.configureTestingModule({
      imports: [Transferencias],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_URL, useValue: API },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(TransferenciasService), 'listar').mockReturnValue(
      of(paginaHistorico()),
    );
    fixture = TestBed.createComponent(Transferencias);
    fixture.detectChanges();
  });

  afterEach(() => {
    backend.verify();
    vi.useRealTimers();
  });

  it('mostra os saldos vindos da API, formatados', () => {
    responderContas(CONTAS);

    expect(texto()).toContain('Nubank');
    expect(texto()).toContain('R$ 1.000,00');
    expect(texto()).toContain('Itaú');
    expect(tela().querySelectorAll('.contas app-logo-banco')).toHaveLength(2);
    expect(tela().querySelector('.transferencia')).toBeNull();
    expect(tela().querySelector('.ver-movimentacoes')?.getAttribute('href')).toBe('/movimentacoes');
  });

  it('com menos de duas contas, orienta a criar outra e não mostra o formulário', () => {
    responderContas([CONTAS[0]]);

    expect(texto()).toContain('pelo menos duas contas');
    expect(tela().querySelector('a[href="/contas"]')).not.toBeNull();
    expect(tela().querySelector('form')).toBeNull();
  });

  it('não envia o formulário vazio e mostra os erros', () => {
    responderContas(CONTAS);

    enviar();

    backend.expectNone((r) => r.method === 'POST');
    expect(texto()).toContain('Escolha a conta de origem.');
    expect(texto()).toContain('Escolha a conta de destino.');
    expect(texto()).toContain('Informe a descrição.');
    expect(texto()).toContain('Informe o valor.');
  });

  it('bloqueia origem igual ao destino, sem chamar a API', () => {
    responderContas(CONTAS);

    preencher('contaOrigemId', '1', 'change');
    preencher('contaDestinoId', '1', 'change');
    preencher('descricao', 'Reserva');
    preencher('valor', '10');
    enviar();

    backend.expectNone((r) => r.method === 'POST');
    expect(texto()).toContain('A conta de destino deve ser diferente da de origem.');
  });

  it('trocar a origem reavalia o destino', () => {
    responderContas(CONTAS);

    preencher('contaOrigemId', '1', 'change');
    preencher('contaDestinoId', '1', 'change');
    fixture.detectChanges();

    expect(campo<HTMLSelectElement>('contaDestinoId').classList.contains('ng-invalid')).toBe(true);

    preencher('contaOrigemId', '2', 'change');
    fixture.detectChanges();

    expect(campo<HTMLSelectElement>('contaDestinoId').classList.contains('ng-invalid')).toBe(false);

    preencher('contaOrigemId', '1', 'change');
    fixture.detectChanges();

    expect(campo<HTMLSelectElement>('contaDestinoId').classList.contains('ng-invalid')).toBe(true);

    preencher('contaDestinoId', '2', 'change');
    fixture.detectChanges();

    expect(campo<HTMLSelectElement>('contaDestinoId').classList.contains('ng-invalid')).toBe(false);
  });

  it('envia valor brasileiro como número no contrato de transferência', () => {
    responderContas(CONTAS);
    preencherValido('1.206,35');
    enviar();
    const req = backend.expectOne(`${API}/transferencias`);
    expect(req.request.body.valor).toBe(1206.35);
    const { corpo, opcoes } = erroApi(422, 'Saldo insuficiente.');
    req.flush(corpo, opcoes);
    fixture.detectChanges();
    expect(campo<HTMLInputElement>('valor').value).toBe('1.206,35');
  });

  it('recusa valor com mais de 2 casas, sem chamar a API', () => {
    responderContas(CONTAS);

    preencherValido('10,555');
    enviar();

    backend.expectNone((r) => r.method === 'POST');
    expect(texto()).toContain('Use apenas números');
  });

  it('bloqueia valor que perderia centavos e explica o motivo', () => {
    responderContas(CONTAS);
    preencherValido('999999999999999,99');
    enviar();

    backend.expectNone((r) => r.method === 'POST');
    expect(texto()).toContain('Este valor não pode ser enviado sem perda de centavos.');
  });

  it('transfere com UMA requisição, mostra o que a API devolveu e recarrega os saldos pela API', () => {
    responderContas(CONTAS);

    preencherValido('100,50');
    enviar();

    const post = backend.expectOne(`${API}/transferencias`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({
      contaOrigemId: 1,
      contaDestinoId: 2,
      descricao: 'Reserva',
      valor: 100.5,
      data: '2026-10-22',
    });
    post.flush({
      transferenciaId: 'abc',
      contaOrigemId: 1,
      contaDestinoId: 2,
      descricao: 'Reserva',
      valor: 100.5,
      data: '2026-10-22',
    });

    // Os saldos novos só aparecem quando a API os devolve: o front não calculou nada.
    responderContas([
      { id: 1, nome: 'Nubank', saldo: 899.5 },
      { id: 2, nome: 'Itaú', saldo: 100.5 },
    ]);

    expect(texto()).toContain(
      'Transferência realizada: R$ 100,50 de Nubank para Itaú, em 22/10/2026.',
    );
    expect(texto()).toContain('R$ 899,50');
    expect(campo<HTMLInputElement>('descricao').value).toBe('');
    expect(campo<HTMLInputElement>('valor').value).toBe('');
    expect(TestBed.inject(TransferenciasService).listar).toHaveBeenCalledTimes(2);
    // O POST não é inserido localmente no histórico: só o GET é a fonte da lista.
    expect(tela().querySelector('.transferencia')).toBeNull();
  });

  it('422 (saldo insuficiente) aparece como mensagem geral, mantém o formulário e não recarrega os saldos', () => {
    responderContas(CONTAS);

    preencherValido('99999');
    enviar();
    const { corpo, opcoes } = erroApi(422, 'Saldo insuficiente.');
    backend.expectOne(`${API}/transferencias`).flush(corpo, opcoes);
    fixture.detectChanges();

    expect(tela().querySelector('.erro')!.textContent).toContain('Saldo insuficiente.');
    expect(campo<HTMLInputElement>('descricao').value).toBe('Reserva');
    backend.expectNone(`${API}/contas`);
  });

  it('409 (conta alterada por outra operação) vira mensagem geral', () => {
    responderContas(CONTAS);

    preencherValido();
    enviar();
    const { corpo, opcoes } = erroApi(
      409,
      'A conta foi alterada por outra operação. Tente novamente.',
    );
    backend.expectOne(`${API}/transferencias`).flush(corpo, opcoes);
    fixture.detectChanges();

    expect(tela().querySelector('.erro')!.textContent).toContain('alterada por outra operação');
  });

  it('400 com campos aponta a mensagem para o campo certo', () => {
    responderContas(CONTAS);

    preencherValido();
    enviar();
    const { corpo, opcoes } = erroApi(400, 'Dados inválidos.', [
      { campo: 'valor', mensagem: 'O valor deve ser maior que zero.' },
    ]);
    backend.expectOne(`${API}/transferencias`).flush(corpo, opcoes);
    fixture.detectChanges();

    expect(texto()).toContain('O valor deve ser maior que zero.');
    expect(tela().querySelector('.erro')).toBeNull();
  });

  it('não duplica o envio enquanto a requisição está pendente', () => {
    responderContas(CONTAS);

    preencherValido();
    enviar();
    enviar();

    // expectOne falha se houvesse duas requisições.
    const post = backend.expectOne(`${API}/transferencias`);
    expect(tela().querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);
    post.flush({
      transferenciaId: 'abc',
      contaOrigemId: 1,
      contaDestinoId: 2,
      descricao: 'Reserva',
      valor: 100.5,
      data: '2026-10-22',
    });
    responderContas(CONTAS);
  });

  function filtrarHistorico(conta: string): void {
    const select = tela().querySelector<HTMLSelectElement>('.filtro-historico select')!;
    select.value = conta;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  it('carrega ao entrar, mostra transferências antigas e pagina mantendo o filtro independente', () => {
    responderContas(CONTAS);
    const listar = vi.mocked(TestBed.inject(TransferenciasService).listar);
    expect(listar).toHaveBeenCalledWith(null, 0);
    listar.mockReturnValueOnce(of(paginaHistorico([transferenciaHistorica], 0, 6)));
    filtrarHistorico('1');
    expect(texto()).toContain('01/01/2020');
    expect(texto()).toContain('Nome da origem na API');
    expect(texto()).toContain('Nome do destino na API');
    expect(texto()).toContain('R$ 1.206,35');
    preencher('contaOrigemId', '2', 'change');
    listar.mockReturnValueOnce(
      of(paginaHistorico([{ ...transferenciaHistorica, transferenciaId: 'pagina2' }], 1, 6)),
    );
    tela().querySelector<HTMLButtonElement>('.proxima-historico')!.click();
    fixture.detectChanges();
    expect(listar).toHaveBeenLastCalledWith(1, 1);
    expect(texto()).toContain('Página 2 de 2');
    expect(tela().querySelector<HTMLButtonElement>('.proxima-historico')!.disabled).toBe(true);
    listar.mockReturnValueOnce(of(paginaHistorico()));
    filtrarHistorico('');
    expect(listar).toHaveBeenLastCalledWith(null, 0);
  });

  it('histórico tem erro e nova tentativa independentes do formulário', () => {
    responderContas(CONTAS);
    const listar = vi.mocked(TestBed.inject(TransferenciasService).listar);
    listar.mockReturnValueOnce(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 500,
            error: { status: 500, mensagem: 'Falha no histórico.', timestamp: 'x', campos: [] },
          }),
      ),
    );
    filtrarHistorico('1');
    expect(texto()).toContain('Falha no histórico.');
    expect(tela().querySelector('form')).not.toBeNull();
    listar.mockReturnValueOnce(of(paginaHistorico([transferenciaHistorica])));
    tela().querySelector<HTMLButtonElement>('.tentar-historico')!.click();
    fixture.detectChanges();
    expect(texto()).toContain('Reserva antiga');
    expect(listar).toHaveBeenLastCalledWith(1, 0);
  });

  it('ignora resultado e erro atrasados após mudar o filtro', () => {
    responderContas(CONTAS);
    const primeira = new Subject<Pagina<TransferenciaHistorico>>();
    const segunda = new Subject<Pagina<TransferenciaHistorico>>();
    const atual = new Subject<Pagina<TransferenciaHistorico>>();
    vi.mocked(TestBed.inject(TransferenciasService).listar)
      .mockReturnValueOnce(primeira)
      .mockReturnValueOnce(segunda)
      .mockReturnValueOnce(atual);
    filtrarHistorico('1');
    filtrarHistorico('2');
    filtrarHistorico('');
    expect(texto()).toContain('Carregando histórico');
    atual.next(paginaHistorico([transferenciaHistorica]));
    primeira.next(paginaHistorico());
    segunda.error(new Error('Erro antigo'));
    fixture.detectChanges();
    expect(texto()).toContain('Reserva antiga');
    expect(texto()).not.toContain('Erro antigo');
  });
});
