import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../../core/api/mensagem-erro';
import { AuthService } from '../../../core/auth/auth.service';
import { aplicarErrosDoServidor, marcarErroDoServidor } from '../../../shared/forms/erros-do-servidor';

@Component({
  selector: 'app-cadastro',
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cadastro {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  // Espelha o contrato do backend só para orientar. A decisão final é da API.
  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(255)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    senha: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected cadastrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.enviando()) {
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);

    this.auth
      .cadastrar(this.form.getRawValue())
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigate(['/login'], { queryParams: { cadastro: 'ok' } });
        },
        error: (erro: unknown) => this.falhar(erro),
      });
  }

  private falhar(erro: unknown): void {
    // 409: o e-mail já existe. A mensagem da API aparece no próprio campo.
    if (erro instanceof HttpErrorResponse && erro.status === 409) {
      marcarErroDoServidor(this.form.controls.email, mensagemDeErro(erro));
      return;
    }
    // 400: cada mensagem vai para o seu campo. Se nenhum campo foi atendido, mensagem geral.
    if (!aplicarErrosDoServidor(this.form, erro)) {
      this.erro.set(mensagemDeErro(erro));
    }
  }
}
