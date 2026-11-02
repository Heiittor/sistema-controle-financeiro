import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../core/api/api-url';
import { Orcamento, PaginaOrcamentos } from './orcamento.models';
@Injectable({ providedIn: 'root' })
export class OrcamentosService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL) + '/orcamentos';
  listar(mes: string, pagina = 0) {
    return this.http.get<PaginaOrcamentos>(this.url, { params: { mes, page: pagina, size: 10 } });
  }
  criar(categoriaId: number, mes: string, limite: number) {
    return this.http.post<Orcamento>(this.url, { categoriaId, mes, limite });
  }
  editar(id: number, limite: number) {
    return this.http.patch<Orcamento>(this.url + '/' + id, { limite });
  }
}
