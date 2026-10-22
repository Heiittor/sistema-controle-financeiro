// Espelha o ErroResponse do backend: o formato único de todo erro da API.
export interface CampoErro {
  campo: string;
  mensagem: string;
}

export interface ErroApi {
  status: number;
  mensagem: string;
  timestamp: string;
  campos: CampoErro[];
}

// O corpo de um erro HTTP chega como "unknown". Só o tratamos como ErroApi se tiver o formato esperado,
// porque erros de rede ou de proxy não seguem o nosso molde.
export function ehErroApi(corpo: unknown): corpo is ErroApi {
  if (typeof corpo !== 'object' || corpo === null) {
    return false;
  }
  const c = corpo as Record<string, unknown>;
  return (
    typeof c['status'] === 'number' &&
    typeof c['mensagem'] === 'string' &&
    Array.isArray(c['campos'])
  );
}
