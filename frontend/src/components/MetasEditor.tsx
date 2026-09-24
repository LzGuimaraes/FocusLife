import { useState } from "react";
import type { CategoriaInvestimento } from "../types/planejamento";
import AtivoAutocomplete from "./AtivoAutocomplete";
import { CATEGORIAS, catInfo, classeSugerida, fmtMoeda, fmtPercentual } from "../utils/percentual";
import { novaChave } from "../utils/chaves";
import {
  chip, controlStyle, iconBtn, linkBtnStyle, miniLabel, noticeCard, numMedio, overline,
  sectionCard, sectionSubtitle, sectionTitle, segmentBtn, segmented,
} from "./FormStyles";
import { apenasNumero, textoParaNum } from "../utils/numeros";
import { type ClasseDraft } from "./CarteiraIdealEditor";

/* ══════════════════════════════════════════════════════════════════════
   Metas por ativo (Módulos 1 e 7).

   A lista parte dos ATIVOS QUE O USUÁRIO JÁ TEM na carteira: cada linha já vem
   com o valor e o percentual atuais, e ele só decide o alvo. Nada de
   recadastrar ativos nem planejar num espaço paralelo.

   ORGANIZAÇÃO DA TELA:
     • abas por CLASSE — ninguém precisa rolar uma lista longa para achar FIIs;
     • alternância TABELA × GRADE — a tabela compara números, a grade dá
       alvos maiores para digitar no celular;
     • posições sem ticker saem da tabela e viram um CARD DE AVISO (elas não
       têm meta por ativo, então misturá-las na tabela só criava armadilha).

   Nada de regra muda aqui: os mesmos campos, os mesmos handlers.
   ══════════════════════════════════════════════════════════════════════ */

export interface MetaDraft {
  key: string;
  ativo_cadastro_id: string;
  ticker: string;
  /**
   * Nome como o usuário cadastrou a POSIÇÃO (ex.: "Petrobras PN"). É o que a
   * tabela mostra abaixo do ticker: "PETR4" sozinho não diz nada a quem olha.
   */
  nome: string;
  classe: CategoriaInvestimento;
  subclasse_nome: string;
  /** Setor dentro da subclasse (nível opcional). */
  setor_nome: string;
  percentual_ideal: string;
  /** Linha marcada participa do payload (as desmarcadas perdem a meta). */
  incluir: boolean;
  /** "carteira" = ativo que já existe; "planejado" = ativo que ainda vai comprar. */
  origem: "carteira" | "planejado";
  /** false = posição sem vínculo com o catálogo (não pode ter meta por ticker). */
  vinculado: boolean;
  /** Posições agrupadas nesta linha (para vincular todas de uma vez). */
  ativo_ids: number[];
  sugestao_catalogo_id: string | null;
  sugestao_catalogo_nome: string | null;
  /**
   * Subclasse da Carteira Ideal atribuída À POSIÇÃO (renda fixa / sem ticker).
   * É gravada na posição, não no payload da Carteira Ideal — vale para
   * qualquer posição, com ou sem ticker.
   */
  subclasse_id: number | null;
  subclasse_nome_posicao: string | null;
  /** Setor atribuído À POSIÇÃO (renda fixa / sem ticker). */
  setor_id: number | null;
  setor_nome_posicao: string | null;

  /** Tolerância (p.p. sobre o % ideal) — dentro dela o ativo conta como no alvo. */
  tolerancia: string;
  /** Teto de concentração do ativo (%). */
  limite_maximo: string;

  /* ── Situação atual (informativo, vem das posições) ── */
  percentual_atual: number | null;
  valor_atual: number | null;
}

export function novaMetaPlanejada(classe: CategoriaInvestimento): MetaDraft {
  return {
    key: novaChave(),
    ativo_cadastro_id: "",
    ticker: "",
    nome: "",
    classe,
    subclasse_nome: "",
    setor_nome: "",
    percentual_ideal: "",
    incluir: true,
    origem: "planejado",
    vinculado: true,
    ativo_ids: [],
    sugestao_catalogo_id: null,
    sugestao_catalogo_nome: null,
    subclasse_id: null,
    subclasse_nome_posicao: null,
    setor_id: null,
    setor_nome_posicao: null,
    tolerancia: "",
    limite_maximo: "",
    percentual_atual: null,
    valor_atual: null,
  };
}

