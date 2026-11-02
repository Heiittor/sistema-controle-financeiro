import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import {
  FormGroupDirective,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../core/api/mensagem-erro';
import { tratarErroDeEnvio } from '../../shared/forms/erros-do-servidor';
import { Categoria, ROTULO_TIPO, TIPOS_CATEGORIA, TipoCategoria } from './categoria.models';
import { CategoriasService } from './categorias.service';

@Component({
  selector: 'app-categorias',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
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
  protected readonly totalReceitas = computed(
    () => this.categorias().filter((c) => c.tipo === 'RECEITA').length,
  );
  protected readonly totalDespesas = computed(
    () => this.categorias().filter((c) => c.tipo === 'DESPESA').length,
  );

  // Ícones apenas ilustrativos: não mudam o tipo nem o contrato de cadastro.
  protected icone(nome: string): string {
    const texto = nome
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
    if (/alimenta|almoco|jantar|restaurante/.test(texto)) return 'restaurant';
    if (/mercado|compra/.test(texto)) return 'shopping_bag';
    if (/salario|trabalho/.test(texto)) return 'work';
    if (/transporte|combustivel/.test(texto)) return 'directions_car';
    if (/moradia|aluguel/.test(texto)) return 'home';
    return 'label';
  }

  // Validação só orienta o usuário. Quem decide é o backend.
  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    tipo: ['' as TipoCategoria | '', [Validators.required]],
  });
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  // Só a consulta mais recente pode atualizar a lista e seu estado.
  private consulta = 0;

  constructor() {
    this.carregar();
  }

  protected filtrar(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;
    this.filtro.set(valor === 'RECEITA' || valor === 'DESPESA' ? valor : null);
    this.carregar();
  }

  protected carregar(mostrarCarregando = true): void {
    const minha = ++this.consulta;
    if (mostrarCarregando) {
      this.estado.set('carregando');
    }
    this.api.listar(this.filtro() ?? undefined).subscribe({
      next: (categorias) => {
        if (minha !== this.consulta) {
          return;
        }
        this.categorias.set(categorias);
        this.estado.set('pronto');
      },
      error: (erro: unknown) => {
        if (minha !== this.consulta) {
          return;
        }
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
