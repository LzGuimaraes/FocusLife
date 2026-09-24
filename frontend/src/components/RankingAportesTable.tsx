import type { RankingAportes, StatusElegibilidade, StatusNivel, AcaoAtivo } from "../types/aporte";
import { catInfo, compraEmCotas, fmtMoeda, fmtPercentual } from "../utils/percentual";
import { fmtUnidades } from "../utils/numeros";
import { fmtScore, scoreBg, scoreColor } from "../utils/avaliacao";
import { miniLabel } from "./FormStyles";
/* ══════════════════════════════════════════════════════════════════════
   Ranking de aporte — as quatro perguntas na tela, sem número escondido:

     ONDE FALTA       déficit por classe e subclasse
     QUEM PODE        ativos elegíveis (e por que os outros ficaram de fora)
     QUEM VEM ANTES   NOTA do checklist
     QUANTO ENTRA     sugestão de rateio, com teto no déficit

   A ordem é a nota: ativo sem nota entra na fila depois de quem já foi
   avaliado — e a tela diz "avaliar", em vez de esconder.
   ══════════════════════════════════════════════════════════════════════ */

export default function RankingAportesTable({ ranking, titulo }: { ranking: RankingAportes; titulo?: string }) {
  const { itens, moeda, avisos } = ranking;
  const elegiveis = itens.filter(i => i.elegivel);
  const descartados = itens.filter(i => !i.elegivel);
  const comSugestao = itens.some(i => (i.sugestao_aporte ?? 0) > 0);

  return (
    <div style={{
      background: "white", borderRadius: "12px", padding: "18px",
      boxShadow: "0 1px 3px rgba(0,0,0,0.06)", border: "1px solid #f1f5f9",
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "10px", marginBottom: "12px" }}>
        <div>
          <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>
            {titulo ?? "Prioridade de aporte"}
          </h3>
          <p style={{ fontSize: "12px", color: "#64748b", margin: "2px 0 0" }}>
            Onde falta → quem pode → <strong>nota do checklist</strong> → quanto entra
          </p>
          <p style={{ fontSize: "12px", color: "#475569", margin: "8px 0 0" }}>
            <strong style={{ color: "#047857" }}>{ranking.total_elegiveis} elegível(is)</strong>
            {descartados.length > 0 && (
              <> · <strong style={{ color: "#b91c1c" }}>{descartados.length} descartado(s)</strong></>
            )}
          </p>
        </div>
        {comSugestao && ranking.valor_aporte != null && (
          <div style={{ textAlign: "right", fontSize: "12px", color: "#64748b" }}>
            <div>
              Aporte de <strong>{fmtMoeda(ranking.valor_aporte, moeda)}</strong>
            </div>
            <div title="Margem operacional: o quanto o ativo pode passar da meta antes de ser considerado cheio">
              Margem: <strong>{fmtPercentual(ranking.margem_percentual)}</strong>
            </div>
            <div>
              Alocado: <strong style={{ color: "#047857" }}>{fmtMoeda(ranking.valor_alocado ?? 0, moeda)}</strong>
              {(ranking.valor_nao_alocado ?? 0) > 0 && (
                <> · não alocado: <strong style={{ color: "#b45309" }}>{fmtMoeda(ranking.valor_nao_alocado ?? 0, moeda)}</strong></>
              )}
            </div>
            {(ranking.valor_nao_alocado ?? 0) > 0 && ranking.nao_alocado_explicacao && (
              <div style={{ fontSize: "11px", color: "#b45309", maxWidth: "420px", marginTop: "4px" }}>
                {ranking.nao_alocado_explicacao}
              </div>
            )}
            <button type="button" onClick={() => exportarPlano(ranking)}
              title="Baixar o plano de aporte em CSV (o que aportar e a nota de cada um)"
              style={{ marginTop: "8px", border: "1px solid #e2e8f0", background: "#fff", color: "#475569",
                fontSize: "11px", fontWeight: 700, padding: "4px 10px", borderRadius: "8px", cursor: "pointer" }}>
              ⬇ Exportar plano (CSV)
            </button>
          </div>
        )}
      </div>

      {ranking.alertas.length > 0 && (
        <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "10px", padding: "10px 14px", marginBottom: "12px" }}>
          <p style={{ ...miniLabel, marginBottom: "5px" }}>Alertas do motor</p>
          {ranking.alertas.map((a, i) => (
            <p key={i} style={{ fontSize: "12px", color: alertaCor(a.tipo), margin: i === 0 ? 0 : "3px 0 0" }}>
              {alertaIcone(a.tipo)} {a.mensagem}
            </p>
          ))}
        </div>
      )}

      {avisos.length > 0 && (
        <div style={{ background: "#fffbeb", border: "1px solid #fde68a", borderRadius: "10px", padding: "10px 14px", marginBottom: "14px" }}>
          {avisos.map((a, i) => (
            <p key={i} style={{ fontSize: "12px", color: "#92400e", margin: i === 0 ? 0 : "4px 0 0" }}>⚠ {a}</p>
          ))}
        </div>
      )}

      {/* Onde o dinheiro entra: a decisão é da classe/subclasse, antes de olhar ticker. */}
      <AportePorClasse ranking={ranking} />

      {/* ══ ATIVOS ELEGÍVEIS ══ */}
      <h4 style={{ fontSize: "13px", fontWeight: 800, color: "#047857", margin: "0 0 8px" }}>
        Ativos elegíveis ({elegiveis.length})
      </h4>
      {elegiveis.length === 0 ? (
        <p style={{ fontSize: "12.5px", color: "#94a3b8", margin: "0 0 16px" }}>
          Nenhum ativo pode receber aporte agora. Veja os descartes abaixo — o déficit continua existindo na carteira.
        </p>
      ) : (
        <div style={{ overflowX: "auto", marginBottom: "20px" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", minWidth: comSugestao ? "1000px" : "820px", fontSize: "13px" }}>
            <thead>
              <tr style={{ background: "#f8fafc" }}>
                {["#", "Ativo", "Classe", "Nota", "Status", "Ação",
                  comSugestao ? "Atual (após)" : "Atual", "Meta (após)", "Déficit", "Limite", "Capacidade"]
                  .map(h => <th key={h} style={{ ...th, textAlign: ["#", "Ativo", "Classe", "Status", "Ação"].includes(h) ? "left" : "right" }}
                    title={h.includes("(após)") ? "Percentual considerando o aporte informado (o alvo cresce com o patrimônio final)" : undefined}>{h}</th>)}
                {comSugestao && <th style={{ ...th, textAlign: "right" }}>Aporte</th>}
              </tr>
            </thead>
            <tbody>
              {elegiveis.map(i => {
                const info = catInfo(i.classe);
                return (
                  <tr key={i.ticker ?? `id:${i.ativo_cadastro_id}`} style={{ borderTop: "1px solid #f1f5f9" }}>
                    <td style={{ ...td, color: "#94a3b8", fontWeight: 700 }}>{i.posicao}</td>
                    <td style={{ ...td, fontWeight: 700, color: "#0f172a" }}>
                      {i.ticker ?? "—"}
                      {i.subclasse_nome && <div style={{ fontSize: "11px", color: "#94a3b8", fontWeight: 500 }}>{i.subclasse_nome}</div>}
                    </td>
                    <td style={{ ...td, color: "#475569", whiteSpace: "nowrap" }}>{info.icon} {info.label}</td>
                    <td style={{ ...td, textAlign: "right" }}>
                      {i.nota != null
                        ? <span style={{ ...scorePill, color: scoreColor(i.nota), background: scoreBg(i.nota) }}>{fmtScore(i.nota)}</span>
                        : <span style={{ fontSize: "11px", color: "#b45309" }}>sem nota</span>}
                      <div style={{ fontSize: "10.5px", color: "#94a3b8" }}>{i.respondidas}/{i.perguntas} perguntas</div>
                    </td>
                    <td style={{ ...td, whiteSpace: "nowrap" }}>
                      <span style={{ fontSize: "11px", fontWeight: 700, padding: "2px 9px", borderRadius: "9999px",
                        color: statusCor(i.status), background: statusBg(i.status) }}>
                        {statusLabel(i.status)}
                      </span>
                    </td>
                    <td style={{ ...td, fontSize: "11.5px", fontWeight: 700, color: acaoCor(i.acao), whiteSpace: "nowrap" }}>
                      {acaoLabel(i.acao)}
                    </td>
                    <td style={{ ...td, textAlign: "right", color: "#475569" }}>{fmtPercentual(i.percentual_atual)}</td>
                    <td style={{ ...td, textAlign: "right", fontWeight: 700, color: "#4338ca" }}>
                      {i.meta_id == null
                        ? <span title="Posição sem meta própria: o alvo dela é o da subclasse/classe" style={{ color: "#cbd5e1" }}>—</span>
                        : fmtPercentual(i.percentual_ideal)}
                    </td>
                    <td style={{ ...td, textAlign: "right", color: i.deficit > 0 ? "#1d4ed8" : "#cbd5e1" }}>
                      {fmtMoeda(i.deficit, moeda)}
                    </td>
                    {/* LIMITE = min(meta × (1+margem), limite cadastrado). É dele que sai
                        a CAPACIDADE — a meta sozinha não bloqueia ninguém. */}
                    <td style={{ ...td, textAlign: "right", color: "#475569" }}
                      title="Limite operacional: meta + margem (ou o limite cadastrado, se for menor)">
                      {i.limite_percentual != null
                        ? fmtPercentual(i.limite_percentual)
                        : <span style={{ color: "#cbd5e1" }} title="Posição sem meta própria: ela herda o alvo da subclasse/classe">—</span>}
                      {i.limite_em_reais != null && (
                        <div style={{ fontSize: "10.5px", color: "#94a3b8" }}>{fmtMoeda(i.limite_em_reais, moeda)}</div>
                      )}
                    </td>
                    <td style={{ ...td, textAlign: "right", color: "#475569" }}>{fmtMoeda(i.capacidade_aporte, moeda)}</td>
                    {comSugestao && (
                      <td style={{ ...td, textAlign: "right", fontWeight: 800, color: (i.sugestao_aporte ?? 0) > 0 ? "#047857" : "#cbd5e1" }}>
                        {fmtMoeda(i.sugestao_aporte ?? 0, moeda)}
                        {i.quantidade != null && i.quantidade > 0 && (
                          <div style={{ fontSize: "11px", fontWeight: 700, color: "#0f172a", whiteSpace: "nowrap" }}>
                            {fmtUnidades(i.quantidade, compraEmCotas(i.classe))} × {fmtMoeda(i.preco_unitario ?? 0, moeda)}
                          </div>
                        )}
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* ══ ATIVOS DESCARTADOS ══ */}
      {descartados.length > 0 && (
        <>
          <h4 style={{ fontSize: "13px", fontWeight: 800, color: "#b91c1c", margin: "0 0 4px" }}>
            Ativos descartados ({descartados.length})
          </h4>
          <p style={{ fontSize: "12px", color: "#64748b", margin: "0 0 10px" }}>
            Estas posições <strong>não recebem aporte</strong> enquanto a regra não for atendida — mesmo com nota alta.
            Estar na meta NÃO descarta: o que descarta é critério eliminatório, falta de espaço até o
            <strong> limite operacional</strong> ou a classe fora da Carteira Ideal.
          </p>
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "800px", fontSize: "13px" }}>
              <thead>
                <tr style={{ background: "#fef2f2" }}>
                  {["Ativo", "Classe", "Nota", "Atual (após)", "Limite", "Capacidade", "Motivo"]
                    .map(h => <th key={h} style={{ ...th, textAlign: ["Ativo", "Classe", "Motivo"].includes(h) ? "left" : "right" }}>{h}</th>)}
                </tr>
              </thead>
              <tbody>
                {descartados.map(i => {
                  const info = catInfo(i.classe);
                  return (
                    <tr key={i.ticker ?? `id:${i.ativo_cadastro_id}`} style={{ borderTop: "1px solid #fee2e2" }}>
                      <td style={{ ...td, fontWeight: 700, color: "#0f172a" }}>
                        {i.ticker ?? "—"}
                        {i.subclasse_nome && <div style={{ fontSize: "11px", color: "#94a3b8", fontWeight: 500 }}>{i.subclasse_nome}</div>}
                      </td>
                      <td style={{ ...td, color: "#475569", whiteSpace: "nowrap" }}>{info.icon} {info.label}</td>
                      <td style={{ ...td, textAlign: "right" }}>
                        {i.nota != null
                          ? <span style={{ ...scorePill, color: scoreColor(i.nota), background: scoreBg(i.nota) }}>{fmtScore(i.nota)}</span>
                          : <span style={{ fontSize: "11px", color: "#b45309" }}>sem nota</span>}
                      </td>
                      <td style={{ ...td, textAlign: "right", color: "#475569" }}>{fmtPercentual(i.percentual_atual)}</td>
                      <td style={{ ...td, textAlign: "right", color: "#b45309" }}>
                        {i.limite_percentual != null ? fmtPercentual(i.limite_percentual) : "—"}
                      </td>
                      <td style={{ ...td, textAlign: "right", color: "#cbd5e1" }}>{fmtMoeda(i.capacidade_aporte, moeda)}</td>
                      <td style={{ ...td, maxWidth: "440px" }}>
                        <span style={{ fontSize: "11px", fontWeight: 700, padding: "2px 9px", borderRadius: "9999px", whiteSpace: "nowrap",
                          color: statusCor(i.status), background: statusBg(i.status), marginRight: "8px" }}>
                          {statusLabel(i.status)}
                        </span>
                        <span style={{ fontSize: "11.5px", color: "#64748b" }}>
                          {i.motivos_inelegibilidade.length > 0
                            ? i.motivos_inelegibilidade.join(" ")
                            : (i.bloqueios.length > 0 ? i.bloqueios.join("; ") : (i.motivo ?? ""))}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}

      <p style={{ fontSize: "11px", color: "#94a3b8", marginTop: "14px", marginBottom: 0 }}>
        A ordem do aporte é a <strong>nota do checklist</strong> (por subclasse, uma nota por ativo) ·
        A compra é em <strong>cotas inteiras</strong> (ação, FII e ETF) pelo preço atual — o valor de cada
        ativo é arredondado para baixo e o que não fecha uma cota volta para o pool do aporte ·
        Ninguém recebe mais do que o espaço até o <strong>limite operacional</strong>
        (meta + margem de <strong>{fmtPercentual(ranking.margem_percentual)}</strong>, ou o limite cadastrado
        quando for menor); o que sobra depois disso fica não alocado.
        {ranking.valor_total_com_aporte != null && (
          <> Os alvos são calculados sobre o patrimônio <strong>depois</strong> do aporte
            ({fmtMoeda(ranking.valor_total, ranking.moeda)} + {fmtMoeda(ranking.valor_aporte ?? 0, ranking.moeda)} ={" "}
            {fmtMoeda(ranking.valor_total_com_aporte, ranking.moeda)}), então os percentuais da tabela
            já mostram onde cada ativo ficaria sem o dinheiro novo — e é por isso que um ativo
            <strong> na meta</strong> continua candidato.</>
        )}
      </p>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════
   ONDE ENTRA O DINHEIRO — classe → subclasse
   ══════════════════════════════════════════════════════════════════════ */

function AportePorClasse({ ranking }: { ranking: RankingAportes }) {
  const { classes, valor_aporte, moeda } = ranking;
  if (classes.length === 0) return null;

  const base = (valor_aporte ?? 0) > 0 ? (valor_aporte as number) : 0;
  const ordenadas = [...classes].sort((a, b) => (b.sugerido - a.sugerido) || (b.deficit - a.deficit));

  return (
    <div style={{ marginBottom: "16px" }}>
      <h4 style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a", margin: "0 0 4px" }}>
        Onde entra o dinheiro — classe e subclasse
      </h4>
      <p style={{ fontSize: "11.5px", color: "#64748b", margin: "0 0 10px" }}>
        O orçamento de cada nível é a <strong>capacidade elegível</strong> (o que os ativos dela absorvem),
        limitada pelo limite máximo cadastrado quando existir. O déficit é só informativo: ele não reserva dinheiro.
        {ranking.valor_total_com_aporte != null && (
          <> · os alvos já contam o aporte (patrimônio final de {fmtMoeda(ranking.valor_total_com_aporte, moeda)})</>
        )}
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 260px), 1fr))", gap: "10px" }}>
        {ordenadas.map(c => {
          const info = catInfo(c.classe);
          const noAlvo = c.deficit <= 0;
          const pct = (base > 0) ? Math.min(100, (c.sugerido / base) * 100) : 0;
          const subs = c.subclasses.filter(s => s.percentual_ideal > 0 || s.sugerido > 0);
          return (
            <div key={c.classe} style={{ border: "1px solid #e9eef5", borderRadius: "12px", padding: "12px 14px", background: "white" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: "8px" }}>
                <span style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a" }}>{info.icon} {info.label}</span>
                <span style={{ fontSize: "11px", fontWeight: 700, color: statusCorNivel(c.status), background: statusBgNivel(c.status), padding: "2px 9px", borderRadius: "9999px" }}>
                  {statusLabelNivel(c.status)}
                </span>
              </div>
              <div style={{ fontSize: "11.5px", color: "#64748b", marginTop: "5px" }}>
                {fmtPercentual(c.percentual_atual)} → {fmtPercentual(c.percentual_ideal)}
                {c.limite_operacional_percentual != null && (
                  <> · limite {fmtPercentual(c.limite_operacional_percentual)}</>
                )}
                {noAlvo
                  ? <span style={{ color: "#b45309" }}> · acima da meta em {fmtMoeda(c.excesso, moeda)}</span>
                  : <span style={{ color: "#1d4ed8" }}> · falta {fmtMoeda(c.deficit, moeda)} até a meta</span>}
              </div>
              <div style={{ fontSize: "11px", color: "#94a3b8", marginTop: "2px" }}>
                capacidade elegível dos ativos: <strong style={{ color: "#475569" }}>{fmtMoeda(c.capacidade_elegivel, moeda)}</strong>
              </div>

              <div style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "9px" }}>
                <div style={{ flex: 1, height: "8px", background: "#f1f5f9", borderRadius: "5px", overflow: "hidden" }}>
                  <div style={{ width: `${pct}%`, height: "100%", background: info.color, transition: "width 0.35s ease" }} />
                </div>
                <span style={{ fontSize: "12px", fontWeight: 800, color: c.sugerido > 0 ? "#047857" : "#cbd5e1" }}>
                  {fmtMoeda(c.sugerido, moeda)}
                </span>
              </div>
              <p style={{ fontSize: "11px", color: "#94a3b8", margin: "6px 0 0" }}>{c.motivo}</p>

              {subs.length > 0 && (
                <div style={{ marginTop: "8px", paddingTop: "8px", borderTop: "1px solid #f1f5f9", display: "flex", flexDirection: "column", gap: "5px" }}>
                  {subs.map(s => (
                    <div key={s.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "8px" }}>
                      <span style={{ fontSize: "11.5px", color: "#475569" }}>
                        ↳ <strong>{s.nome}</strong>
                        <span style={{ color: "#94a3b8" }}> {fmtPercentual(s.percentual_atual)} → {fmtPercentual(s.percentual_ideal)}</span>
                      </span>
                      <span style={{ fontSize: "11.5px", fontWeight: 700, color: s.sugerido > 0 ? "#047857" : "#cbd5e1", whiteSpace: "nowrap" }}>
                        {fmtMoeda(s.sugerido, moeda)}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════
   Exportação do plano (CSV montado no navegador, a partir do MESMO JSON)
   ══════════════════════════════════════════════════════════════════════ */

function exportarPlano(ranking: RankingAportes) {
  const linhas: string[] = ["ativo;classe;subclasse;nota;atual;meta;deficit;limite;limite_operacional;"
    + "capacidade;sugestao;final;acao;motivo"];
  for (const i of ranking.itens) {
    const valor = i.elegivel && (i.sugestao_aporte ?? 0) > 0 ? i.sugestao_aporte : null;
    linhas.push([
      csv(i.ticker ?? ""),
      csv(catInfo(i.classe).label),
      csv(i.subclasse_nome ?? ""),
      i.nota != null ? String(i.nota).replace(".", ",") : "",
      num(i.valor_atual),
      num(i.percentual_ideal),
      num(i.deficit),
      num(i.limite_percentual),
      num(i.limite_operacional_percentual),
      num(i.capacidade_aporte),
      valor != null ? String(valor).replace(".", ",") : "",
      num((i.valor_atual ?? 0) + (valor ?? 0)),
      acaoLabel(i.acao),
      csv(i.motivo ?? i.motivos_inelegibilidade.join(" ")),
    ].join(";"));
  }
  const blob = new Blob(["\uFEFF" + linhas.join("\n")], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `plano-aporte-${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

const csv = (v: string) => `"${v.replace(/"/g, '""')}"`;

/** Número do CSV em pt-BR (vazio quando não existe). */
const num = (v: number | null | undefined) =>
  v == null ? "" : String(v).replace(".", ",");

/* ── Rótulos e cores ── */

const td = { padding: "8px 10px", verticalAlign: "top" as const };
const th = {
  padding: "8px 10px", fontSize: "10.5px", fontWeight: 700, color: "#64748b",
  textTransform: "uppercase" as const, letterSpacing: "0.3px",
};

const scorePill = {
  fontSize: "11px", fontWeight: 800, padding: "2px 8px",
  borderRadius: "9999px", fontVariantNumeric: "tabular-nums" as const,
};

const statusLabel = (s: StatusElegibilidade): string => ({
  ELEGIVEL: "elegível",
  SEM_AVALIACAO: "sem nota",
  CRITERIO_ELIMINATORIO: "critério eliminatório",
  LIMITE_ATINGIDO: "limite atingido",
  SEM_CAPACIDADE: "sem espaço no limite",
}[s] ?? s);

const statusCor = (s: StatusElegibilidade): string => ({
  ELEGIVEL: "#047857",
  SEM_AVALIACAO: "#b45309",
  CRITERIO_ELIMINATORIO: "#b91c1c",
  LIMITE_ATINGIDO: "#b45309",
  SEM_CAPACIDADE: "#64748b",
}[s] ?? "#64748b");

const statusBg = (s: StatusElegibilidade): string => ({
  ELEGIVEL: "#ecfdf5",
  SEM_AVALIACAO: "#fffbeb",
  CRITERIO_ELIMINATORIO: "#fef2f2",
  LIMITE_ATINGIDO: "#fffbeb",
  SEM_CAPACIDADE: "#f1f5f9",
}[s] ?? "#f1f5f9");

const statusLabelNivel = (s: StatusNivel): string => ({
  ABAIXO: "abaixo do alvo",
  EQUILIBRADO: "equilibrado",
  ACIMA: "acima do alvo",
  SEM_ALVO: "sem alvo",
}[s] ?? s);

const statusCorNivel = (s: StatusNivel): string => ({
  ABAIXO: "#1d4ed8",
  EQUILIBRADO: "#047857",
  ACIMA: "#b45309",
  SEM_ALVO: "#64748b",
}[s] ?? "#64748b");

const statusBgNivel = (s: StatusNivel): string => ({
  ABAIXO: "#eff6ff",
  EQUILIBRADO: "#ecfdf5",
  ACIMA: "#fffbeb",
  SEM_ALVO: "#f1f5f9",
}[s] ?? "#f1f5f9");

const acaoLabel = (a: AcaoAtivo): string => ({
  APORTAR: "aportar",
  MANTER: "manter",
  NAO_APORTAR: "não aportar",
  AVALIAR: "avaliar",
}[a] ?? a);

const acaoCor = (a: AcaoAtivo): string => ({
  APORTAR: "#047857",
  MANTER: "#475569",
  NAO_APORTAR: "#b91c1c",
  AVALIAR: "#1d4ed8",
}[a] ?? "#475569");

const alertaIcone = (tipo: string): string => {
  if (tipo === "BLOQUEIO" || tipo === "ATIVO_LIMITE") return "⛔";
  if (tipo === "NAO_ALOCADO") return "💤";
  if (tipo === "SEM_AVALIACAO") return "⚠";
  return "📉";
};

const alertaCor = (tipo: string): string => {
  if (tipo === "BLOQUEIO" || tipo === "ATIVO_LIMITE") return "#b91c1c";
  if (tipo === "NAO_ALOCADO") return "#b45309";
  if (tipo === "SEM_AVALIACAO") return "#92400e";
  return "#334155";
};
