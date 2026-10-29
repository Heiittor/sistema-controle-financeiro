import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../core/api/api-url';
import { Conta } from './conta.models';

@Injectable({ providedIn: 'root' })
export class ContasService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  listar(): Observable<Conta[]> {
    return this.http.get<Conta[]>(`${this.apiUrl}/contas`);
  }

  // A conta nasce com saldo zero: o contrato só aceita o nome.
  criar(nome: string): Observable<Conta> {
    return this.http.post<Conta>(`${this.apiUrl}/contas`, { nome });
  }
}
