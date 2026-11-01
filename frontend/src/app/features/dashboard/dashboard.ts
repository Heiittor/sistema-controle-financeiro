import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';
import { mensagemDeErro } from '../../core/api/mensagem-erro';
import { hojeNegocio, formatarData } from '../../shared/datas/datas';
import { MoedaPipe } from '../../shared/pipes/moeda-pipe';
import { ContasService } from '../contas/contas.service';
import { DashboardService } from './dashboard.service';
import { ResumoDashboard, MesDashboard } from './dashboard.models';
import { Grafico } from './grafico';
import { ValorAnimado } from './valor-animado';
import { graficoLinha, graficoRosca, CORES_CATEGORIAS } from './graficos-dashboard';
import { periodoDashboard, rotuloMes } from './periodo-dashboard';
import { UltimasMovimentacoesService } from './ultimas-movimentacoes.service';
import { Transacao, INFO_TIPO } from '../movimentacoes/transacao.models';
import { minigrafico } from './painel-graficos';

@Component({
  selector: 'app-dashboard',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MoedaPipe,
    Grafico,
    ValorAnimado,
    MatIconModule,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard {
  private readonly api = inject(DashboardService);
  private readonly contas = inject(ContasService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly historico = inject(UltimasMovimentacoesService);
  private consultaHistorico?: Subscription;
  protected readonly ultimas = signal<Transacao[]>([]);
  protected readonly estadoHistorico = signal<'carregando' | 'erro' | 'pronto'>('carregando');
  protected readonly info = INFO_TIPO;
  private consulta?: Subscription;
  protected readonly form = inject(NonNullableFormBuilder).group({
    mes: [hojeNegocio().slice(0, 7), Validators.required],
  });
  protected readonly estado = signal<'carregando' | 'erro' | 'pronto'>('carregando');
  protected readonly erro = signal('');
  protected readonly erroPeriodo = signal('');
  protected readonly dados = signal<{
    resumo: ResumoDashboard;
    serie: MesDashboard[];
    semContas: boolean;
    linha: ReturnType<typeof graficoLinha>;
    rosca: ReturnType<typeof graficoRosca>;
    inicioSerie: string;
    fimSerie: string;
    miniReceitas: ReturnType<typeof minigrafico>;
    miniDespesas: ReturnType<typeof minigrafico>;
  } | null>(null);
  protected readonly selecionada = signal<number | null>(null);
  protected readonly cores = CORES_CATEGORIAS;
  protected readonly rotuloMes = rotuloMes;
  protected readonly formatarData = formatarData;
  protected participacao(valor: number, total: number): string {
    // Proporção visual da rosca, sem alterar ou agregar valores financeiros.
    return new Intl.NumberFormat('pt-BR', {
      style: 'percent',
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(total > 0 ? valor / total : 0);
  }

  constructor() {
    this.carregar();
  }

  protected carregar(): void {
    const periodo = periodoDashboard(this.form.controls.mes.value);
    this.erroPeriodo.set('');
    if (!periodo) {
      this.form.markAllAsTouched();
      this.erroPeriodo.set('Selecione um mês válido.');
      return;
    }
    this.consulta?.unsubscribe();
    this.consultaHistorico?.unsubscribe();
    this.ultimas.set([]);
    this.estadoHistorico.set('carregando');
    this.estado.set('carregando');
    this.dados.set(null);
    this.selecionada.set(null);
    this.consulta = forkJoin({
      contas: this.contas.listar(),
      resumo: this.api.resumo(periodo.inicio, periodo.fim),
      serie: this.api.serie(periodo.inicioSerie, periodo.fim),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ contas, resumo, serie }) => {
          this.dados.set({
            resumo,
            serie,
            semContas: contas.length === 0,
            linha: graficoLinha(serie),
            rosca: graficoRosca(resumo.porCategoria),
            inicioSerie: periodo.inicioSerie,
            fimSerie: periodo.fim,
            miniReceitas: minigrafico(serie, 'receitas'),
            miniDespesas: minigrafico(serie, 'despesas'),
          });
          this.estado.set('pronto');
          this.consultaHistorico = this.historico
            .listar(
              contas.map((c) => c.id),
              periodo.inicio,
              periodo.fim,
            )
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: (linhas) => {
                this.ultimas.set(linhas);
                this.estadoHistorico.set('pronto');
              },
              error: () => this.estadoHistorico.set('erro'),
            });
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemDeErro(erro));
          this.estado.set('erro');
        },
      });
  }
}
