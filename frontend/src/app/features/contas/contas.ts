import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { FormGroupDirective, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../core/api/mensagem-erro';
import { tratarErroDeEnvio } from '../../shared/forms/erros-do-servidor';
import { MoedaPipe } from '../../shared/pipes/moeda-pipe';
import { Conta } from './conta.models';
import { ContasService } from './contas.service';

@Component({
  selector: 'app-contas',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MoedaPipe],
  templateUrl: './contas.html',
  styleUrl: './contas.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Contas {
  private readonly api = inject(ContasService);
  private readonly diretiva = viewChild(FormGroupDirective);

  protected readonly contas = signal<Conta[]>([]);
  protected readonly estado = signal<'carregando' | 'erro' | 'pronto'>('carregando');
  protected readonly erroLista = signal<string | null>(null);

  // Validação só orienta o usuário. Quem decide é o backend.
  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
  });
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  constructor() {
    this.carregar();
  }

  protected carregar(mostrarCarregando = true): void {
    if (mostrarCarregando) {
      this.estado.set('carregando');
    }
    this.api.listar().subscribe({
      next: (contas) => {
        this.contas.set(contas);
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
    if (this.enviando()) {
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);
    this.sucesso.set(null);

    this.api
      .criar(this.form.getRawValue().nome)
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => {
          this.sucesso.set('Conta criada.');
          // resetForm limpa também o estado "enviado", para o campo não aparecer em erro.
          this.diretiva()?.resetForm();
          // A lista vem da API, e não de um item montado aqui. Sem piscar "Carregando".
          this.carregar(false);
        },
        error: (erro: unknown) => this.erro.set(tratarErroDeEnvio(this.form, erro, 'nome')),
      });
  }
}
