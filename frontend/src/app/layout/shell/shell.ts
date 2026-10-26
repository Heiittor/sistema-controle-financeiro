import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ITENS_MENU } from '../nav-itens';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule, MatIconModule, MatButtonModule],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  protected readonly itens = ITENS_MENU;

  // Abaixo de 900 px a sidebar vira um menu sobreposto, aberto pelo botão do topo.
  protected readonly compacto = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 900px)')
      .pipe(map((estado) => estado.matches)),
    { initialValue: false },
  );

  // O título vem da propriedade "title" da rota ativa (a mesma que define o título da aba).
  protected readonly titulo = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map(() => this.tituloAtual()),
    ),
    { initialValue: this.tituloAtual() },
  );

  protected sair(): void {
    this.auth.sair();
    void this.router.navigateByUrl('/login');
  }

  private tituloAtual(): string {
    let rota = this.router.routerState.snapshot.root;
    while (rota.firstChild) {
      rota = rota.firstChild;
    }
    return rota.title ?? '';
  }
}
