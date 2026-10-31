// Contrato de POST /transferencias. O valor é sempre positivo.
export interface NovaTransferencia {
  contaOrigemId: number;
  contaDestinoId: number;
  descricao: string;
  valor: number;
  data: string;
}

// O que a API devolve. Os saldos NÃO vêm aqui: o front os consulta de novo em GET /contas.
export interface Transferencia {
  transferenciaId: string;
  contaOrigemId: number;
  contaDestinoId: number;
  descricao: string;
  valor: number;
  data: string;
}
