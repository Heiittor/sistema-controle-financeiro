import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

// Áreas sem funcionalidade ainda usam "Em construção". Cada fase troca a sua rota pela tela real.
const emConstrucao = () =>
  import('./shared/componentes/em-construcao/em-construcao').then((m) => m.EmConstrucao);

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'cadastro',
    title: 'Criar conta',
    loadComponent: () => import('./features/auth/cadastro/cadastro').then((m) => m.Cadastro),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', title: 'Dashboard', loadComponent: emConstrucao },
      { path: 'contas', title: 'Contas', loadComponent: () => import('./features/contas/contas').then((m) => m.Contas),},
      { path: 'movimentacoes', title: 'Movimentações', loadComponent: emConstrucao },
      { path: 'categorias', title: 'Categorias', loadComponent: () => import('./features/categorias/categorias').then((m) => m.Categorias), },
      { path: 'transferencias', title: 'Transferências', loadComponent: emConstrucao },
      { path: 'orcamentos', title: 'Orçamentos', loadComponent: emConstrucao },
    ],
  },
  {
    path: '**',
    title: 'Página não encontrada',
    loadComponent: () =>
      import('./features/nao-encontrada/nao-encontrada').then((m) => m.NaoEncontrada),
  },
];
