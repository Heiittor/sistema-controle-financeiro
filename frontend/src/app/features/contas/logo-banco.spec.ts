import { TestBed } from '@angular/core/testing';
import { describe, it, expect } from 'vitest';
import { LogoBanco } from './logo-banco';

describe('Logotipo da conta', () => {
  it('resolve o logotipo pelo nome salvo, inclusive alias e acento', () => {
    const f = TestBed.createComponent(LogoBanco);
    f.componentRef.setInput('nome', 'itau');
    f.detectChanges();
    expect(f.nativeElement.querySelector('img').getAttribute('src')).toBe('/bancos/itau.svg');
    expect(f.nativeElement.querySelector('img').getAttribute('alt')).toBe('');
  });
  it('usa ícone genérico para conta personalizada', () => {
    const f = TestBed.createComponent(LogoBanco);
    f.componentRef.setInput('nome', 'Carteira pessoal');
    f.detectChanges();
    expect(f.nativeElement.querySelector('img')).toBeNull();
    expect(f.nativeElement.textContent).toContain('account_balance_wallet');
  });
  it('trata falha da imagem e carrega a marca seguinte quando o nome mudar', () => {
    const f = TestBed.createComponent(LogoBanco);
    f.componentRef.setInput('nome', 'Nubank');
    f.detectChanges();
    f.nativeElement.querySelector('img').dispatchEvent(new Event('error'));
    f.detectChanges();
    expect(f.nativeElement.querySelector('img')).toBeNull();
    f.componentRef.setInput('nome', 'Bradesco');
    f.detectChanges();
    expect(f.nativeElement.querySelector('img').getAttribute('src')).toBe('/bancos/bradesco.svg');
  });
});
