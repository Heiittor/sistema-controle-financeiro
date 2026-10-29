import { HttpRequest } from '@angular/common/http';

// A URL pertence à nossa API? Comparamos com "apiUrl/" para que "http://api.teste.evil.com"
// não passe por "http://api.teste" e receba o token.
export function ehDaApi(url: string, apiUrl: string): boolean {
  return url.startsWith(`${apiUrl}/`);
}

const ROTAS_PUBLICAS = ['/auth/login', '/usuarios'];

// Login e cadastro são públicos: não levam token, e um 401 deles nunca significa sessão expirada.
export function ehRotaPublica(req: HttpRequest<unknown>, apiUrl: string): boolean {
  if (!ehDaApi(req.url, apiUrl)) {
    return false;
  }
  const caminho = req.url.slice(apiUrl.length).split('?')[0];
  return req.method === 'POST' && ROTAS_PUBLICAS.includes(caminho);
}
