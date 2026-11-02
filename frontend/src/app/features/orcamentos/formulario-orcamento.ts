import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../core/api/mensagem-erro';
import { tratarErroDeEnvio } from '../../shared/forms/erros-do-servidor';
import { converterValor, validarValor } from '../../shared/forms/valor';
import { CategoriasService } from '../categorias/categorias.service';
import { Categoria } from '../categorias/categoria.models';
import { MES_VALIDO, Orcamento } from './orcamento.models';
import { OrcamentosService } from './orcamentos.service';

export interface DadosOrcamento {
  mes: string;
  orcamento?: Orcamento;
}
@Component({
  selector: 'app-formulario-orcamento',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    RouterLink,
  ],
  templateUrl: './formulario-orcamento.html',
  styles: `
    @use '../../shared/estilos/mensagens';
    form mat-dialog-content {
      display: flex;
      flex-direction: column;
      gap: 20px;
      padding-top: 14px;
    }
    mat-form-field {
      width: 100%;
    }
    .ajuda {
      color: #627399;
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormularioOrcamento {
  protected readonly dados = inject<DadosOrcamento>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<FormularioOrcamento, boolean>>(MatDialogRef);
  private readonly api = inject(OrcamentosService);
  private readonly categoriasApi = inject(CategoriasService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly estado = signal<'carregando' | 'erro' | 'pronto'>('carregando');
  protected readonly erroCategorias = signal<string | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly form = inject(NonNullableFormBuilder).group({
    categoriaId: [
      this.dados.orcamento ? String(this.dados.orcamento.categoriaId) : '',
      Validators.required,
    ],
    mes: [
      this.dados.orcamento?.mes ?? this.dados.mes,
      [Validators.required, Validators.pattern(MES_VALIDO)],
    ],
    limite: [
      this.dados.orcamento ? String(this.dados.orcamento.limite) : '',
      [Validators.required, validarValor],
    ],
  });
  constructor() {
    if (this.dados.orcamento) {
      this.form.controls.categoriaId.disable();
      this.form.controls.mes.disable();
      this.estado.set('pronto');
    } else this.carregarCategorias();
  }
  protected carregarCategorias(): void {
    this.estado.set('carregando');
    this.categoriasApi
      .listar('DESPESA')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (categorias) => {
          this.categorias.set(categorias);
          this.estado.set('pronto');
        },
        error: (erro) => {
          this.erroCategorias.set(mensagemDeErro(erro));
          this.estado.set('erro');
        },
      });
  }
  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (
      this.enviando() ||
      this.estado() !== 'pronto' ||
      (!this.dados.orcamento && !this.categorias().length)
    )
      return;
    const v = this.form.getRawValue();
    const limite = converterValor(v.limite);
    if (limite === null) return;
    this.enviando.set(true);
    this.erro.set(null);
    this.ref.disableClose = true;
    const chamada = this.dados.orcamento
      ? this.api.editar(this.dados.orcamento.id, limite)
      : this.api.criar(Number(v.categoriaId), v.mes, limite);
    chamada
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.enviando.set(false);
          this.ref.disableClose = false;
        }),
      )
      .subscribe({
        next: () => this.ref.close(true),
        error: (erro) => this.erro.set(tratarErroDeEnvio(this.form, erro, null)),
      });
  }
}
