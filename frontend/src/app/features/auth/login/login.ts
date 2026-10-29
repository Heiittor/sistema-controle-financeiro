import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { mensagemDeErro, segundosDeEspera } from '../../../core/api/mensagem-erro';
import { AuthService } from '../../../core/auth/auth.service';
import { SessaoService } from '../../../core/sessao/sessao.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly sessao = inject(SessaoService);

  // Vem do parâmetro de URL ?cadastro=ok (withComponentInputBinding), depois do cadastro.
  readonly cadastro = input<string>();

  // Validação só orienta o usuário. Quem decide é o backend.
  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    senha: ['', [Validators.required, Validators.maxLength(72)]],
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  // Segundos restantes de bloqueio (429). null = sem bloqueio.
  protected readonly espera = signal<number | null>(null);
  protected readonly esperaFormatada = computed(() => {
    const total = this.espera();
    if (total === null) {
      return '';
    }
    const minutos = String(Math.floor(total / 60)).padStart(2, '0');
    const segundos = String(total % 60).padStart(2, '0');
    return `${minutos}:${segundos}`;
  });

  private contagem: ReturnType<typeof setInterval> | null = null;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.pararContagem());
  }

  protected entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // Protege contra duplo envio e contra tentar durante o bloqueio do 429.
    if (this.enviando() || this.espera() !== null) {
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);

    this.auth
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/');
        },
        error: (erro: unknown) => this.falhar(erro),
      });
  }

  private falhar(erro: unknown): void {
    this.erro.set(mensagemDeErro(erro));
    if (erro instanceof HttpErrorResponse && erro.status === 429) {
      const segundos = segundosDeEspera(erro);
      if (segundos !== null) {
        this.iniciarContagem(segundos);
      }
    }
  }

  // Só mostra o tempo e libera o botão no fim. Nunca repete a tentativa sozinho.
  private iniciarContagem(segundos: number): void {
    this.pararContagem();
    this.espera.set(segundos);
    this.contagem = setInterval(() => {
      const atual = this.espera();
      if (atual === null || atual <= 1) {
        this.pararContagem();
        this.espera.set(null);
        this.erro.set(null);
      } else {
        this.espera.set(atual - 1);
      }
    }, 1000);
  }

  private pararContagem(): void {
    if (this.contagem !== null) {
      clearInterval(this.contagem);
      this.contagem = null;
    }
  }
}
