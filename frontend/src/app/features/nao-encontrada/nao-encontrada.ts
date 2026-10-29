import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-nao-encontrada',
  imports: [RouterLink, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    main {
      min-height: 100vh;
      display: grid;
      place-content: center;
      justify-items: center;
      gap: var(--app-espaco-4);
      padding: var(--app-espaco-4);
      text-align: center;
    }
    h1 {
      margin: 0;
      font: var(--mat-sys-headline-small);
    }
    p {
      margin: 0;
    }
  `,
  template: `
    <main>
      <h1>Página não encontrada</h1>
      <p>O endereço acessado não existe.</p>
      <a mat-flat-button routerLink="/">Voltar ao início</a>
    </main>
  `,
})
export class NaoEncontrada {}
