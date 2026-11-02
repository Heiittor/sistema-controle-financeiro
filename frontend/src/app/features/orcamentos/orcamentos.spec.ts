import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { API_URL } from '../../core/api/api-url';
import { Orcamentos } from './orcamentos';
import { formatarMes, Orcamento } from './orcamento.models';
import { FormularioOrcamento } from './formulario-orcamento';
const item: Orcamento = {
  id: 1,
  categoriaId: 2,
  categoriaNome: 'Alimentação',
  mes: '2026-11',
  limite: 1200,
  gasto: 960,
  restante: 240,
  percentualConsumido: 80,
  situacao: 'ATENCAO',
};
const pagina = (conteudo = [item], numero = 0) => ({
  conteudo,
  pagina: numero,
  tamanho: 10,
  totalElementos: 11,
  totalPaginas: 2,
  resumo: { ativos: 11, dentroDoPlanejado: 10, acimaDoLimite: 1 },
});
let fixture: ComponentFixture<Orcamentos>, http: HttpTestingController;
const tela = () => fixture.nativeElement as HTMLElement;
const texto = () => tela().textContent!.replace(/\s/g, ' ');
const consulta = () => http.expectOne((r) => r.url === '/api/orcamentos');
function mes(valor: string) {
  const input = tela().querySelector<HTMLInputElement>('input[type=month]')!;
  input.value = valor;
  input.dispatchEvent(new Event('change'));
  fixture.detectChanges();
}
beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date('2026-11-02T15:00:00Z'));
  TestBed.configureTestingModule({
    imports: [Orcamentos],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: API_URL, useValue: '/api' },
    ],
  });
  http = TestBed.inject(HttpTestingController);
  fixture = TestBed.createComponent(Orcamentos);
  fixture.detectChanges();
});
afterEach(() => {
  http.verify();
  vi.useRealTimers();
});
it('exibe os indicadores do mês inteiro e os valores da API sem somar a página', () => {
  const r = consulta();
  expect(r.request.params.get('mes')).toBe('2026-11');
  r.flush(pagina());
  fixture.detectChanges();
  expect(tela().querySelector('.indicador.ativos dd')!.textContent).toBe('11');
  expect(texto()).toContain('R$ 960,00');
  expect(texto()).toContain('Atenção ao limite');
  expect(tela().querySelector('.barra')!.getAttribute('aria-valuenow')).toBe('80');
  expect(tela().querySelector('tr.atencao')).not.toBeNull();
  expect(formatarMes('2026-11')).toBe('nov/2026');
});
it('preserva percentual acima de 100 e limita somente a largura da barra', () => {
  consulta().flush(
    pagina([{ ...item, percentualConsumido: 116.67, situacao: 'ACIMA_DO_LIMITE', restante: -50 }]),
  );
  fixture.detectChanges();
  expect(texto()).toContain('116,67%');
  expect(tela().querySelector('.barra span')!.getAttribute('style')).toContain('100%');
  expect(tela().querySelector('tr.ultrapassado')).not.toBeNull();
});
it('pagina e troca o mês voltando à primeira página; mês vazio não consulta a API', () => {
  consulta().flush(pagina());
  fixture.detectChanges();
  tela().querySelector<HTMLButtonElement>('.proxima')!.click();
  const segunda = consulta();
  expect(segunda.request.params.get('page')).toBe('1');
  segunda.flush(pagina([item], 1));
  fixture.detectChanges();
  mes('2026-10');
  const anterior = consulta();
  expect(anterior.request.params.get('page')).toBe('0');
  expect(anterior.request.params.get('mes')).toBe('2026-10');
  anterior.flush(pagina());
  fixture.detectChanges();
  mes('');
  http.expectNone((r) => r.url === '/api/orcamentos');
  expect(texto()).toContain('período anterior continua aplicado');
});
it('erro permite nova tentativa e resposta atrasada não substitui o mês atual', () => {
  const antiga = consulta();
  mes('2026-10');
  const atual = consulta();
  atual.flush(
    { status: 500, mensagem: 'Falha de consulta.', timestamp: 'x', campos: [] },
    { status: 500, statusText: 'erro' },
  );
  antiga.flush(pagina());
  fixture.detectChanges();
  expect(texto()).toContain('Falha de consulta.');
  tela().querySelector<HTMLButtonElement>('.tentar')!.click();
  consulta().flush({ ...pagina([]), totalElementos: 0, totalPaginas: 0 });
  fixture.detectChanges();
  expect(texto()).toContain('Nenhum orçamento neste mês');
});
it('abre edição com os dados corretos e somente recarrega depois de salvar', () => {
  consulta().flush(pagina());
  fixture.detectChanges();
  const abrir = vi
    .spyOn(TestBed.inject(MatDialog), 'open')
    .mockReturnValue({ afterClosed: () => of(true) } as never);
  tela().querySelector<HTMLButtonElement>('.editar')!.click();
  expect(abrir.mock.calls[0][0]).toBe(FormularioOrcamento);
  expect((abrir.mock.calls[0][1] as { data: unknown }).data).toEqual({
    mes: '2026-11',
    orcamento: item,
  });
  consulta().flush(pagina());
  fixture.detectChanges();
  abrir.mockReturnValue({ afterClosed: () => of(false) } as never);
  tela().querySelector<HTMLButtonElement>('.novo')!.click();
  http.expectNone((r) => r.url === '/api/orcamentos');
});
