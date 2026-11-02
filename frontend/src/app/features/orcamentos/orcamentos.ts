import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { mensagemDeErro } from '../../core/api/mensagem-erro';
import { hojeNegocio } from '../../shared/datas/datas';
import { MoedaPipe } from '../../shared/pipes/moeda-pipe';
import { DadosOrcamento, FormularioOrcamento } from './formulario-orcamento';
import { formatarMes, MES_VALIDO, Orcamento, PaginaOrcamentos } from './orcamento.models';
import { OrcamentosService } from './orcamentos.service';

@Component({
  selector: 'app-orcamentos',
  imports: [MatButtonModule, MatFormFieldModule, MatInputModule, MatIconModule, MoedaPipe],
  templateUrl: './orcamentos.html',
  styleUrl: './orcamentos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Orcamentos {
  private readonly api = inject(OrcamentosService);
  private readonly dialogo = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly mes = signal(hojeNegocio().slice(0, 7));
  protected readonly estado = signal<'carregando' | 'erro' | 'pronto'>('carregando');
  protected readonly resultado = signal<PaginaOrcamentos | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly erroMes = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);
  protected readonly formatarMes = formatarMes;
  protected readonly situacoes = {
    DENTRO_DO_LIMITE: 'Dentro do limite',
    ATENCAO: 'Atenção ao limite',
    ACIMA_DO_LIMITE: 'Acima do limite',
  };
  private consulta = 0;
  private pagina = 0;
  constructor() {
    this.carregar();
  }
  protected selecionarMes(evento: Event): void {
    const mes = (evento.target as HTMLInputElement).value;
    if (!MES_VALIDO.test(mes)) {
      this.erroMes.set('Informe um mês válido. O período anterior continua aplicado.');
      return;
    }
    this.erroMes.set(null);
    this.sucesso.set(null);
    this.mes.set(mes);
    this.carregar(0);
  }
  protected carregar(pagina = this.pagina): void {
    const consulta = ++this.consulta;
    this.pagina = pagina;
    this.estado.set('carregando');
    this.api
      .listar(this.mes(), pagina)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (resultado) => {
          if (consulta !== this.consulta) return;
          this.resultado.set(resultado);
          this.estado.set('pronto');
        },
        error: (erro) => {
          if (consulta !== this.consulta) return;
          this.erro.set(mensagemDeErro(erro));
          this.estado.set('erro');
        },
      });
  }
  protected abrir(orcamento?: Orcamento): void {
    const mesAberto = this.mes();
    this.dialogo
      .open<FormularioOrcamento, DadosOrcamento, boolean>(FormularioOrcamento, {
        data: { mes: mesAberto, orcamento },
        width: '32rem',
        maxWidth: '95vw',
        restoreFocus: true,
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((salvo) => {
        if (salvo) {
          this.sucesso.set('Orçamento salvo. A lista exibe o mês selecionado.');
          this.carregar(0);
        }
      });
  }
  protected largura(percentual: number): number {
    return Math.min(100, Math.max(0, percentual));
  }
  protected percentual(valor: number): string {
    return valor.toLocaleString('pt-BR', { maximumFractionDigits: 2 }) + '%';
  }
  protected icone(nome: string): string {
    const texto = nome
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
    if (/alimenta|restaurante/.test(texto)) return 'restaurant';
    if (/transporte|combustivel/.test(texto)) return 'directions_car';
    if (/saude|medic/.test(texto)) return 'favorite';
    return 'label';
  }
}
