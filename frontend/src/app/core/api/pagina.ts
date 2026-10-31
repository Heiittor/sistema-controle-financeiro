// Formato único de toda listagem paginada da API. "pagina" começa em 0.
export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
}
