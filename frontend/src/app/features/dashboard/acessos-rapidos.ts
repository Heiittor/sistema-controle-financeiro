import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { TipoCategoria } from '../categorias/categoria.models';

@Component({
  selector: 'nav[appAcessosRapidos]',
  imports: [MatIconModule, RouterLink],
  templateUrl: './acessos-rapidos.html',
  styleUrl: './acessos-rapidos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcessosRapidos {
  readonly habilitado = input.required<boolean>();
  readonly registrar = output<TipoCategoria>();
}
