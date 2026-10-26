import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { NaoEncontrada } from './nao-encontrada';

describe('NaoEncontrada', () => {
  let tela: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [NaoEncontrada], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(NaoEncontrada);
    fixture.detectChanges();
    tela = fixture.nativeElement;
  });

  it('explica que o endereço não existe', () => {
    expect(tela.querySelector('h1')?.textContent).toContain('Página não encontrada');
  });

  it('oferece o caminho de volta ao início', () => {
    expect(tela.querySelector('a[href="/"]')).not.toBeNull();
  });
});
