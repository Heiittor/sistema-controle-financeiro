import { HttpErrorResponse } from '@angular/common/http';
import { ehErroApi } from './erro-api';

const SEM_CONEXAO = 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.';
const GENERICA = 'Ocorreu um erro inesperado. Tente novamente.';

// Texto para mostrar ao usuário. Usa a mensagem da API quando o erro segue o formato dela.
export function mensagemDeErro(erro: unknown): string {
  if (!(erro instanceof HttpErrorResponse)) {
    return GENERICA;
  }
  // Status 0: a requisição nem chegou (rede fora do ar, servidor desligado ou bloqueio de CORS).
  if (erro.status === 0) {
    return SEM_CONEXAO;
  }
  if (ehErroApi(erro.error)) {
    return erro.error.mensagem;
  }
  return GENERICA;
}

// Segundos de espera do cabeçalho Retry-After (o backend envia em segundos, no 429). Null se ausente ou inválido.
export function segundosDeEspera(erro: unknown): number | null {
  if (!(erro instanceof HttpErrorResponse)) {
    return null;
  }
  const bruto = erro.headers.get('Retry-After')?.trim();
  if (bruto === undefined || !/^\d+$/.test(bruto)) {
    return null;
  }
  const segundos = Number(bruto);
  return segundos > 0 ? segundos : null;
}
