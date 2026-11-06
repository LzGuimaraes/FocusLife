export type StatusAtividade = 'NAO_INICIADA' | 'EM_ANDAMENTO' | 'PAUSADA' | 'CONCLUIDA' | 'CANCELADA';
export interface Progresso { id: number; requestId: string; data: string; modo: 'INCREMENTO' | 'ACUMULADO'; quantidade: number; observacao: string | null; motivacao: number | null }
export interface Material { id: number; version: number; titulo: string; url: string; descricao: string | null; atividadeId: number | null }
export interface Indicadores {
  realizado: number; percentual: number | null; restante: number | null; esperado: number | null; diferenca: number | null;
  ritmoObservado: number | null; projetadoNoPrazo: number | null; conclusaoEstimada: string | null; concluidaEm: string | null;
  necessarioDia: number | null; necessarioSemana: number | null; praticoDia: number | null; praticoSemana: number | null;
  diasObservados: number; diasDisponiveis: number; baseDias: string; posicao: string; tendencia: string; situacaoPrazo: string;
  mensagem: string; recomendacao: string;
}
export interface Sessao { id: number; inicio: string; fim: string | null; duracaoSegundos: number }
export interface Estudo { id: number; nome: string; data: string; duracaoMin: number; notas: string | null }
export interface Atividade {
  id: number; version: number; titulo: string; descricao: string | null; inicio: string | null; prazo: string | null;
  unidade: string | null; fracionavel: boolean; total: number | null; saldoInicial: number; saldoData: string | null;
  status: StatusAtividade; prioridade: string | null; horario: string | null; diasSemana: string[]; agrupamento: string | null;
  origem: string | null; percentualLegado: number | null; referencia: string; indicadores: Indicadores; historico: Progresso[];
  grafico: { data: string; realizado: number; planejado: number | null }[]; materiais: Material[]; totalSegundos: number; sessoes: Sessao[]; estudos: Estudo[];
}
export const statusNomes: Record<StatusAtividade, string> = { NAO_INICIADA: 'Não iniciada', EM_ANDAMENTO: 'Em andamento', PAUSADA: 'Pausada', CONCLUIDA: 'Concluída', CANCELADA: 'Cancelada' };
export const numero = (n: number | null | undefined) => n == null ? '—' : new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(n);
export const dataLocal = (d: string | null) => d ? new Date(`${d}T12:00:00`).toLocaleDateString('pt-BR') : 'Não informado';
export const hojeCuiaba = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Cuiaba', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
