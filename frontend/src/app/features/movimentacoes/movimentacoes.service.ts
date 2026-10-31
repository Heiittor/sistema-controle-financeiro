import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../core/api/api-url';
import { Pagina } from '../../core/api/pagina';
import { TipoCategoria } from '../categorias/categoria.models';
import { EdicaoTransacao, NovaTransacao, Transacao } from './transacao.models';

export interface FiltroHistorico {
  inicio: string;
  fim: string;
  pagina: number;
  tamanho: number;
}

@Injectable({ providedIn: 'root' })
export class MovimentacoesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  // Período sempre explícito: o front não depende do padrão do servidor.
  listar(contaId: number, filtro: FiltroHistorico): Observable<Pagina<Transacao>> {
    const params = new HttpParams()
      .set('inicio', filtro.inicio)
      .set('fim', filtro.fim)
      .set('page', filtro.pagina)
      .set('size', filtro.tamanho);
    return this.http.get<Pagina<Transacao>>(`${this.apiUrl}/contas/${contaId}/transacoes`, { params });
  }

  // O tipo escolhe a rota (receitas ou despesas): o cliente não consegue contradizê-la no corpo.
  registrar(contaId: number, tipo: TipoCategoria, dados: NovaTransacao): Observable<Transacao> {
    const rota = tipo === 'RECEITA' ? 'receitas' : 'despesas';
    return this.http.post<Transacao>(`${this.apiUrl}/contas/${contaId}/${rota}`, dados);
  }

  // Sem corpo: o estorno é sempre o inverso exato da original. Quem define a data é o backend.
  estornar(contaId: number, transacaoId: number): Observable<Transacao> {
    return this.http.post<Transacao>(
      `${this.apiUrl}/contas/${contaId}/transacoes/${transacaoId}/estorno`,
      null,
    );
  }

  editar(contaId: number, transacaoId: number, dados: EdicaoTransacao): Observable<Transacao> {
    return this.http.patch<Transacao>(`${this.apiUrl}/contas/${contaId}/transacoes/${transacaoId}`, dados);
  }
}
