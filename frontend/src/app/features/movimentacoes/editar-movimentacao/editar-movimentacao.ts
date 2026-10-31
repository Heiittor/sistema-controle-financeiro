import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../../core/api/mensagem-erro';
import { formatarData } from '../../../shared/datas/datas';
import { tratarErroDeEnvio } from '../../../shared/forms/erros-do-servidor';
import { MoedaPipe } from '../../../shared/pipes/moeda-pipe';
import { Categoria, TipoCategoria } from '../../categorias/categoria.models';
import { CategoriasService } from '../../categorias/categorias.service';
import { MovimentacoesService } from '../movimentacoes.service';
import { INFO_TIPO, Transacao } from '../transacao.models';

export interface DadosEdicao {
  contaId: number;
  transacao: Transacao;
}

@Component({
  selector: 'app-editar-movimentacao',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MoedaPipe],
  templateUrl: './editar-movimentacao.html',
  styleUrl: './editar-movimentacao.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditarMovimentacao {
  private readonly dados = inject<DadosEdicao>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<EditarMovimentacao, boolean>>(MatDialogRef);
  private readonly categoriasApi = inject(CategoriasService);
  private readonly api = inject(MovimentacoesService);

  protected readonly transacao = this.dados.transacao;
  protected readonly info = INFO_TIPO;
  protected readonly formatarData = formatarData;

  // A categoria precisa ser do mesmo tipo da transação (a API também valida).
  private readonly tipoCategoria: TipoCategoria = this.transacao.tipo === 'RECEITA' ? 'RECEITA' : 'DESPESA';

  // Só descrição e categoria: valor, data, tipo e conta não são editáveis.
  protected readonly form = inject(NonNullableFormBuilder).group({
    categoriaId: [String(this.transacao.categoriaId ?? ''), [Validators.required]],
    descricao: [this.transacao.descricao, [Validators.required, Validators.maxLength(255)]],
  });

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly estadoCategorias = signal<'carregando' | 'erro' | 'pronto'>('carregando');
  protected readonly erroCategorias = signal<string | null>(null);

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  constructor() {
    this.carregarCategorias();
  }

  protected carregarCategorias(): void {
    this.estadoCategorias.set('carregando');
    this.categoriasApi.listar(this.tipoCategoria).subscribe({
      next: (lista) => {
        this.categorias.set(lista);
        this.estadoCategorias.set('pronto');
      },
      error: (erro: unknown) => {
        this.erroCategorias.set(mensagemDeErro(erro));
        this.estadoCategorias.set('erro');
      },
    });
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.enviando()) {
      return;
    }
    const v = this.form.getRawValue();

    this.enviando.set(true);
    this.erro.set(null);

    this.api
      .editar(this.dados.contaId, this.transacao.id, {
        categoriaId: Number(v.categoriaId),
        descricao: v.descricao,
      })
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => this.ref.close(true),
        error: (erro: unknown) => this.erro.set(tratarErroDeEnvio(this.form, erro, null)),
      });
  }

  protected cancelar(): void {
    this.ref.close();
  }
}
