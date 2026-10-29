import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { EmConstrucao } from './em-construcao';

describe('EmConstrucao', () => {
  it('avisa que a área ainda não existe, sem mostrar dados', () => {
    const fixture = TestBed.createComponent(EmConstrucao);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Em construção');
  });
});
