import { inject, InjectionToken } from '@angular/core';
import { Router } from '@angular/router';

// A ação "levar ao login" entra por injeção: nos testes vira um espião.
// A rota /login ainda não existe; ela nasce junto da tela de login.
export const IR_PARA_LOGIN = new InjectionToken<() => void>('IR_PARA_LOGIN', {
  providedIn: 'root',
  factory: () => {
    const router = inject(Router);
    return () => {
      void router.navigate(['/login']);
    };
  },
});
