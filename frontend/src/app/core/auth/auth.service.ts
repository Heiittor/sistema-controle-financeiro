import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable, tap } from 'rxjs';
import { API_URL } from '../api/api-url';
import { SessaoService } from '../sessao/sessao.service';
import { CadastroRequest, LoginRequest, TokenResponse, UsuarioResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);
  private readonly sessao = inject(SessaoService);

  // Em caso de erro (401, 429...) a sessão não é tocada: o erro chega intacto à tela.
  login(dados: LoginRequest): Observable<void> {
    return this.http.post<TokenResponse>(`${this.apiUrl}/auth/login`, dados).pipe(
      tap((resposta) => this.sessao.iniciar(resposta.token, resposta.expiraEmSegundos)),
      map(() => undefined),
    );
  }

  // Cadastrar não inicia sessão: o usuário precisa fazer login em seguida.
  cadastrar(dados: CadastroRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${this.apiUrl}/usuarios`, dados);
  }

  sair(): void {
    this.sessao.encerrar();
  }
}
