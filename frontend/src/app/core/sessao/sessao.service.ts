import { Injectable, signal } from '@angular/core';

const CHAVE = 'sessao';

interface SessaoArmazenada {
  token: string;
  // Instante absoluto de vencimento, em milissegundos.
  expiraEm: number;
}

@Injectable({ providedIn: 'root' })
export class SessaoService {
  // A memória é a fonte principal; o sessionStorage só faz a sessão sobreviver ao F5.
  private sessao: SessaoArmazenada | null = SessaoService.lerArmazenada();
  private readonly estado = signal(false);
  private readonly expirada = signal(false);

  readonly autenticado = this.estado.asReadonly();
  // true quando a sessão acabou sem o usuário pedir (vencimento ou 401). A tela de login mostra o aviso.
  readonly sessaoExpirada = this.expirada.asReadonly();

  constructor() {
    // Uma sessão restaurada do F5 pode já estar vencida: token() descarta nesse caso.
    this.estado.set(this.token() !== null);
  }

  // expiraEmSegundos vem da resposta do login. Não decodificamos o JWT.
  iniciar(token: string, expiraEmSegundos: number): void {
    this.sessao = { token, expiraEm: Date.now() + expiraEmSegundos * 1000 };
    this.estado.set(true);
    this.expirada.set(false);
    try {
      sessionStorage.setItem(CHAVE, JSON.stringify(this.sessao));
    } catch {
      // Armazenamento bloqueado: a sessão continua válida só na memória desta página.
    }
  }

  // Devolve o token válido, ou null (sem sessão ou vencido).
  token(): string | null {
    const atual = this.sessao;
    if (atual === null) {
      return null;
    }
    if (Date.now() >= atual.expiraEm) {
      this.expirar();
      return null;
    }
    return atual.token;
  }

  // Logout pedido pelo usuário: apaga tudo, sem aviso de expiração.
  encerrar(): void {
    this.sessao = null;
    this.estado.set(false);
    try {
      sessionStorage.removeItem(CHAVE);
    } catch {
      // Nada a fazer: já limpamos a memória.
    }
  }

  // Sessão invalidada sem ação do usuário. Devolve true só se havia sessão para invalidar,
  // então quem chama sabe se deve redirecionar (e não redireciona duas vezes).
  expirar(): boolean {
    const havia = this.sessao !== null;
    this.encerrar();
    if (havia) {
      this.expirada.set(true);
    }
    return havia;
  }

  private static lerArmazenada(): SessaoArmazenada | null {
    try {
      const bruto = sessionStorage.getItem(CHAVE);
      if (bruto === null) {
        return null;
      }
      const dado: unknown = JSON.parse(bruto);
      if (typeof dado === 'object' && dado !== null) {
        const d = dado as Record<string, unknown>;
        if (typeof d['token'] === 'string' && typeof d['expiraEm'] === 'number') {
          return { token: d['token'], expiraEm: d['expiraEm'] };
        }
      }
    } catch {
      // Armazenamento indisponível ou conteúdo corrompido: tratamos como "sem sessão".
    }
    return null;
  }
}
