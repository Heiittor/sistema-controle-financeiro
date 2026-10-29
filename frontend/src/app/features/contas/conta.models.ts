// Contrato de GET /contas e POST /contas. O saldo é calculado pelo backend: o front só exibe.
export interface Conta {
  id: number;
  nome: string;
  saldo: number;
}
