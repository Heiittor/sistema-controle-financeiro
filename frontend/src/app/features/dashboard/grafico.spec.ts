import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Grafico, CRIAR_GRAFICO } from './grafico';

describe('Ciclo de vida do gráfico', () => {
  const destruir = vi.fn();
  const destacar = vi.fn();
  const criar = vi.fn(() => ({ destroy: destruir, setActiveElements: destacar, update: vi.fn() }));
  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      imports: [Grafico],
      providers: [{ provide: CRIAR_GRAFICO, useValue: criar }],
    });
  });
  it('destrói o gráfico anterior ao trocar dados e ao sair da tela', () => {
    const fixture = TestBed.createComponent(Grafico);
    fixture.componentRef.setInput('configuracao', { type: 'line', data: { datasets: [] } });
    fixture.componentRef.setInput('descricao', 'Receitas e despesas');
    fixture.detectChanges();
    expect(criar).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('canvas').getAttribute('aria-label')).toBe(
      'Receitas e despesas',
    );
    fixture.componentRef.setInput('configuracao', { type: 'doughnut', data: { datasets: [] } });
    fixture.componentRef.setInput('destaque', 1);
    fixture.detectChanges();
    expect(destruir).toHaveBeenCalledTimes(1);
    expect(destacar).toHaveBeenCalledWith([{ datasetIndex: 0, index: 1 }]);
    fixture.destroy();
    expect(destruir).toHaveBeenCalledTimes(2);
  });
  it('destaca uma categoria sem recriar o gráfico ou repetir a entrada', () => {
    const f = TestBed.createComponent(Grafico);
    f.componentRef.setInput('configuracao', { type: 'doughnut', data: { datasets: [] } });
    f.componentRef.setInput('descricao', 'Categorias');
    f.detectChanges();
    f.componentRef.setInput('destaque', 0);
    f.detectChanges();
    expect(criar).toHaveBeenCalledTimes(1);
    expect(destacar).toHaveBeenCalledWith([{ datasetIndex: 0, index: 0 }]);
    f.componentRef.setInput('destaque', null);
    f.detectChanges();
    expect(destacar).toHaveBeenLastCalledWith([]);
  });
});
