import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ITENS_MENU } from '../nav-itens';

@Component({
  selector: 'div[appMenuLateral]',
  imports: [MatButtonModule, MatIconModule, RouterLink, RouterLinkActive],
  templateUrl: './menu-lateral.html',
  styleUrl: './menu-lateral.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MenuLateral {
  readonly titulo = input.required<string>();
  readonly compacto = input.required<boolean>();
  readonly recolhida = input.required<boolean>();
  readonly fechar = output<void>();
  readonly alternar = output<void>();
  readonly sair = output<void>();
  protected readonly itens = ITENS_MENU;
}
