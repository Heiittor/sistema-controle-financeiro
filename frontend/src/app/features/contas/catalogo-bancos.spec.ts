import { describe, it, expect } from 'vitest';
import { BANCOS, bancoDoNome, filtrarBancos } from './catalogo-bancos';

describe('Catálogo visual de bancos', () => {
  it('contém dez instituições distintas com caminhos locais', () => {
    expect(BANCOS).toHaveLength(10);
    expect(new Set(BANCOS.map((b) => b.nome)).size).toBe(10);
    expect(
      BANCOS.every((b) => b.logotipo.startsWith('/bancos/') && b.logotipo.endsWith('.svg')),
    ).toBe(true);
  });
  it('filtra por nome, aliases, caixa e acentos', () => {
    expect(filtrarBancos('ITAÚ').map((b) => b.nome)).toEqual(['Itaú']);
    expect(filtrarBancos('economica').map((b) => b.nome)).toEqual(['Caixa Econômica Federal']);
    expect(filtrarBancos('  BB  ').map((b) => b.nome)).toEqual(['Banco do Brasil']);
    expect(filtrarBancos('b')).toContainEqual(BANCOS.find((b) => b.nome === 'Bradesco'));
  });
  it('reconhece aliases exatos sem classificar nomes personalizados por aproximação', () => {
    expect(bancoDoNome('  ITAU  UNIBANCO ')?.nome).toBe('Itaú');
    expect(bancoDoNome('nu')?.nome).toBe('Nubank');
    expect(bancoDoNome('Minha carteira')).toBeUndefined();
    expect(bancoDoNome('Nubank reserva pessoal')).toBeUndefined();
  });
  it('oferece todo o catálogo vazio e nenhuma sugestão para nome sem correspondência', () => {
    expect(filtrarBancos('')).toHaveLength(10);
    expect(filtrarBancos('Carteira pessoal')).toEqual([]);
    expect(bancoDoNome(null)).toBeUndefined();
  });
});
