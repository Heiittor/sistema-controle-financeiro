export interface BancoVisual {
  nome: string;
  aliases: readonly string[];
  logotipo: string;
}

export const BANCOS: readonly BancoVisual[] = [
  {
    nome: 'Banco do Brasil',
    aliases: ['bb', 'banco do brasil', 'banco brasil'],
    logotipo: '/bancos/banco-do-brasil.svg',
  },
  { nome: 'Bradesco', aliases: ['bradesco', 'banco bradesco'], logotipo: '/bancos/bradesco.svg' },
  {
    nome: 'Banco Inter',
    aliases: ['inter', 'banco inter', 'intermedium'],
    logotipo: '/bancos/inter.svg',
  },
  {
    nome: 'BTG Pactual',
    aliases: ['btg', 'btg pactual', 'banco btg'],
    logotipo: '/bancos/btg-pactual.svg',
  },
  {
    nome: 'Banrisul',
    aliases: ['banrisul', 'banco do estado do rio grande do sul'],
    logotipo: '/bancos/banrisul.svg',
  },
  {
    nome: 'Caixa Econômica Federal',
    aliases: ['caixa', 'cef', 'caixa economica', 'caixa economica federal'],
    logotipo: '/bancos/caixa.svg',
  },
  { nome: 'Itaú', aliases: ['itau', 'itau unibanco', 'banco itau'], logotipo: '/bancos/itau.svg' },
  { nome: 'Nubank', aliases: ['nu', 'nubank', 'nu pagamentos'], logotipo: '/bancos/nubank.svg' },
  {
    nome: 'Santander',
    aliases: ['santander', 'banco santander', 'santander brasil'],
    logotipo: '/bancos/santander.svg',
  },
  {
    nome: 'Sicredi',
    aliases: ['sicredi', 'banco cooperativo sicredi'],
    logotipo: '/bancos/sicredi.svg',
  },
];

export function normalizarBanco(nome: string | null | undefined): string {
  return (nome ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('pt-BR')
    .trim()
    .replace(/\s+/g, ' ');
}

export function filtrarBancos(consulta: string | null | undefined): readonly BancoVisual[] {
  const termo = normalizarBanco(consulta);
  return BANCOS.filter((b) =>
    [b.nome, ...b.aliases].some((nome) => normalizarBanco(nome).includes(termo)),
  );
}

export function bancoDoNome(nome: string | null | undefined): BancoVisual | undefined {
  const termo = normalizarBanco(nome);
  return BANCOS.find((b) =>
    [b.nome, ...b.aliases].some((alias) => normalizarBanco(alias) === termo),
  );
}
