// Contratos de autenticação da API (POST /auth/login e POST /usuarios).
export interface LoginRequest {
  email: string;
  senha: string;
}

export interface CadastroRequest {
  nome: string;
  email: string;
  senha: string;
}

export interface TokenResponse {
  token: string;
  tipo: string;
  expiraEmSegundos: number;
}

// A senha nunca é devolvida pela API. O e-mail volta normalizado (minúsculas), então pode diferir do digitado.
export interface UsuarioResponse {
  id: number;
  nome: string;
  email: string;
}
