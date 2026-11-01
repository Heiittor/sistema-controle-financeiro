import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { MoedaPipe } from '../../shared/pipes/moeda-pipe';
import { DURACAO_ENTRADA, MOVIMENTO_REDUZIDO, QUADRO_ANIMACAO } from './movimento';

@Component({
  selector: 'app-valor-animado',
  imports: [MoedaPipe],
  template:
    '<span role="img" [attr.aria-label]="valor() | moeda"><span aria-hidden="true">{{ exibido() | moeda }}</span></span>',
  styles: [':host { font-variant-numeric: tabular-nums; }'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ValorAnimado {
  readonly valor = input.required<number>();
  protected readonly exibido = signal(0);
  private readonly reduzido = inject(MOVIMENTO_REDUZIDO);
  private readonly quadro = inject(QUADRO_ANIMACAO);

  constructor() {
    effect((limpar) => {
      const destino = this.valor();
      if (this.reduzido() || destino === 0) {
        this.exibido.set(destino);
        return;
      }
      this.exibido.set(0);
      const inicio = this.quadro.agora();
      let frame: number;
      const atualizar = (agora: number) => {
        const progresso = Math.min(1, Math.max(0, (agora - inicio) / DURACAO_ENTRADA));
        // Interpolação exclusivamente visual. O DTO e o valor final permanecem intactos.
        this.exibido.set(progresso === 1 ? destino : destino * (1 - Math.pow(1 - progresso, 3)));
        if (progresso < 1) frame = this.quadro.agendar(atualizar);
      };
      frame = this.quadro.agendar(atualizar);
      limpar(() => this.quadro.cancelar(frame));
    });
  }
}
