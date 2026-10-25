import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessaoService } from '../sessao/sessao.service';

// Só experiência do usuário: a autorização real é do backend (um token forjado leva 401 da API).
// token() descarta o token vencido e marca a sessão como expirada, para o login avisar.
export const authGuard: CanActivateFn = () => {
  const sessao = inject(SessaoService);
  const router = inject(Router);

  return sessao.token() !== null ? true : router.createUrlTree(['/login']);
};
