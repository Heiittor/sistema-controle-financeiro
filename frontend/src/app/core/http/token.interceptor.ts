import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_URL } from '../api/api-url';
import { ehDaApi, ehRotaPublica } from '../api/rotas-api';
import { IR_PARA_LOGIN } from '../sessao/ir-para-login';
import { SessaoService } from '../sessao/sessao.service';

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const apiUrl = inject(API_URL);
  const sessao = inject(SessaoService);
  const irParaLogin = inject(IR_PARA_LOGIN);

  // IMPORTANTE: o token só vai para a nossa API, e nunca para as rotas públicas.
  if (!ehDaApi(req.url, apiUrl) || ehRotaPublica(req, apiUrl)) {
    return next(req);
  }

  const haviaSessao = sessao.autenticado();
  const token = sessao.token();

  if (token === null) {
    // Havia sessão e ela venceu agora: avisa e leva ao login (uma vez, pois a sessão já foi limpa).
    if (haviaSessao) {
      irParaLogin();
    }
    // Sem token a API responde 401; o interceptor de erros não faz nada, pois não há mais sessão.
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
