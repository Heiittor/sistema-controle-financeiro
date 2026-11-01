import { Injectable, inject } from '@angular/core';
import { from, mergeMap, map, reduce } from 'rxjs';
import { MovimentacoesService } from '../movimentacoes/movimentacoes.service';
import { Transacao } from '../movimentacoes/transacao.models';

@Injectable({ providedIn: 'root' })
export class UltimasMovimentacoesService {
  private readonly api = inject(MovimentacoesService);
  listar(contas: number[], inicio: string, fim: string) {
    // Cada conta já vem ordenada por data/id. Suas quatro primeiras linhas
    // bastam para encontrar as quatro mais recentes do conjunto, sem somar valores.
    return from(contas).pipe(
      mergeMap((id) => this.api.listar(id, { inicio, fim, pagina: 0, tamanho: 4 }), 4),
      map((pagina) => pagina.conteudo),
      reduce(
        (ultimas, linhas) =>
          [...ultimas, ...linhas]
            .sort((a, b) => b.data.localeCompare(a.data) || b.id - a.id)
            .slice(0, 4),
        [] as Transacao[],
      ),
    );
  }
}
