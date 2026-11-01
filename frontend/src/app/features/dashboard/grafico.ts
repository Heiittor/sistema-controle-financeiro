import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  ViewChild,
  InjectionToken,
  inject,
  effect,
  SimpleChanges,
} from '@angular/core';
import {
  Chart,
  ChartConfiguration,
  LineController,
  DoughnutController,
  LineElement,
  PointElement,
  ArcElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { comAnimacao } from './animacao-grafico';
import { MOVIMENTO_REDUZIDO } from './movimento';

Chart.register(
  LineController,
  DoughnutController,
  LineElement,
  PointElement,
  ArcElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
  Filler,
);

type InstanciaGrafico = Pick<Chart, 'destroy' | 'setActiveElements' | 'update'>;
export const CRIAR_GRAFICO = new InjectionToken<
  (canvas: HTMLCanvasElement, config: ChartConfiguration) => InstanciaGrafico
>('CRIAR_GRAFICO', {
  providedIn: 'root',
  factory: () => (canvas, config) => new Chart(canvas, config),
});

@Component({
  selector: 'app-grafico-dashboard',
  template:
    '<div class="area" [style.height.px]="altura"><canvas #canvas role="img" [attr.aria-label]="descricao"></canvas></div>',
  styles: [':host { display: block; min-width: 0; } .area { position: relative; height: 18rem; }'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Grafico implements AfterViewInit, OnChanges, OnDestroy {
  @Input({ required: true }) configuracao!: ChartConfiguration;
  @Input({ required: true }) descricao = '';
  @Input() destaque: number | null = null;
  @Input() altura = 288;
  @ViewChild('canvas') private canvas?: ElementRef<HTMLCanvasElement>;
  private chart?: InstanciaGrafico;
  private readonly criar = inject(CRIAR_GRAFICO);
  private readonly reduzido = inject(MOVIMENTO_REDUZIDO);
  private preferenciaAnterior = this.reduzido();

  constructor() {
    effect(() => {
      const atual = this.reduzido();
      if (atual !== this.preferenciaAnterior) {
        this.preferenciaAnterior = atual;
        if (this.canvas) this.desenhar();
      }
    });
  }

  ngAfterViewInit(): void {
    this.desenhar();
  }
  ngOnChanges(mudancas: SimpleChanges): void {
    if (!this.canvas) return;
    if (mudancas['configuracao']) this.desenhar();
    else if (mudancas['destaque'] && this.chart) {
      this.chart.setActiveElements(
        this.destaque === null ? [] : [{ datasetIndex: 0, index: this.destaque }],
      );
      this.chart.update('none');
    }
  }
  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private desenhar(): void {
    this.chart?.destroy();
    this.chart = this.criar(
      this.canvas!.nativeElement,
      comAnimacao(this.configuracao, this.reduzido()),
    );
    if (this.destaque !== null) {
      this.chart.setActiveElements([{ datasetIndex: 0, index: this.destaque }]);
      this.chart.update('none');
    }
  }
}
