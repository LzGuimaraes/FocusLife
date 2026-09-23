import type { TipoPergunta } from "../types/checklist";

/* ══════════════════════════════════════════════════════════════════════
   Metadados e formatação da avaliação de ativos (Módulos 3 e 5).
   Espelha as regras do backend:
     • NUMERO/PERCENTUAL pontuam por FAIXA cadastrada
     • MULTIPLA_ESCOLHA pontua pela OPÇÃO escolhida
     • TEXTO/LISTA são informativos (nunca entram no score)
   ══════════════════════════════════════════════════════════════════════ */

export interface TipoInfo {
  key: TipoPergunta;
  label: string;
  icon: string;
  hint: string;
  pontua: boolean;
}

export const TIPOS_PERGUNTA: TipoInfo[] = [
  { key: "SIM_NAO", label: "Sim / Não", icon: "✅", hint: "A resposta vale a nota máxima ou zero.", pontua: true },
  { key: "NOTA", label: "Nota", icon: "🔢", hint: "Você digita a nota (0 até a nota máxima).", pontua: true },
  { key: "PERCENTUAL", label: "Percentual", icon: "％", hint: "Você informa o valor (%); a nota vem da faixa.", pontua: true },
  { key: "NUMERO", label: "Número", icon: "#️⃣", hint: "Você informa o valor; a nota vem da faixa.", pontua: true },
  { key: "MULTIPLA_ESCOLHA", label: "Múltipla escolha", icon: "🔘", hint: "Cada opção tem a sua nota.", pontua: true },
  { key: "TEXTO", label: "Texto", icon: "✍️", hint: "Resposta livre, não entra no score.", pontua: false },
  { key: "LISTA", label: "Lista", icon: "📋", hint: "Lista de itens, não entra no score.", pontua: false },
];

export const tipoInfo = (tipo: TipoPergunta): TipoInfo =>
  TIPOS_PERGUNTA.find(t => t.key === tipo) ?? TIPOS_PERGUNTA[0];

export const usaFaixas = (tipo: TipoPergunta): boolean =>
  tipo === "NUMERO" || tipo === "PERCENTUAL";

export const usaOpcoes = (tipo: TipoPergunta): boolean => tipo === "MULTIPLA_ESCOLHA";

export const ehInformativo = (tipo: TipoPergunta): boolean =>
  tipo === "TEXTO" || tipo === "LISTA";

/** Score 0–100 exibido como "83,3%"; vazio quando nada foi pontuado. */
export function fmtScore(valor: number | null | undefined): string {
  if (valor == null) return "—";
  return `${valor.toLocaleString("pt-BR", { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`;
}

/** Cor do score: verde ≥ 80, âmbar ≥ 60, vermelho abaixo (mesma régua do ProgressBar). */
export function scoreColor(valor: number | null | undefined): string {
  if (valor == null) return "#94a3b8";
  if (valor >= 80) return "#047857";
  if (valor >= 60) return "#b45309";
  return "#b91c1c";
}

export function scoreBg(valor: number | null | undefined): string {
  if (valor == null) return "#f1f5f9";
  if (valor >= 80) return "#d1fae5";
  if (valor >= 60) return "#fef3c7";
  return "#fee2e2";
}

/** Texto curto descrevendo uma faixa: "15% a 25%", "acima de 15%", "até 10%". */
export function fmtFaixa(min: number | null, max: number | null, percentual = false): string {
  const u = percentual ? "%" : "";
  if (min != null && max != null) return `${min}${u} a ${max}${u}`;
  if (min != null) return `acima de ${min}${u}`;
  if (max != null) return `até ${max}${u}`;
  return "qualquer valor";
}
