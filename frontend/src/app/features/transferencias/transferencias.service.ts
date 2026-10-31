import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../core/api/api-url';
import { NovaTransferencia, Transferencia } from './transferencia.models';

@Injectable({ providedIn: 'root' })
export class TransferenciasService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  // Uma única chamada: é a API que debita uma conta, credita a outra e grava as duas transações.
  transferir(dados: NovaTransferencia): Observable<Transferencia> {
    return this.http.post<Transferencia>(`${this.apiUrl}/transferencias`, dados);
  }
}
