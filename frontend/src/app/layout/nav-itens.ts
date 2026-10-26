// Única fonte do menu. O ícone é o nome do glifo da fonte Material Icons.
export interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
}

export const ITENS_MENU: readonly ItemMenu[] = [
  { rota: '/', rotulo: 'Dashboard', icone: 'dashboard' },
  { rota: '/contas', rotulo: 'Contas', icone: 'account_balance_wallet' },
  { rota: '/movimentacoes', rotulo: 'Movimentações', icone: 'receipt_long' },
  { rota: '/categorias', rotulo: 'Categorias', icone: 'category' },
  { rota: '/transferencias', rotulo: 'Transferências', icone: 'swap_horiz' },
  { rota: '/orcamentos', rotulo: 'Orçamentos', icone: 'savings' },
];
