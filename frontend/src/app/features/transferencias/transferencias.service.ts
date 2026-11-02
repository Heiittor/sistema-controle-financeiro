import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../core/api/api-url';
import { NovaTransferencia, Transferencia, TransferenciaHistorico } from './transferencia.models';
import { Pagina } from '../../core/api/pagina';

@Injectable({ providedIn: 'root' })
export class TransferenciasService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  listar(contaId: number | null, pagina = 0): Observable<Pagina<TransferenciaHistorico>> {
    let params = new HttpParams().set('page', pagina).set('size', 5);
    if (contaId !== null) params = params.set('contaId', contaId);
    return this.http.get<Pagina<TransferenciaHistorico>>(`${this.apiUrl}/transferencias`, {
      params,
    });
  }

  // Uma única chamada: é a API que debita uma conta, credita a outra e grava as duas transações.
  transferir(dados: NovaTransferencia): Observable<Transferencia> {
    return this.http.post<Transferencia>(`${this.apiUrl}/transferencias`, dados);
  }
}
