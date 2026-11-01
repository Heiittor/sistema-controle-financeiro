import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_URL } from '../../core/api/api-url';
import { MesDashboard, ResumoDashboard } from './dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly url = inject(API_URL);
  resumo(inicio: string, fim: string) {
    return this.http.get<ResumoDashboard>(this.url + '/dashboard/resumo', {
      params: new HttpParams().set('inicio', inicio).set('fim', fim),
    });
  }
  serie(inicio: string, fim: string) {
    return this.http.get<MesDashboard[]>(this.url + '/dashboard/serie-mensal', {
      params: new HttpParams().set('inicio', inicio).set('fim', fim),
    });
  }
}
