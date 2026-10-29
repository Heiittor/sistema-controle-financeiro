import { ChangeDetectionStrategy, Component } from '@angular/core';

// Honestidade de interface: uma área sem contrato real não mostra números inventados.
@Component({
  selector: 'app-em-construcao',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    section {
      padding: var(--app-espaco-8) 0;
    }
    h2 {
      margin: 0 0 var(--app-espaco-2);
      font: var(--mat-sys-title-medium);
    }
    p {
      margin: 0;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
  template: `
    <section>
      <h2>Em construção</h2>
      <p>Esta área será implementada em uma próxima fase.</p>
    </section>
  `,
})
export class EmConstrucao {}
