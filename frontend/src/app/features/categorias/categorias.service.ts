import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../core/api/api-url';
import { Categoria, TipoCategoria } from './categoria.models';

@Injectable({ providedIn: 'root' })
export class CategoriasService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  // Sem tipo, a API devolve todas as categorias do usuário.
  listar(tipo?: TipoCategoria): Observable<Categoria[]> {
    const params = tipo ? new HttpParams().set('tipo', tipo) : undefined;
    return this.http.get<Categoria[]>(`${this.apiUrl}/categorias`, { params });
  }

  criar(nome: string, tipo: TipoCategoria): Observable<Categoria> {
    return this.http.post<Categoria>(`${this.apiUrl}/categorias`, { nome, tipo });
  }
}
