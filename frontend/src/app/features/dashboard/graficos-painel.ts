import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MoedaPipe } from '../../shared/pipes/moeda-pipe';
import { formatarData } from '../../shared/datas/datas';
import { Grafico } from './grafico';
import { CORES_CATEGORIAS, graficoLinha, graficoRosca } from './graficos-dashboard';
import { ResumoDashboard, MesDashboard } from './dashboard.models';
import { rotuloMes } from './periodo-dashboard';

interface DadosGraficos {
  resumo: ResumoDashboard;
  serie: MesDashboard[];
  linha: ReturnType<typeof graficoLinha>;
  rosca: ReturnType<typeof graficoRosca>;
  inicioSerie: string;
  fimSerie: string;
}

@Component({
  selector: 'div[appGraficosPainel]',
  imports: [Grafico, MatIconModule, MoedaPipe],
  templateUrl: './graficos-painel.html',
  styleUrl: './graficos-painel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraficosPainel {
  readonly dados = input.required<DadosGraficos>();
  readonly selecionada = input<number | null>(null);
  readonly selecionar = output<number | null>();
  protected readonly cores = CORES_CATEGORIAS;
  protected readonly formatarData = formatarData;
  protected readonly rotuloMes = rotuloMes;
  protected participacao(valor: number, total: number): string {
    // Proporção visual da rosca, sem alterar ou agregar valores financeiros.
    return new Intl.NumberFormat('pt-BR', {
      style: 'percent',
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(total > 0 ? valor / total : 0);
  }
}
