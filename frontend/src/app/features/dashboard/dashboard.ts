import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

// PROVISÓRIO: destino do login até o shell existir. O cabeçalho é neutro de propósito:
// não inferimos o nome do usuário a partir do token.
@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main>
      <h1>Minha conta</h1>
      <p>Área autenticada em construção.</p>
      <button type="button" (click)="sair()">Sair</button>
    </main>
  `,
})
export class Dashboard {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected sair(): void {
    this.auth.sair();
    void this.router.navigateByUrl('/login');
  }
}
