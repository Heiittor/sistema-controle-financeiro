import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { FormGroupDirective, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../core/api/mensagem-erro';
import { tratarErroDeEnvio } from '../../shared/forms/erros-do-servidor';
import { Categoria, ROTULO_TIPO, TIPOS_CATEGORIA, TipoCategoria } from './categoria.models';
import { CategoriasService } from './categorias.service';

@Component({
  selector: 'app-categorias',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './categorias.html',
  styleUrl: './categorias.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Categorias {
  private readonly api = inject(CategoriasService);
  private readonly diretiva = viewChild(FormGroupDirective);

  protected readonly tipos = TIPOS_CATEGORIA;
  protected readonly rotulo = ROTULO_TIPO;

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly filtro = signal<TipoCategoria | null>(null);
  protected readonly estado = signal<'carregando' | 'erro' | 'pronto'>('carregando');
  protected readonly erroLista = signal<string | null>(null);

  // Validação só orienta o usuário. Quem decide é o backend.
  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    tipo: ['' as TipoCategoria | '', [Validators.required]],
  });
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  constructor() {
    this.carregar();
  }

  protected filtrar(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;
    this.filtro.set(valor === 'RECEITA' || valor === 'DESPESA' ? valor : null);
    this.carregar();
  }

  protected carregar(mostrarCarregando = true): void {
    if (mostrarCarregando) {
      this.estado.set('carregando');
    }
    this.api.listar(this.filtro() ?? undefined).subscribe({
      next: (categorias) => {
        this.categorias.set(categorias);
        this.estado.set('pronto');
      },
      error: (erro: unknown) => {
        this.erroLista.set(mensagemDeErro(erro));
        this.estado.set('erro');
      },
    });
  }

  protected criar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nome, tipo } = this.form.getRawValue();
    if (tipo === '' || this.enviando()) {
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);
    this.sucesso.set(null);

    this.api
      .criar(nome, tipo)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => {
          this.sucesso.set('Categoria criada.');
          this.diretiva()?.resetForm();
          // Volta para "Todas": com um filtro ativo, a categoria nova poderia não aparecer.
          this.filtro.set(null);
          this.carregar(false);
        },
        error: (erro: unknown) => this.erro.set(tratarErroDeEnvio(this.form, erro, 'nome')),
      });
  }
}
