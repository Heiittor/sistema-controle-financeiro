import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { mensagemDeErro, segundosDeEspera } from './mensagem-erro';

describe('mensagemDeErro', () => {
  it('usa a mensagem da API quando o erro segue o formato dela', () => {
    const erro = new HttpErrorResponse({
      status: 422,
      error: { status: 422, mensagem: 'Saldo insuficiente.', timestamp: 'x', campos: [] },
    });

    expect(mensagemDeErro(erro)).toBe('Saldo insuficiente.');
  });

  it('explica a falta de conexão quando o status é 0', () => {
    const erro = new HttpErrorResponse({ status: 0 });

    expect(mensagemDeErro(erro)).toContain('conectar ao servidor');
  });

  it('usa mensagem genérica quando o corpo foge do formato', () => {
    const erro = new HttpErrorResponse({ status: 502, error: '<html>Bad Gateway</html>' });

    expect(mensagemDeErro(erro)).toBe('Ocorreu um erro inesperado. Tente novamente.');
  });

  it('usa mensagem genérica para erros que não vieram do HTTP', () => {
    expect(mensagemDeErro(new Error('detalhe técnico'))).toBe(
      'Ocorreu um erro inesperado. Tente novamente.',
    );
  });
});

describe('segundosDeEspera', () => {
  it('lê o Retry-After em segundos', () => {
    const erro = new HttpErrorResponse({
      status: 429,
      headers: new HttpHeaders({ 'Retry-After': '120' }),
    });

    expect(segundosDeEspera(erro)).toBe(120);
  });

  it('devolve null quando o cabeçalho não existe', () => {
    expect(segundosDeEspera(new HttpErrorResponse({ status: 429 }))).toBeNull();
  });

  it('devolve null para valor inválido, zero ou data', () => {
    for (const valor of ['abc', '0', '-5', 'Wed, 21 Oct 2026 07:28:00 GMT']) {
      const erro = new HttpErrorResponse({
        status: 429,
        headers: new HttpHeaders({ 'Retry-After': valor }),
      });
      expect(segundosDeEspera(erro)).toBeNull();
    }
  });
});
