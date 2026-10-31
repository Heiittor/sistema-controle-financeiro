import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../../core/api/mensagem-erro';
import { formatarData } from '../../../shared/datas/datas';
import { MoedaPipe } from '../../../shared/pipes/moeda-pipe';
import { MovimentacoesService } from '../movimentacoes.service';
import { INFO_TIPO, Transacao } from '../transacao.models';

export interface DadosEstorno {
  contaId: number;
  transacao: Transacao;
}

// 'ok': estornou. 'recarregar': não estornou, mas a lista da tela estava desatualizada.
export type ResultadoEstorno = 'ok' | 'recarregar';

@Component({
  selector: 'app-estornar-movimentacao',
  imports: [MatDialogModule, MatButtonModule, MoedaPipe],
  templateUrl: './estornar-movimentacao.html',
  styleUrl: './estornar-movimentacao.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstornarMovimentacao {
  private readonly dados = inject<DadosEstorno>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<EstornarMovimentacao, ResultadoEstorno>>(MatDialogRef);
  private readonly api = inject(MovimentacoesService);

  protected readonly transacao = this.dados.transacao;
  protected readonly info = INFO_TIPO;
  protected readonly formatarData = formatarData;

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  // Verdadeiro quando a API diz que a transação já foi estornada: a lista da tela está velha.
  protected readonly desatualizada = signal(false);

  protected confirmar(): void {
    if (this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);

    this.api
      .estornar(this.dados.contaId, this.transacao.id)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => this.ref.close('ok'),
        error: (erro: unknown) => {
          if (erro instanceof HttpErrorResponse && erro.status === 409) {
            this.desatualizada.set(true);
          }
          this.erro.set(mensagemDeErro(erro));
        },
      });
  }

  protected fechar(): void {
    this.ref.close(this.desatualizada() ? 'recarregar' : undefined);
  }
}
