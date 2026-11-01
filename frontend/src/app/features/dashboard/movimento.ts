import { DestroyRef, InjectionToken, inject, signal } from '@angular/core';

// A preferência pode mudar sem recarregar a página.
export const MOVIMENTO_REDUZIDO = new InjectionToken('MOVIMENTO_REDUZIDO', {
  providedIn: 'root',
  factory: () => {
    if (typeof matchMedia !== 'function') return signal(true).asReadonly();
    const media = matchMedia('(prefers-reduced-motion: reduce)');
    const reduzido = signal(media.matches);
    const atualizar = () => reduzido.set(media.matches);
    media.addEventListener('change', atualizar);
    inject(DestroyRef).onDestroy(() => media.removeEventListener('change', atualizar));
    return reduzido.asReadonly();
  },
});
export const DURACAO_ENTRADA = 1000;
export const QUADRO_ANIMACAO = new InjectionToken('QUADRO_ANIMACAO', {
  providedIn: 'root',
  factory: () => ({
    agendar: (callback: FrameRequestCallback) => requestAnimationFrame(callback),
    cancelar: (id: number) => cancelAnimationFrame(id),
    agora: () => performance.now(),
  }),
});
