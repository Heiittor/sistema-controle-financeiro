import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormGroupDirective,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../core/api/mensagem-erro';
import { formatarData, hojeNegocio } from '../../shared/datas/datas';
import { tratarErroDeEnvio } from '../../shared/forms/erros-do-servidor';
import { converterValor, validarValor } from '../../shared/forms/valor';
import { MoedaPipe } from '../../shared/pipes/moeda-pipe';
import { Conta } from '../contas/conta.models';
import { ContasService } from '../contas/contas.service';
import { Transferencia } from './transferencia.models';
import { TransferenciasService } from './transferencias.service';

// Só orienta o usuário: o backend também recusa origem igual ao destino (422).
const diferenteDaOrigem: ValidatorFn = (controle): ValidationErrors | null => {
  const origem = controle.parent?.get('contaOrigemId')?.value;
  return origem !== undefined && origem !== '' && origem === controle.value ? { mesmaConta: true } : null;
};

@Component({
  selector: 'app-transferencias',
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, MoedaPipe],
  templateUrl: './transferencias.html',
  styleUrl: './transferencias.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Transferencias {
  private readonly contasApi = inject(ContasService);
  private readonly api = inject(TransferenciasService);
  private readonly diretiva = viewChild(FormGroupDirective);

  protected readonly formatarData = formatarData;

  protected readonly contas = signal<Conta[]>([]);
  protected readonly estadoContas = signal<'carregando' | 'erro' | 'pronto'>('carregando');
  protected readonly erroContas = signal<string | null>(null);

  // Os nomes dos controles são os do backend: o campos[] do 400 cai no lugar certo.
  protected readonly form = inject(NonNullableFormBuilder).group({
    contaOrigemId: ['', [Validators.required]],
    contaDestinoId: ['', [Validators.required, diferenteDaOrigem]],
    descricao: ['', [Validators.required, Validators.maxLength(255)]],
    valor: ['', [Validators.required, validarValor]],
    data: [hojeNegocio(), [Validators.required]],
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  // O que a API devolveu na última transferência. A mensagem de sucesso usa isto, e não o que foi digitado.
  protected readonly ultima = signal<Transferencia | null>(null);

  constructor() {
    this.carregarContas();
    // Trocar a origem pode tornar o destino inválido (ou válido): reavalia.
    this.form.controls.contaOrigemId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.contaDestinoId.updateValueAndValidity());
  }

  protected carregarContas(mostrarCarregando = true): void {
    if (mostrarCarregando) {
      this.estadoContas.set('carregando');
    }
    this.contasApi.listar().subscribe({
      next: (contas) => {
        this.contas.set(contas);
        this.estadoContas.set('pronto');
      },
      error: (erro: unknown) => {
        this.erroContas.set(mensagemDeErro(erro));
        this.estadoContas.set('erro');
      },
    });
  }

  protected transferir(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // Sem isto, um duplo clique poderia gerar duas transferências (o backend não tem chave de idempotência).
    if (this.enviando()) {
      return;
    }
    const v = this.form.getRawValue();
    const valor = converterValor(v.valor);
    if (valor === null) {
      return; // o validador já bloqueou; isto só satisfaz o tipo
    }

    this.enviando.set(true);
    this.erro.set(null);
    this.ultima.set(null);

    this.api
      .transferir({
        contaOrigemId: Number(v.contaOrigemId),
        contaDestinoId: Number(v.contaDestinoId),
        descricao: v.descricao,
        valor,
        data: v.data,
      })
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: (resposta) => {
          this.ultima.set(resposta);
          this.diretiva()?.resetForm({
            contaOrigemId: '',
            contaDestinoId: '',
            descricao: '',
            valor: '',
            data: hojeNegocio(),
          });
          // Os saldos novos vêm da API. O front não soma nem subtrai nada.
          this.carregarContas(false);
        },
        // Tentativa falha: o formulário fica como está, para o usuário corrigir.
        error: (erro: unknown) => this.erro.set(tratarErroDeEnvio(this.form, erro, null)),
      });
  }

  protected nomeDaConta(id: number): string {
    return this.contas().find((c) => c.id === id)?.nome ?? `conta ${id}`;
  }
}
