import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { mensagemDeErro } from '../../core/api/mensagem-erro';
import { Pagina } from '../../core/api/pagina';
import { formatarData, hojeNegocio, primeiroDiaDoMes, ultimoDiaDoMes } from '../../shared/datas/datas';
import { MoedaPipe } from '../../shared/pipes/moeda-pipe';
import { Conta } from '../contas/conta.models';
import { ContasService } from '../contas/contas.service';
import { MovimentacoesService } from './movimentacoes.service';
import { INFO_TIPO, Transacao } from './transacao.models';

const TAMANHO_DA_PAGINA = 20;

interface FiltroAplicado {
  contaId: number;
  inicio: string;
  fim: string;
}

@Component({
  selector: 'app-movimentacoes',
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, MoedaPipe],
  templateUrl: './movimentacoes.html',
  styleUrl: './movimentacoes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Movimentacoes {
  private readonly contasApi = inject(ContasService);
  private readonly api = inject(MovimentacoesService);

  protected readonly formatarData = formatarData;
  protected readonly info = INFO_TIPO;

  protected readonly contas = signal<Conta[]>([]);
  protected readonly estadoContas = signal<'carregando' | 'erro' | 'pronto'>('carregando');
  protected readonly erroContas = signal<string | null>(null);

  // O que o usuário está digitando. Só vale depois de "Aplicar".
  protected readonly form = inject(NonNullableFormBuilder).group({
    conta: ['', [Validators.required]],
    inicio: ['', [Validators.required]],
    fim: ['', [Validators.required]],
  });
  protected readonly erroFiltro = signal<string | null>(null);

  // O filtro que de fato está valendo. A paginação usa ESTE, e não o formulário.
  protected readonly aplicado = signal<FiltroAplicado | null>(null);
  protected readonly estado = signal<'ocioso' | 'carregando' | 'erro' | 'pronto'>('ocioso');
  protected readonly erro = signal<string | null>(null);
  protected readonly resultado = signal<Pagina<Transacao> | null>(null);

  // Descarta respostas atrasadas: só a consulta mais recente atualiza a tela.
  private consulta = 0;

  constructor() {
    this.carregarContas();
  }

  protected carregarContas(): void {
    this.estadoContas.set('carregando');
    this.contasApi.listar().subscribe({
      next: (contas) => {
        this.contas.set(contas);
        this.estadoContas.set('pronto');
        if (contas.length > 0) {
          // Começa na primeira conta, com o mês atual no fuso de negócio.
          const hoje = hojeNegocio();
          this.form.patchValue({
            conta: String(contas[0].id),
            inicio: primeiroDiaDoMes(hoje),
            fim: ultimoDiaDoMes(hoje),
          });
          this.aplicar();
        }
      },
      error: (erro: unknown) => {
        this.erroContas.set(mensagemDeErro(erro));
        this.estadoContas.set('erro');
      },
    });
  }

  protected aplicar(): void {
    this.erroFiltro.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { conta, inicio, fim } = this.form.getRawValue();
    // Datas AAAA-MM-DD comparam corretamente como texto. O backend também valida (422).
    if (inicio > fim) {
      this.erroFiltro.set('A data inicial não pode ser posterior à data final.');
      return;
    }
    this.aplicado.set({ contaId: Number(conta), inicio, fim });
    this.carregar(0);
  }

  protected carregar(pagina: number): void {
    const filtro = this.aplicado();
    if (filtro === null) {
      return;
    }
    const minha = ++this.consulta;
    this.estado.set('carregando');

    this.api
      .listar(filtro.contaId, {
        inicio: filtro.inicio,
        fim: filtro.fim,
        pagina,
        tamanho: TAMANHO_DA_PAGINA,
      })
      .subscribe({
        next: (resultado) => {
          if (minha !== this.consulta) {
            return;
          }
          this.resultado.set(resultado);
          this.estado.set('pronto');
        },
        error: (erro: unknown) => {
          if (minha !== this.consulta) {
            return;
          }
          this.erro.set(mensagemDeErro(erro));
          this.estado.set('erro');
        },
      });
  }

  protected tentarNovamente(): void {
    this.carregar(this.resultado()?.pagina ?? 0);
  }

  protected nomeDaConta(): string {
    const id = this.aplicado()?.contaId;
    return this.contas().find((c) => c.id === id)?.nome ?? '';
  }

  // Só apresentação: o valor vem positivo e o sinal sai do tipo.
  protected sinal(transacao: Transacao): string {
    return INFO_TIPO[transacao.tipo].sentido === 'entrada' ? '+' : '−';
  }
}