interface Props {
  metas: MetaDraft[];
  classes: ClasseDraft[];
  moeda: string;
  valorTotal: number;
  onChange: (metas: MetaDraft[]) => void;
  /** Vincula posições sem catálogo a um ticker (um clique resolve). */
  onVincular: (ativoIds: number[], ativoCadastroId: string) => void;
  /** Classifica posições sem ticker em uma subclasse da Carteira Ideal (null = remover). */
  onAtribuirSubclasse: (ativoIds: number[], subclasseId: number | null) => void;
}

type Visual = "tabela" | "grade";

/** Quantas linhas a tabela mostra antes do "ver mais". */
const PAGINA_ATIVOS = 8;

export default function MetasEditor({
  metas, classes, moeda, valorTotal, onChange, onVincular, onAtribuirSubclasse,
}: Props) {
  const [buscando, setBuscando] = useState<string | null>(null);
  const [visual, setVisual] = useState<Visual>("tabela");
  const [abaClasse, setAbaClasse] = useState<CategoriaInvestimento | null>(null);
  /** Busca pela ação (ticker ou nome) — a lista de metas pode ser longa. */
  const [busca, setBusca] = useState("");
  /** A tabela mostra os 8 primeiros; o resto só no "ver mais". */
  const [mostrarTodas, setMostrarTodas] = useState(false);
  /** O card de aviso de "sem ticker" começa aberto porque pede ação. */
  const [avisoAberto, setAvisoAberto] = useState(true);

  const atualizar = (key: string, patch: Partial<MetaDraft>) =>
    onChange(metas.map(m => (m.key === key ? { ...m, ...patch } : m)));

  const subclassesDaClasse = (classe: CategoriaInvestimento) =>
    classes.find(c => c.classe === classe)?.subclasses ?? [];

  /**
   * Sugere a subclasse pelo NOME da posição ("Reserva de emergência" →
   * "Reserva"). Renda fixa não tem ticker para bater com o catálogo, mas o
   * nome da posição costuma ser o nome do produto — então o match é por texto
   * normalizado (sem acento, minúsculo e sem o ruído de banco/app).
   */
  const sugestaoSubclasse = (nomePosicao: string, subs: { id?: number | null; nome: string }[]) => {
    const norm = (t: string) => t.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-z0-9 ]/g, " ").replace(/\s+/g, " ").trim();
    const alvo = norm(nomePosicao);
    if (alvo.length < 3) return null;
    return subs.find(s => s.id != null && norm(s.nome) === alvo)
      ?? subs.find(s => s.id != null && norm(s.nome).length >= 3
          && (alvo.includes(norm(s.nome)) || norm(s.nome).includes(alvo)))
      ?? null;
  };


  const daCarteira = metas.filter(m => m.origem === "carteira");
  const planejados = metas.filter(m => m.origem === "planejado");
  const semVinculo = daCarteira.filter(m => !m.vinculado);
  /** Lista que entra na tabela/grade: só quem PODE ter meta por ativo. */
  const comVinculo = daCarteira.filter(m => m.vinculado);

  const incluídas = metas.filter(m => m.incluir && m.ativo_cadastro_id);
  const somaIdeal = incluídas.reduce((s, m) => s + textoParaNum(m.percentual_ideal), 0);

  /* ── Abas: uma por classe presente nas metas, na ordem oficial ── */
  const classesNasMetas = CATEGORIAS.map(c => c.key).filter(k => metas.some(m => m.classe === k));
  const classeAtiva = (abaClasse && classesNasMetas.includes(abaClasse))
    ? abaClasse
    : classesNasMetas[0] ?? null;
  const naAba = (m: MetaDraft) => classeAtiva == null || m.classe === classeAtiva;
  const comVinculoDaAba = comVinculo.filter(naAba);
  const planejadosDaAba = planejados.filter(naAba);
  const semVinculoDaAba = semVinculo.filter(naAba);
  const classePadrao = classeAtiva ?? classes[0]?.classe ?? "ACOES";

  /* ── Busca + paginação da tabela ──
     O usuário acompanha poucos ativos por vez: a busca acha o que ele quer e a
     lista abre com os 8 primeiros, para a tela não virar um paredão de linhas. */
  const termoBusca = busca.trim().toLowerCase();
  const filtradasDaAba = comVinculoDaAba.filter(m => termoBusca === ""
    || m.ticker.toLowerCase().includes(termoBusca)
    || m.nome.toLowerCase().includes(termoBusca)
    || m.subclasse_nome.toLowerCase().includes(termoBusca));
  const visiveisNaTabela = mostrarTodas ? filtradasDaAba : filtradasDaAba.slice(0, PAGINA_ATIVOS);

  /** Atalho: registrar a distribuição atual como alvo (um clique, zero digitação). */
  const usarDistribuicaoAtual = () => {
    onChange(metas.map(m => (m.origem === "carteira" && m.vinculado && m.percentual_atual != null
      ? { ...m, incluir: true, percentual_ideal: m.percentual_atual.toFixed(2).replace(".", ",") }
      : m)));
  };

  const limparAlvos = () =>
    onChange(metas.map(m => (m.origem === "carteira"
      ? { ...m, incluir: false, percentual_ideal: "", subclasse_nome: "" }
      : m)));

  /** Card de um ativo da carteira (visual em grade). */
  const cartaoAtivo = (m: MetaDraft) => {
    const info = catInfo(m.classe);
    const subs = subclassesDaClasse(m.classe);
    const temAlvo = m.percentual_ideal.trim() !== "";
    const valorIdeal = valorTotal * (textoParaNum(m.percentual_ideal) / 100);
    return (
      <article key={m.key} style={{
        border: `1px solid ${m.incluir ? "#e0e7ff" : "#eef2f7"}`, borderRadius: "14px",
        padding: "16px", background: m.incluir ? "#fdfdff" : "white",
        display: "flex", flexDirection: "column", gap: "13px",
      }}>
        <header style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
          <input type="checkbox" checked={m.incluir}
            aria-label={`Definir meta para ${m.ticker}`}
            onChange={e => atualizar(m.key, { incluir: e.target.checked })}
            style={{ width: "18px", height: "18px", accentColor: "#6366f1", cursor: "pointer" }} />
          <span style={{ fontSize: "15px", fontWeight: 800, color: "#0f172a" }}>{m.ticker}</span>
          {m.nome.trim() !== "" && m.nome.trim() !== m.ticker.trim() && (
            <span style={{ fontSize: "12.5px", color: "#64748b", fontWeight: 600 }}>{m.nome}</span>
          )}
          <span style={chip(info.color, info.bg)}>{info.icon} {info.label}</span>
          <span style={{ ...numMedio, marginLeft: "auto", color: "#475569" }}>
            {fmtMoeda(m.valor_atual, moeda)}
          </span>
        </header>

        <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", gap: "12px" }}>
          <div>
            <span style={overline}>Hoje</span>
            <p style={{ ...numMedio, color: "#64748b", margin: "3px 0 0" }}>
              {m.percentual_atual != null ? fmtPercentual(m.percentual_atual) : "—"}
            </p>
          </div>
          <div style={{ textAlign: "right" }}>
            <span style={overline}>Alvo (% ideal)</span>
            <div style={{ display: "flex", alignItems: "center", gap: "5px", justifyContent: "flex-end", marginTop: "3px" }}>
              <input value={m.percentual_ideal} disabled={!m.incluir} inputMode="decimal"
                placeholder={m.percentual_atual != null ? m.percentual_atual.toFixed(2).replace(".", ",") : "0,00"}
                aria-label={`Percentual ideal de ${m.ticker}`}
                onChange={e => atualizar(m.key, { percentual_ideal: apenasNumero(e.target.value) })}
                style={{ ...controlStyle, width: "86px", textAlign: "right", fontSize: "16px", fontWeight: 700, opacity: m.incluir ? 1 : 0.5 }} />
              <span style={{ fontSize: "14px", fontWeight: 700, color: "#94a3b8" }}>%</span>
            </div>
          </div>
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", borderTop: "1px solid #f1f5f9", paddingTop: "11px" }}>
          <span style={overline}>Valor ideal</span>
          <span style={{ ...numMedio, color: m.incluir && temAlvo ? "#4338ca" : "#cbd5e1" }}>
            {m.incluir && temAlvo ? fmtMoeda(valorIdeal, moeda) : "—"}
          </span>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: "10px" }}>
          <div>
            <label style={miniLabel}>Tolerância ±</label>
            <input value={m.tolerancia} disabled={!m.incluir} inputMode="decimal" placeholder="0"
              aria-label={`Tolerância de ${m.ticker}`}
              title="Tolerância em pontos percentuais: dentro dela o ativo conta como no alvo — e é o que dá espaço ao aporte quando a meta já foi atingida."
              onChange={e => atualizar(m.key, { tolerancia: apenasNumero(e.target.value) })}
              style={{ ...controlStyle, width: "100%", textAlign: "right", opacity: m.incluir ? 1 : 0.5 }} />
          </div>
          <div>
            <label style={miniLabel}>Limite máx.</label>
            <input value={m.limite_maximo} disabled={!m.incluir} inputMode="decimal" placeholder="—"
              aria-label={`Limite máximo de ${m.ticker}`}
              title="Limite máximo de concentração (%): acima dele o ativo não recebe novos aportes."
              onChange={e => atualizar(m.key, { limite_maximo: apenasNumero(e.target.value) })}
              style={{ ...controlStyle, width: "100%", textAlign: "right", opacity: m.incluir ? 1 : 0.5 }} />
          </div>
        </div>

        <div>
          <label style={miniLabel}>Subclasse</label>
          <select value={m.subclasse_nome} disabled={!m.incluir || subs.length === 0}
            aria-label={`Subclasse de ${m.ticker}`}
            onChange={e => atualizar(m.key, { subclasse_nome: e.target.value, setor_nome: "" })}
            style={{ ...controlStyle, width: "100%", opacity: (!m.incluir || subs.length === 0) ? 0.5 : 1 }}>
            <option value="">—</option>
            {subs.map(s => <option key={s.key} value={s.nome}>{s.nome || "(sem nome)"}</option>)}
          </select>
        </div>
      </article>
    );
  };

  return (
    <section style={sectionCard}>
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "12px" }}>
        <div style={{ maxWidth: "560px" }}>
          <h3 style={sectionTitle}>Metas dos seus ativos</h3>
          <p style={sectionSubtitle}>
            Marque os ativos que devem ter meta e informe o alvo — o percentual atual já vem da sua carteira.
            Nada é recadastrado: a linha é a posição que você já tem.
          </p>
        </div>
        <span style={chip("#4338ca", "#eef2ff")}>
          {incluídas.length} meta(s) · soma {fmtPercentual(somaIdeal)}
        </span>
      </header>

      {/* ── Card de aviso: posições que não podem ter meta por ativo ── */}
      {semVinculo.length > 0 && (
        <div style={{ ...noticeCard("#b45309", "#fffbeb", "#fde68a"), marginTop: "18px" }}>
          <button type="button" onClick={() => setAvisoAberto(v => !v)} aria-expanded={avisoAberto}
            style={{ display: "flex", alignItems: "center", gap: "10px", width: "100%", background: "transparent", border: "none", cursor: "pointer", padding: 0, textAlign: "left" }}>
            <span style={{ fontSize: "18px", lineHeight: 1 }}>⚠️</span>
            <span style={{ flex: 1, minWidth: 0 }}>
              <span style={{ display: "block", fontSize: "13.5px", fontWeight: 800, color: "#92400e" }}>
                {semVinculo.length} posição(ões) sem ticker do catálogo
              </span>
              <span style={{ display: "block", fontSize: "12px", color: "#b45309", marginTop: "3px" }}>
                Renda fixa, Tesouro e caixinhas não têm ticker — então não existe meta por ativo. O valor já conta
                no total e no alvo da CLASSE; classificá-las em uma subclasse diz qual fatia da classe cada uma representa.
              </span>
            </span>
            <span style={{ fontSize: "11px", color: "#b45309", transition: "transform 0.2s ease", transform: avisoAberto ? "rotate(90deg)" : "none" }}>▶</span>
          </button>

          {avisoAberto && (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px", marginTop: "14px" }}>
              {semVinculo.map(m => {
                const info = catInfo(m.classe);
                const subs = subclassesDaClasse(m.classe);
                const sugere = m.subclasse_id == null ? sugestaoSubclasse(m.ticker, subs) : null;
                // Outras posições sem ticker da MESMA classe: classificar uma a uma
                // é o caminho longo quando a carteira tem várias caixinhas.
                const outras = semVinculo.filter(o => o.key !== m.key && o.classe === m.classe
                  && o.subclasse_id == null).flatMap(o => o.ativo_ids);
                return (
                  <div key={m.key} style={{
                    display: "flex", gap: "14px", alignItems: "flex-end", flexWrap: "wrap",
                    background: "white", border: "1px solid #fde68a", borderRadius: "12px", padding: "12px 14px",
                  }}>
                    <div>
                      <span style={{ display: "block", fontSize: "14px", fontWeight: 800, color: "#0f172a" }}>{m.ticker}</span>
                      <span style={{ display: "block", fontSize: "12px", fontWeight: 600, color: "#b45309", marginTop: "2px" }}>
                        {info.icon} {info.label} · {fmtMoeda(m.valor_atual, moeda)} ·{" "}
                        {m.percentual_atual != null ? fmtPercentual(m.percentual_atual) : "—"}
                      </span>
                    </div>

                    {subs.length === 0 ? (
                      <span style={{ fontSize: "12px", color: "#92400e", maxWidth: "340px" }}>
                        Nada a fazer: {info.label} não tem subclasses, então esta posição já conta integralmente no
                        alvo da classe. Se quiser detalhar (ex.: Reserva × CDI), crie as subclasses no editor de classes.
                      </span>
                    ) : (
                      <div>
                        <label style={miniLabel}>Subclasse de {info.label}</label>
                        <select value={m.subclasse_id ?? ""}
                          aria-label={`Subclasse de ${m.ticker}`}
                          onChange={e => {
                            const bruto = e.target.value;
                            onAtribuirSubclasse(m.ativo_ids, bruto === "" ? null : Number(bruto));
                          }}
                          style={{ ...controlStyle, minWidth: "190px", fontSize: "12.5px" }}>
                          <option value="">— sem subclasse —</option>
                          {subs.map(s => (
                            <option key={s.id} value={s.id as number}>{s.nome}</option>
                          ))}
                        </select>
                      </div>
                    )}

                    {sugere && (
                      <button type="button" title="O nome da posição combina com esta subclasse"
                        onClick={() => onAtribuirSubclasse(m.ativo_ids, sugere.id as number)}
                        style={{ ...linkBtnStyle, borderStyle: "solid", borderColor: "#bbf7d0", color: "#047857", background: "white" }}>
                        🔗 usar {sugere.nome}
                      </button>
                    )}

                    {m.subclasse_id != null && outras.length > 0 && (
                      <button type="button"
                        onClick={() => onAtribuirSubclasse(outras, m.subclasse_id)}
                        style={{ ...linkBtnStyle, color: "#4338ca", borderColor: "#c7d2fe" }}>
                        {outras.length === 1
                          ? `aplicar ${m.subclasse_nome_posicao ?? "esta subclasse"} à outra posição de ${info.label}`
                          : `aplicar ${m.subclasse_nome_posicao ?? "esta subclasse"} às outras ${outras.length} posições de ${info.label}`}
                      </button>
                    )}

                    {buscando === m.key ? (
                      <div style={{ minWidth: "220px", flex: "1 1 220px" }}>
                        <label style={miniLabel}>Vincular ao catálogo</label>
                        <AtivoAutocomplete value="" onSelect={a => {
                          onVincular(m.ativo_ids, a.id);
                          setBuscando(null);
                        }} />
                      </div>
                    ) : (
                      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                        {m.sugestao_catalogo_id && (
                          <button type="button"
                            onClick={() => onVincular(m.ativo_ids, m.sugestao_catalogo_id as string)}
                            style={{ ...linkBtnStyle, borderStyle: "solid", borderColor: "#bbf7d0", color: "#047857", background: "white" }}>
                            🔗 vincular a {m.sugestao_catalogo_nome}
                          </button>
                        )}
                        <button type="button" onClick={() => setBuscando(m.key)}
                          style={{ ...linkBtnStyle, color: "#64748b", borderColor: "#e2e8f0" }}>
                          escolher outro ticker
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {metas.length === 0 && (
        <p style={{ fontSize: "13px", color: "#94a3b8", margin: "16px 0 0" }}>
          Esta carteira ainda não tem investimentos cadastrados. Cadastre-os em Finanças ou adicione abaixo um ativo
          que pretende comprar.
        </p>
      )}

      {/* ── Ações rápidas ── */}
      {comVinculo.length > 0 && (
        <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", marginTop: "18px" }}>
          <button type="button" onClick={usarDistribuicaoAtual}
            style={{ ...linkBtnStyle, borderStyle: "solid", borderColor: "#c7d2fe", color: "#4338ca", background: "white" }}>
            ⚡ Usar minha distribuição atual como alvo
          </button>
          <button type="button" onClick={limparAlvos} style={{ ...linkBtnStyle, color: "#64748b", borderColor: "#e2e8f0" }}>
            Limpar alvos
          </button>
        </div>
      )}

      {/* ── Abas por classe + alternância de visual ── */}
      {metas.length > 0 && (
        <div style={{
          display: "flex", justifyContent: "space-between", alignItems: "center", gap: "12px",
          flexWrap: "wrap", marginTop: "18px", paddingBottom: "12px", borderBottom: "1px solid #f1f5f9",
        }}>
          <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
            {classesNasMetas.map(k => {
              const info = catInfo(k);
              const qtd = metas.filter(m => m.classe === k).length;
              const ativo = k === classeAtiva;
              return (
                <button key={k} type="button" onClick={() => setAbaClasse(k)}
                  aria-pressed={ativo}
                  style={{
                    ...segmentBtn(ativo), borderRadius: "10px", display: "inline-flex", alignItems: "center", gap: "7px",
                    border: ativo ? `1.5px solid ${info.color}33` : "1.5px solid transparent",
                  }}>
                  {info.icon} {info.label}
                  <span style={{
                    fontSize: "10.5px", fontWeight: 800, padding: "1px 7px", borderRadius: "9999px",
                    background: ativo ? info.bg : "#e2e8f0", color: ativo ? info.color : "#64748b",
                  }}>{qtd}</span>
                </button>
              );
            })}
          </div>

          <div style={segmented}>
            <button type="button" onClick={() => setVisual("tabela")} style={segmentBtn(visual === "tabela")}
              aria-pressed={visual === "tabela"}>▦ Tabela</button>
            <button type="button" onClick={() => setVisual("grade")} style={segmentBtn(visual === "grade")}
              aria-pressed={visual === "grade"}>▤ Grade</button>
          </div>
        </div>
      )}

      {/* ── Visual em TABELA (comparação numérica) ── */}
      {visual === "tabela" && comVinculoDaAba.length > 0 && (
        <>
          {/* ── Busca + paginação: a lista pode ser longa e o essencial cabe em 8 linhas ── */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap", marginTop: "14px" }}>
            <input value={busca} onChange={e => { setBusca(e.target.value); setMostrarTodas(false); }}
              placeholder="🔍 Buscar pela ação (ticker ou nome)" aria-label="Buscar ativo nas metas"
              style={{ ...controlStyle, minWidth: "240px", flex: "1 1 240px", maxWidth: "360px" }} />
            <span style={{ fontSize: "12px", color: "#64748b" }}>
              {filtradasDaAba.length} ativo(s)
              {termoBusca !== "" && ` para "${busca.trim()}"`}
              {filtradasDaAba.length > PAGINA_ATIVOS && !mostrarTodas && ` · mostrando ${PAGINA_ATIVOS}`}
            </span>
            {filtradasDaAba.length > PAGINA_ATIVOS && (
              <button type="button" onClick={() => setMostrarTodas(v => !v)}
                style={{ ...linkBtnStyle, borderStyle: "solid", borderColor: "#c7d2fe", color: "#4338ca", background: "white" }}>
                {mostrarTodas ? "ver menos" : `ver mais (${filtradasDaAba.length - PAGINA_ATIVOS} restantes)`}
              </button>
            )}
          </div>

          <div style={{ overflowX: "auto", marginTop: "10px" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "620px" }}>
              <thead>
                <tr>
                  <th style={th}>Meta?</th>
                  <th style={{ ...th, textAlign: "left" }}>Ativo</th>
                  <th style={{ ...th, textAlign: "right" }}>Hoje</th>
                  <th style={{ ...th, textAlign: "right" }}>% atual</th>
                  <th style={{ ...th, textAlign: "right" }}>% ideal</th>
                  <th style={{ ...th, textAlign: "right" }}>Valor ideal</th>
                  <th style={{ ...th, textAlign: "right" }}>±</th>
                  <th style={{ ...th, textAlign: "left" }}>Subclasse</th>
                </tr>
              </thead>
              <tbody>
                {visiveisNaTabela.map(m => {
                  const info = catInfo(m.classe);
                  const valorIdeal = valorTotal * (textoParaNum(m.percentual_ideal) / 100);
                  const subs = subclassesDaClasse(m.classe);
                  const temAlvo = m.percentual_ideal.trim() !== "";
                  return (
                    <tr key={m.key} style={{ borderTop: "1px solid #f1f5f9", opacity: m.incluir ? 1 : 0.55 }}>
                      <td style={{ ...td, textAlign: "center" }}>
                        <input type="checkbox" checked={m.incluir}
                          aria-label={`Definir meta para ${m.ticker}`}
                          onChange={e => atualizar(m.key, { incluir: e.target.checked })}
                          style={{ width: "17px", height: "17px", accentColor: "#6366f1", cursor: "pointer" }} />
                      </td>
                      <td style={{ ...td, color: "#0f172a" }}>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                          <span style={{ fontWeight: 800 }}>{m.ticker}</span>
                          <span style={chip(info.color, info.bg)}>{info.icon} {info.label}</span>
                        </div>
                        {m.nome.trim() !== "" && m.nome.trim() !== m.ticker.trim() && (
                          <span style={{ display: "block", fontSize: "12px", color: "#64748b", marginTop: "2px" }}>
                            {m.nome}
                          </span>
                        )}
                      </td>
                      <td style={{ ...td, textAlign: "right", color: "#475569", whiteSpace: "nowrap" }}>
                        {fmtMoeda(m.valor_atual, moeda)}
                      </td>
                      <td style={{ ...td, textAlign: "right", color: "#475569" }}>
                        {m.percentual_atual != null ? fmtPercentual(m.percentual_atual) : "—"}
                      </td>
                      <td style={{ ...td, textAlign: "right" }}>
                        <div style={{ display: "inline-flex", alignItems: "center", gap: "4px" }}>
                          <input value={m.percentual_ideal} disabled={!m.incluir} inputMode="decimal"
                            placeholder={m.percentual_atual != null ? m.percentual_atual.toFixed(2).replace(".", ",") : "0,00"}
                            aria-label={`Percentual ideal de ${m.ticker}`}
                            onChange={e => atualizar(m.key, { percentual_ideal: apenasNumero(e.target.value) })}
                            style={{ ...controlStyle, width: "80px", textAlign: "right", opacity: m.incluir ? 1 : 0.5 }} />
                          <span style={{ fontSize: "12px", color: "#64748b" }}>%</span>
                        </div>
                      </td>
                      <td style={{ ...td, textAlign: "right", fontWeight: 600, whiteSpace: "nowrap", color: m.incluir && temAlvo ? "#4338ca" : "#cbd5e1" }}>
                        {m.incluir && temAlvo ? fmtMoeda(valorIdeal, moeda) : "—"}
                      </td>
                      <td style={{ ...td, textAlign: "right" }}>
                        <input value={m.tolerancia} disabled={!m.incluir} inputMode="decimal" placeholder="0"
                          aria-label={`Tolerância de ${m.ticker}`}
                          title="Tolerância em pontos percentuais: dentro dela o ativo conta como no alvo."
                          onChange={e => atualizar(m.key, { tolerancia: apenasNumero(e.target.value) })}
                          style={{ ...controlStyle, width: "58px", textAlign: "right", fontSize: "12px", opacity: m.incluir ? 1 : 0.5 }} />
                      </td>
                      <td style={td}>
                        <select value={m.subclasse_nome} disabled={!m.incluir || subs.length === 0}
                          aria-label={`Subclasse de ${m.ticker}`}
                          onChange={e => atualizar(m.key, { subclasse_nome: e.target.value, setor_nome: "" })}
                          style={{ ...controlStyle, minWidth: "120px", opacity: (!m.incluir || subs.length === 0) ? 0.5 : 1 }}>
                          <option value="">—</option>
                          {subs.map(s => <option key={s.key} value={s.nome}>{s.nome || "(sem nome)"}</option>)}
                        </select>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {filtradasDaAba.length === 0 && (
            <p style={{ fontSize: "13px", color: "#94a3b8", margin: "14px 0 0" }}>
              Nenhum ativo encontrado para "{busca.trim()}".
            </p>
          )}
        </>
      )}

      {/* ── Visual em GRADE (alvos maiores, melhor no celular) ── */}
      {visual === "grade" && comVinculoDaAba.length > 0 && (
        <div style={{
          display: "grid", gap: "14px", marginTop: "16px",
          gridTemplateColumns: "repeat(auto-fill, minmax(min(100%, 300px), 1fr))",
        }}>
          {comVinculoDaAba.map(cartaoAtivo)}
        </div>
      )}

      {metas.length > 0 && comVinculoDaAba.length === 0 && planejadosDaAba.length === 0 && (
        <p style={{ fontSize: "13px", color: "#94a3b8", margin: "16px 0 0" }}>
          {semVinculoDaAba.length > 0
            ? `Nesta classe só existem posições sem ticker — elas estão no aviso acima, porque não têm meta por ativo.`
            : classeAtiva ? `Nada em ${catInfo(classeAtiva).label} nesta carteira.` : "Nada para configurar aqui."}
        </p>
      )}

      {/* ── Ativos planejados (que ele ainda não tem) ── */}
      {planejadosDaAba.length > 0 && (
        <div style={{ marginTop: "20px", paddingTop: "16px", borderTop: "1px solid #f1f5f9" }}>
          <p style={{ ...overline, marginBottom: "12px" }}>
            Ainda vou comprar {classeAtiva ? `— ${catInfo(classeAtiva).label}` : ""}
          </p>
          <div style={{
            display: "grid", gap: "14px",
            gridTemplateColumns: "repeat(auto-fill, minmax(min(100%, 320px), 1fr))",
          }}>
            {planejadosDaAba.map(m => {
              const subs = subclassesDaClasse(m.classe);
              return (
                <article key={m.key} style={{
                  border: "1px solid #e9eef5", borderRadius: "14px", padding: "16px",
                  background: "white", display: "flex", flexDirection: "column", gap: "13px",
                }}>
                  <AtivoAutocomplete value={m.ticker}
                    onSelect={a => atualizar(m.key, {
                      ativo_cadastro_id: a.id,
                      ticker: a.nome,
                      classe: classeSugerida(a.tipo),
                    })} />

                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                    <div>
                      <label style={miniLabel}>Classe</label>
                      <select value={m.classe} aria-label="Classe da meta"
                        onChange={e => atualizar(m.key, { classe: e.target.value as CategoriaInvestimento, subclasse_nome: "" })}
                        style={{ ...controlStyle, width: "100%" }}>
                        {CATEGORIAS.map(c => <option key={c.key} value={c.key}>{c.icon} {c.label}</option>)}
                      </select>
                    </div>
                    <div>
                      <label style={miniLabel}>Subclasse</label>
                      <select value={m.subclasse_nome} disabled={subs.length === 0} aria-label="Subclasse da meta"
                        onChange={e => atualizar(m.key, { subclasse_nome: e.target.value })}
                        style={{ ...controlStyle, width: "100%", opacity: subs.length === 0 ? 0.5 : 1 }}>
                        <option value="">—</option>
                        {subs.map(s => <option key={s.key} value={s.nome}>{s.nome || "(sem nome)"}</option>)}
                      </select>
                    </div>
                  </div>

                  <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "10px", alignItems: "flex-end" }}>
                    <div>
                      <label style={miniLabel}>% ideal</label>
                      <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                        <input value={m.percentual_ideal} inputMode="decimal" placeholder="0,00"
                          aria-label={`Percentual ideal de ${m.ticker || "ativo planejado"}`}
                          onChange={e => atualizar(m.key, { percentual_ideal: apenasNumero(e.target.value) })}
                          style={{ ...controlStyle, width: "100%", textAlign: "right", fontWeight: 700 }} />
                        <span style={{ fontSize: "13px", color: "#94a3b8" }}>%</span>
                      </div>
                    </div>
                    <button type="button" aria-label="Remover ativo planejado"
                      onClick={() => onChange(metas.filter(x => x.key !== m.key))}
                      style={{ ...iconBtn(false), color: "#ef4444" }}>
                      🗑
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        </div>
      )}

      <div style={{ marginTop: "18px" }}>
        <button type="button" onClick={() => onChange([...metas, novaMetaPlanejada(classePadrao)])} style={linkBtnStyle}>
          + Ativo que ainda não tenho
        </button>
      </div>
    </section>
  );
}

const th: React.CSSProperties = {
  padding: "10px 12px", fontSize: "10.5px", fontWeight: 800, color: "#94a3b8",
  textTransform: "uppercase", letterSpacing: "0.5px", borderBottom: "1px solid #f1f5f9",
  whiteSpace: "nowrap",
};
const td: React.CSSProperties = { padding: "10px 12px", fontSize: "13px" };
