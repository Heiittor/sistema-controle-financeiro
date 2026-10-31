import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../../core/api/mensagem-erro';
import { hojeNegocio } from '../../../shared/datas/datas';
import { tratarErroDeEnvio } from '../../../shared/forms/erros-do-servidor';
import { converterValor, validarValor } from '../../../shared/forms/valor';
import { Categoria, TIPOS_CATEGORIA, TipoCategoria } from '../../categorias/categoria.models';
import { CategoriasService } from '../../categorias/categorias.service';
import { Conta } from '../../contas/conta.models';
import { MovimentacoesService } from '../movimentacoes.service';

export interface DadosNovaMovimentacao {
  contas: Conta[];
  contaIdInicial: number;
}

// O que a tela precisa saber para avisar o usuário e recarregar o histórico.
export interface MovimentacaoRegistrada {
  contaId: number;
  data: string;
}

@Component({
  selector: 'app-nova-movimentacao',
  imports: [ReactiveFormsModule, RouterLink, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './nova-movimentacao.html',
  styleUrl: './nova-movimentacao.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NovaMovimentacao {
  private readonly dados = inject<DadosNovaMovimentacao>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<NovaMovimentacao, MovimentacaoRegistrada>>(MatDialogRef);
  private readonly categoriasApi = inject(CategoriasService);
  private readonly api = inject(MovimentacoesService);

  protected readonly contas = this.dados.contas;
  protected readonly tipos = TIPOS_CATEGORIA;

  // Os nomes de categoriaId, descricao, valor e data são os do backend: o campos[] do 400 cai no lugar certo.
  protected readonly form = inject(NonNullableFormBuilder).group({
    tipo: ['DESPESA' as TipoCategoria, [Validators.required]],
    conta: [String(this.dados.contaIdInicial), [Validators.required]],
    categoriaId: ['', [Validators.required]],
    descricao: ['', [Validators.required, Validators.maxLength(255)]],
    valor: ['', [Validators.required, validarValor]],
    data: [hojeNegocio(), [Validators.required]],
  });

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly estadoCategorias = signal<'carregando' | 'erro' | 'pronto'>('carregando');
  protected readonly erroCategorias = signal<string | null>(null);
  protected readonly semCategorias = computed(
    () => this.estadoCategorias() === 'pronto' && this.categorias().length === 0,
  );

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  // Descarta respostas atrasadas quando o usuário troca o tipo rapidamente.
  private consulta = 0;

  constructor() {
    this.carregarCategorias();
    this.form.controls.tipo.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.carregarCategorias());
  }

  protected carregarCategorias(): void {
    const minha = ++this.consulta;
    // Uma categoria de outro tipo não serve mais: limpa a escolha anterior.
    this.form.controls.categoriaId.setValue('');
    this.estadoCategorias.set('carregando');

    this.categoriasApi.listar(this.form.controls.tipo.value).subscribe({
      next: (lista) => {
        if (minha !== this.consulta) {
          return;
        }
        this.categorias.set(lista);
        this.estadoCategorias.set('pronto');
      },
      error: (erro: unknown) => {
        if (minha !== this.consulta) {
          return;
        }
        this.erroCategorias.set(mensagemDeErro(erro));
        this.estadoCategorias.set('erro');
      },
    });
  }

  protected registrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.enviando()) {
      return;
    }
    const v = this.form.getRawValue();
    const valor = converterValor(v.valor);
    if (valor === null) {
      return; // o validador já bloqueou; isto só satisfaz o tipo
    }

    this.enviando.set(true);
    this.erro.set(null);

    this.api
      .registrar(Number(v.conta), v.tipo, {
        categoriaId: Number(v.categoriaId),
        descricao: v.descricao,
        valor,
        data: v.data,
      })
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => this.ref.close({ contaId: Number(v.conta), data: v.data }),
        // 409 aqui significa "conta alterada por outra operação", e não um campo repetido.
        error: (erro: unknown) => this.erro.set(tratarErroDeEnvio(this.form, erro, null)),
      });
  }

  protected cancelar(): void {
    this.ref.close();
  }
}
