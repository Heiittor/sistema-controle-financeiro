// Datas civis (AAAA-MM-DD) tratadas como TEXTO. Nunca viram timestamp UTC convertido para o fuso local,
// porque isso poderia mudar o dia. O fuso de negócio é o mesmo do backend.
const FUSO_NEGOCIO = 'America/Sao_Paulo';

const formatador = new Intl.DateTimeFormat('en-CA', {
  timeZone: FUSO_NEGOCIO,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

// "Hoje" no fuso de negócio, e não no do navegador.
export function hojeNegocio(agora: Date = new Date()): string {
  const partes = Object.fromEntries(formatador.formatToParts(agora).map((p) => [p.type, p.value]));
  return `${partes['year']}-${partes['month']}-${partes['day']}`;
}

export function primeiroDiaDoMes(data: string): string {
  return `${data.slice(0, 7)}-01`;
}

export function ultimoDiaDoMes(data: string): string {
  const ano = Number(data.slice(0, 4));
  const mes = Number(data.slice(5, 7));
  // Dia 0 do mês seguinte = último dia deste mês. Em UTC, só para a aritmética do calendário.
  const ultimo = new Date(Date.UTC(ano, mes, 0)).getUTCDate();
  return `${data.slice(0, 7)}-${String(ultimo).padStart(2, '0')}`;
}

// AAAA-MM-DD -> DD/MM/AAAA, só reordenando o texto.
export function formatarData(data: string): string {
  const [ano, mes, dia] = data.split('-');
  return `${dia}/${mes}/${ano}`;
}
