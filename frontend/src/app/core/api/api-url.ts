import { InjectionToken } from '@angular/core';
import { environment } from '../../../environments/environment';

// Único ponto que lê a URL da API. Os serviços e o interceptor recebem por injeção.
export const API_URL = new InjectionToken<string>('API_URL', {
  providedIn: 'root',
  factory: () => environment.apiUrl,
});
