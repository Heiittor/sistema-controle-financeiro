import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { API_URL } from '../api/api-url';
import { ehDaApi, ehRotaPublica } from '../api/rotas-api';
import { IR_PARA_LOGIN } from '../sessao/ir-para-login';
import { SessaoService } from '../sessao/sessao.service';

export const erroInterceptor: HttpInterceptorFn = (req, next) => {
  const apiUrl = inject(API_URL);
  const sessao = inject(SessaoService);
  const irParaLogin = inject(IR_PARA_LOGIN);

  return next(req).pipe(
    catchError((erro: unknown) => {
      // IMPORTANTE: só 401 de rota PROTEGIDA da nossa API é sessão inválida.
      // O 401 do login é "senha errada" e não pode limpar nada nem redirecionar.
      if (
        erro instanceof HttpErrorResponse &&
        erro.status === 401 &&
        ehDaApi(req.url, apiUrl) &&
        !ehRotaPublica(req, apiUrl)
      ) {
        // expirar() só devolve true para quem tinha sessão: várias respostas 401 simultâneas
        // redirecionam uma única vez, porque a primeira já limpou a sessão.
        if (sessao.expirar()) {
          irParaLogin();
        }
      }
      // O erro segue intacto para a tela mostrar a mensagem. 429 e demais não são repetidos aqui.
      return throwError(() => erro);
    }),
  );
};
