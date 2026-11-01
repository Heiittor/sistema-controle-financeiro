import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ValorAnimado } from './valor-animado';
import { MOVIMENTO_REDUZIDO, QUADRO_ANIMACAO } from './movimento';

describe('Valor animado', () => {
  let proximo: FrameRequestCallback;
  const reduzido = signal(false);
  const agendar = vi.fn((callback: FrameRequestCallback) => {
    proximo = callback;
    return 42;
  });
  const cancelar = vi.fn();
  beforeEach(() => {
    reduzido.set(false);
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      imports: [ValorAnimado],
      providers: [
        { provide: MOVIMENTO_REDUZIDO, useValue: reduzido },
        { provide: QUADRO_ANIMACAO, useValue: { agendar, cancelar, agora: () => 0 } },
      ],
    });
  });
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });
  function criar(valor: number) {
    const f = TestBed.createComponent(ValorAnimado);
    f.componentRef.setInput('valor', valor);
    f.detectChanges();
    return f;
  }
  it('interpola visualmente com ease-out e termina no valor exato da API', () => {
    const f = criar(1234.56);
    const texto = () => f.nativeElement.textContent.replace(/\s/g, ' ');
    expect(texto()).toContain('R$ 0,00');
    expect(
      f.nativeElement.querySelector('[role="img"]').getAttribute('aria-label').replace(/\s/g, ' '),
    ).toBe('R$ 1.234,56');
    proximo(500);
    f.detectChanges();
    expect(texto()).toContain('R$ 1.080,24');
    proximo(1000);
    f.detectChanges();
    expect(texto()).toContain('R$ 1.234,56');
  });
  it('mostra valores negativos corretamente e cancela ao destruir', () => {
    const f = criar(-200);
    proximo(500);
    f.detectChanges();
    expect(f.nativeElement.textContent.replace(/\s/g, ' ')).toContain('-R$ 175,00');
    f.destroy();
    expect(cancelar).toHaveBeenCalledWith(42);
  });
  it('não agenda animação com movimento reduzido nem para zero', () => {
    reduzido.set(true);
    const f = criar(15.5);
    expect(f.nativeElement.textContent.replace(/\s/g, ' ')).toContain('R$ 15,50');
    expect(agendar).not.toHaveBeenCalled();
    f.componentRef.setInput('valor', 0);
    f.detectChanges();
    expect(agendar).not.toHaveBeenCalled();
  });
  it('interrompe e apresenta o destino se a preferência mudar durante a animação', () => {
    const f = criar(500);
    reduzido.set(true);
    f.detectChanges();
    expect(cancelar).toHaveBeenCalledWith(42);
    expect(f.nativeElement.textContent.replace(/\s/g, ' ')).toContain('R$ 500,00');
  });
});
