import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { bancoDoNome } from './catalogo-bancos';

@Component({
  selector: 'app-logo-banco',
  imports: [MatIconModule],
  template: `@if (banco(); as b) {
      @if (falha() !== b.logotipo) {
        <img [src]="b.logotipo" alt="" width="52" height="40" (error)="falha.set(b.logotipo)" />
      } @else {
        <mat-icon aria-hidden="true">account_balance_wallet</mat-icon>
      }
    } @else {
      <mat-icon aria-hidden="true">account_balance_wallet</mat-icon>
    }`,
  styles: [
    `
      :host {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: var(--banco-largura, 56px);
        height: var(--banco-altura, 44px);
        flex-shrink: 0;
        padding: var(--banco-respiro, 5px);
        border-radius: var(--banco-raio, 10px);
        background: #fff;
        border: 1px solid #e2e8f7;
        box-sizing: border-box;
        color: #0056ff;
      }
      img {
        display: block;
        max-width: 100%;
        height: 100%;
        object-fit: contain;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogoBanco {
  readonly nome = input.required<string>();
  protected readonly banco = computed(() => bancoDoNome(this.nome()));
  protected readonly falha = signal<string | null>(null);
}
