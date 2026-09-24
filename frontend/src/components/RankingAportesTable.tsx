import type { RankingAportes, StatusElegibilidade, StatusNivel, AcaoAtivo } from "../types/aporte";
import { ESTRATEGIAS_APORTE } from "../types/aporte";
import ScoreBadge from "./ScoreBadge";
import { catInfo, fmtMoeda, fmtPercentual } from "../utils/percentual";
import { fmtScore, scoreBg, scoreColor } from "../utils/avaliacao";
import { miniLabel } from "./FormStyles";
/* ══════════════════════════════════════════════════════════════════════
   Ranking de aporte — com as quatro perguntas SEPARADAS na tela:

     ATIVOS ELEGÍVEIS    quem pode receber agora (e quanto recebe)
     ATIVOS DESCARTADOS  quem ficou de fora e POR QUÊ

   Um ativo com Quality Score 97 e preço acima do limite de compra aparece na
   segunda lista, com o motivo PREÇO ACIMA DO LIMITE — nunca no ranking.
   ══════════════════════════════════════════════════════════════════════ */

export default function RankingAportesTable({ ranking, titulo }: { ranking: RankingAportes; titulo?: string }) {
  const { itens, moeda, avisos } = ranking;
  const comSugestao = itens.some(i => i.sugestao_aporte != null);
  const estrategia = ESTRATEGIAS_APORTE.find(e => e.value === ranking.estrategia_aporte);
  // A LISTA SEPARA o que pode receber do que foi descartado: o motivo do
  // descarte é tão importante quanto o ranking.
  const elegiveis = itens.filter(i => i.elegivel);
  const descartados = itens.filter(i => !i.elegivel);

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
            Déficit → <strong>elegibilidade</strong> → <strong>Priority Score</strong> → alocação
            {estrategia && <> · dentro da classe: {estrategia.label}</>}
          </p>
          <p style={{ fontSize: "12px", color: "#475569", margin: "8px 0 0" }}>
            <strong style={{ color: "#047857" }}>{ranking.total_elegiveis ?? elegiveis.length} elegível(is)</strong>
            {descartados.length > 0 && (
              <> · <strong style={{ color: "#b91c1c" }}>{descartados.length} descartado(s)</strong></>
            )}
          </p>
        </div>
        {comSugestao && ranking.valor_aporte != null && (
          <div style={{ textAlign: "right", fontSize: "12px", color: "#64748b" }}>
            <div>
              Aporte de <strong>{fmtMoeda(ranking.valor_aporte, moeda)}</strong>
              {ranking.valor_orcamento != null && ranking.valor_vendas != null && ranking.valor_vendas > 0 && (
                <> + vendas sugeridas <strong style={{ color: "#b45309" }}>{fmtMoeda(ranking.valor_vendas, moeda)}</strong>
                  {' '}= orçamento <strong>{fmtMoeda(ranking.valor_orcamento, moeda)}</strong></>
              )}
            </div>
            <div>
              Alocado: <strong style={{ color: "#047857" }}>{fmtMoeda(ranking.valor_alocado, moeda)}</strong>
              {ranking.valor_nao_alocado != null && ranking.valor_nao_alocado > 0 && (
                <> · não alocado: <strong style={{ color: "#b45309" }}>{fmtMoeda(ranking.valor_nao_alocado, moeda)}</strong></>
              )}
            </div>
            {ranking.valor_nao_alocado != null && ranking.valor_nao_alocado > 0 && ranking.nao_alocado_explicacao && (
              <div style={{ fontSize: "11px", color: "#b45309", maxWidth: "420px", marginTop: "4px" }}>
                {ranking.nao_alocado_explicacao}
              </div>
            )}
            <button type="button" onClick={() => exportarPlano(ranking)}
              title="Baixar o plano de aporte em CSV (o que aportar, o que reduzir e por quê)"
              style={{ marginTop: "8px", border: "1px solid #e2e8f0", background: "#fff", color: "#475569",
                fontSize: "11px", fontWeight: 700, padding: "4px 10px", borderRadius: "8px", cursor: "pointer" }}>
              ⬇ Exportar plano (CSV)
            </button>
          </div>
        )}
      </div>

      {ranking.alertas.length > 0 && (
        <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "10px", padding: "10px 14px", marginBottom: "12px" }}>
          <p style={{ ...miniLabel, marginBottom: "5px" }}>Alertas do motor de decisão</p>
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

      {/* Rebalanceamento: o que passou do alvo e poderia financiar os déficits. */}
      <RebalanceamentoAporte ranking={ranking} />

      {/* Cenários e precedência das travas — transparência, nada escondido. */}
      <CenariosAporte ranking={ranking} />

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
          <table style={{ width: "100%", borderCollapse: "collapse", minWidth: comSugestao ? "1360px" : "1120px", fontSize: "13px" }}>
            <thead>
              <tr style={{ background: "#f8fafc" }}>
                {["#", "Ativo", "Classe", "Quality", "Momento", "Status", "Ação", "Priority", "Preço", "Limite", "Oport.", "Atual", "Ideal", "Déficit", "Capacidade", "Prior."]
                  .map(h => <th key={h} style={{ ...th, textAlign: ["#", "Ativo", "Classe", "Status", "Ação"].includes(h) ? "left" : "right" }}>{h}</th>)}
                {comSugestao && <th style={{ ...th, textAlign: "right" }}>Aporte</th>}
              </tr>
            </thead>
            <tbody>
              {elegiveis.map(i => {
                const info = catInfo(i.classe);
                const semMetaPropria = i.meta_id == null;
                return (
                  <tr key={i.ativo_cadastro_id ?? `nome:${i.ticker}`} style={{ borderTop: "1px solid #f1f5f9" }}>
                    <td style={{ ...td, color: "#94a3b8", fontWeight: 700 }}>{i.posicao}</td>
                    <td style={{ ...td, fontWeight: 700, color: "#0f172a" }}>
                      {i.ticker ?? "—"}
                      {!i.vinculado && (
                        <span title="Posição sem ticker de catálogo (renda fixa/caixinha): a meta é o percentual da subclasse"
                          style={{ marginLeft: "8px", fontSize: "10px", fontWeight: 700, color: "#92400e", background: "#fffbeb", border: "1px solid #fde68a", padding: "1px 7px", borderRadius: "9999px" }}>
                          sem ticker
                        </span>
                      )}
                    </td>
                    <td style={{ ...td, color: "#475569", whiteSpace: "nowrap" }}>
                      {info.icon} {info.label}
                      {i.subclasse_nome && (
                        <div style={{ fontSize: "11px", color: "#94a3b8" }}>{i.subclasse_nome}</div>
                      )}
                    </td>
                    <td style={{ ...td, textAlign: "right" }}>
                      {i.qualidade_avaliada
                        ? <ScoreBadge score={i.quality_score} size="sm" />
                        : <span title="Ativo ainda sem avaliação: o termo de qualidade não entra na conta dele"
                            style={naoAvaliado}>sem aval.</span>}
                    </td>
                    <td style={{ ...td, textAlign: "right" }}>
                      {i.momento_avaliado ? (
                        <span title={`Fator de momento aplicado: ${i.fator_momento}`}
                          style={{ display: "inline-flex", flexDirection: "column", alignItems: "flex-end" }}>
                          <ScoreBadge score={i.momento_score} size="sm" />
                          <span style={{ fontSize: "10px", color: i.fator_momento <= 0 ? "#b91c1c" : i.fator_momento < 1 ? "#b45309" : "#047857" }}>
                            fator {i.fator_momento}
                          </span>
                        </span>
                      ) : (
                        <span title="Sem checklist de momento/valuation: o fator fica NEUTRO (1) — não é penalidade"
                          style={naoAvaliado}>sem momento</span>
                      )}
                    </td>
                    <td style={td}>
                      <span title={i.motivo ?? ""}
                        style={{ fontSize: "11px", fontWeight: 700, padding: "2px 9px", borderRadius: "9999px", whiteSpace: "nowrap",
                          color: statusCor(i.status), background: statusBg(i.status) }}>
                        {statusLabel(i.status)}
                      </span>
                    </td>
                    <td style={td}>
                      <span title={i.motivo ?? ""}
                        style={{ fontSize: "11px", fontWeight: 700, padding: "2px 9px", borderRadius: "9999px", whiteSpace: "nowrap",
                          color: acaoCor(i.acao), background: acaoBg(i.acao) }}>
                        {acaoLabel(i.acao)}
                      </span>
                    </td>
                    <td style={{ ...td, textAlign: "right" }}>
                      <span title={i.formula ?? ""}
                        style={{
                        display: "inline-flex", alignItems: "center", gap: "6px", justifyContent: "flex-end",
                        padding: "2px 10px", borderRadius: "9999px", fontWeight: 700, fontSize: "12px",
                        background: scoreBg(i.priority_score), color: scoreColor(i.priority_score),
                      }}>
                        {fmtScore(i.priority_score)}
                      </span>
                    </td>
                    <td style={{ ...td, textAlign: "right", color: "#475569", whiteSpace: "nowrap" }}>
                      {i.preco_atual != null ? fmtMoeda(i.preco_atual, moeda) : <span style={{ color: "#cbd5e1" }}>—</span>}
                      {i.preco_medio != null && (
                        <div title="Preço médio pago (informativo)" style={{ fontSize: "10px", color: "#94a3b8" }}>
                          méd. {fmtMoeda(i.preco_medio, moeda)}
                        </div>
                      )}
                    </td>
                    <td style={{ ...td, textAlign: "right", color: i.preco_maximo_compra != null ? "#0f172a" : "#cbd5e1", whiteSpace: "nowrap" }}>
                      {i.preco_maximo_compra != null ? fmtMoeda(i.preco_maximo_compra, moeda) : "sem regra"}
                    </td>
                    <td style={{ ...td, textAlign: "right", color: "#475569", whiteSpace: "nowrap" }}
                      title="Oportunidade de preço: quanto o preço atual está abaixo do seu limite de compra">
                      {i.oportunidade_preco != null ? fmtPercentual(i.oportunidade_preco * 100) : "—"}
                    </td>
                    <td style={{ ...td, textAlign: "right", color: "#475569" }}>{fmtPercentual(i.percentual_atual)}</td>
                    <td style={{ ...td, textAlign: "right", color: "#475569" }}>
                      {semMetaPropria ? <span style={{ color: "#cbd5e1" }}>subclasse</span> : fmtPercentual(i.percentual_ideal)}
                    </td>
                    <td style={{ ...td, textAlign: "right", fontWeight: 600, color: i.deficit > 0 ? "#1d4ed8" : "#cbd5e1" }}>
                      {semMetaPropria ? <span style={{ color: "#cbd5e1" }}>—</span> : fmtMoeda(i.deficit, moeda)}
                    </td>
                    <td style={{ ...td, textAlign: "right", color: i.capacidade_aporte > 0 ? "#0f172a" : "#cbd5e1", fontWeight: 600 }}>
                      {fmtMoeda(i.capacidade_aporte, moeda)}
                    </td>
                    <td style={{ ...td, textAlign: "right", color: i.prioridade_manual > 0 ? "#6366f1" : "#cbd5e1", fontWeight: 700 }}>
                      {i.prioridade_manual}
                    </td>
                    {comSugestao && (
                      <td style={{ ...td, textAlign: "right", fontWeight: 800, color: (i.sugestao_aporte ?? 0) > 0 ? "#047857" : "#cbd5e1" }}>
                        <span title={i.motivo ?? ""}>
                          {fmtMoeda(i.sugestao_aporte ?? 0, moeda)}
                        </span>
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
            Estas posições <strong>não recebem aporte</strong> enquanto a regra não for atendida — mesmo com déficit
            ou Quality Score alto. O déficit continua existindo na carteira.
          </p>
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "900px", fontSize: "13px" }}>
              <thead>
                <tr style={{ background: "#fef2f2" }}>
                  {["Ativo", "Classe", "Quality", "Preço", "Limite", "Capacidade", "Motivo"]
                    .map(h => <th key={h} style={{ ...th, textAlign: ["Ativo", "Classe", "Motivo"].includes(h) ? "left" : "right" }}>{h}</th>)}
                </tr>
              </thead>
              <tbody>
                {descartados.map(i => {
                  const info = catInfo(i.classe);
                  return (
                    <tr key={i.ativo_cadastro_id ?? `nome:${i.ticker}`} style={{ borderTop: "1px solid #fee2e2" }}>
                      <td style={{ ...td, fontWeight: 700, color: "#0f172a" }}>{i.ticker ?? "—"}</td>
                      <td style={{ ...td, color: "#475569", whiteSpace: "nowrap" }}>
                        {info.icon} {info.label}
                        {i.subclasse_nome && <div style={{ fontSize: "11px", color: "#94a3b8" }}>{i.subclasse_nome}</div>}
                      </td>
                      <td style={{ ...td, textAlign: "right" }}>
                        {i.qualidade_avaliada ? <ScoreBadge score={i.quality_score} size="sm" /> : <span style={naoAvaliado}>—</span>}
                      </td>
                      <td style={{ ...td, textAlign: "right", color: "#475569", whiteSpace: "nowrap" }}>
                        {i.preco_atual != null ? fmtMoeda(i.preco_atual, moeda) : "—"}
                      </td>
                      <td style={{ ...td, textAlign: "right", color: "#475569", whiteSpace: "nowrap" }}>
                        {i.preco_maximo_compra != null ? fmtMoeda(i.preco_maximo_compra, moeda) : "—"}
                      </td>
                      <td style={{ ...td, textAlign: "right", color: "#475569", whiteSpace: "nowrap" }}>
                        {fmtMoeda(i.capacidade_aporte, moeda)}
                      </td>
                      <td style={{ ...td, maxWidth: "440px" }}>
                        <span style={{ fontSize: "11px", fontWeight: 700, padding: "2px 9px", borderRadius: "9999px", whiteSpace: "nowrap",
                          color: statusCor(i.status), background: statusBg(i.status), marginRight: "8px" }}>
                          {statusLabel(i.status)}
                        </span>
                        <span style={{ fontSize: "11.5px", color: "#64748b" }}>
                          {i.motivos_inelegibilidade.length > 0
                            ? i.motivos_inelegibilidade.join(" ")
                            : (i.bloqueios.length > 0 ? i.bloqueios.join("; ") : i.motivo ?? "")}
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
        Priority Score = ordem entre os ativos ELEGÍVEIS, segundo os SEUS pesos (qualidade + déficit − excesso +
        prioridade + momento + oportunidade de preço) · Quality Score = notas dos checklists de Qualidade ·
        Momento = fator 0–1 dos checklists de Momento (não altera a qualidade) ·
        Capacidade = déficit + tolerância, respeitando o limite de concentração.
      </p>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════
   CENÁRIOS — como o aporte mudaria com outros pesos de decisão.
   Não altera nada da carteira: é comparação, para o usuário escolher.
   ══════════════════════════════════════════════════════════════════════ */
function CenariosAporte({ ranking }: { ranking: RankingAportes }) {
  const { cenarios, moeda } = ranking;
  const precedencia = ranking.precedencia ?? [];
  if (cenarios.length === 0 && precedencia.length === 0) return null;

  return (
    <div style={{ marginBottom: "16px" }}>
      {cenarios.length > 0 && (
        <>
          <h4 style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a", margin: "0 0 6px" }}>
            Como ficaria com outros critérios (cenários)
          </h4>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 240px), 1fr))", gap: "10px", marginBottom: "10px" }}>
            {cenarios.map(c => (
              <div key={c.nome} style={{ border: "1px solid #e2e8f0", borderRadius: "10px", padding: "10px 12px", background: "#fcfdff" }}>
                <p style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a", margin: 0 }}>{c.nome}</p>
                <p style={{ fontSize: "11px", color: "#64748b", margin: "2px 0 6px" }}>{c.descricao}</p>
                <p style={{ fontSize: "12px", margin: 0 }}>
                  Alocado <strong style={{ color: "#047857" }}>{fmtMoeda(c.valor_alocado, moeda)}</strong>
                  {c.valor_nao_alocado > 0 && (
                    <> · sem destino <strong style={{ color: "#b45309" }}>{fmtMoeda(c.valor_nao_alocado, moeda)}</strong></>
                  )}
                </p>
                {c.itens.length > 0 && (
                  <ul style={{ margin: "6px 0 0", paddingLeft: "16px", fontSize: "11px", color: "#475569" }}>
                    {c.itens.map(i => (
                      <li key={i.ticker}>{i.ticker} — {fmtMoeda(i.valor, moeda)}</li>
                    ))}
                  </ul>
                )}
                <p style={{ fontSize: "10px", color: "#94a3b8", margin: "6px 0 0" }}>{c.pesos}</p>
              </div>
            ))}
          </div>
        </>
      )}

      {precedencia.length > 0 && (
        <details style={{ fontSize: "11px", color: "#64748b" }}>
          <summary style={{ cursor: "pointer", fontWeight: 600 }}>
            Ordem em que o motor aplica as regras (precedência)
          </summary>
          <ol style={{ margin: "6px 0 0", paddingLeft: "18px" }}>
            {precedencia.map(p => <li key={p}>{p}</li>)}
          </ol>
          <p style={{ margin: "6px 0 0", color: "#94a3b8" }}>
            A ordem decide COMO o descarte é explicado (o status do ativo é a primeira trava violada nesta ordem).
            Nenhuma trava é desligada por causa dela.
          </p>
        </details>
      )}
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════
   REBALANCEAMENTO — o que passou do alvo + tolerância.

   O motor NUNCA vende nada: ele mostra quanto de cada nível está acima do
   alvo e quanto isso financiaria os déficits. Só o nível mais específico
   que explica o excesso aparece (o filho abate o pai), para o mesmo dinheiro
   não ser contado duas vezes.
   ══════════════════════════════════════════════════════════════════════ */
function RebalanceamentoAporte({ ranking }: { ranking: RankingAportes }) {
  const { moeda } = ranking;
  const rebalanceamento = ranking.rebalanceamento ?? [];
  if (!ranking.rebalancear) return null;

  return (
    <div style={{ marginBottom: "16px" }}>
      <h4 style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a", margin: "0 0 6px" }}>
        Rebalanceamento — o que passou do alvo
      </h4>
      {rebalanceamento.length === 0 ? (
        <p style={{ fontSize: "12px", color: "#64748b", margin: 0 }}>
          Nada acima do alvo + tolerância: a carteira está dentro da faixa em todos os níveis. Nenhuma
          redução sugerida — o plano usa apenas o valor do aporte.
        </p>
      ) : (
        <>
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "12px", minWidth: "620px" }}>
              <thead>
                <tr style={{ background: "#fff7ed" }}>
                  <th style={{ ...th, textAlign: "left" }}>Nível</th>
                  <th style={{ ...th, textAlign: "left" }}>Quem</th>
                  <th style={{ ...th, textAlign: "right" }}>Atual</th>
                  <th style={{ ...th, textAlign: "right" }}>Ideal</th>
                  <th style={{ ...th, textAlign: "right" }}>Reduzir</th>
                </tr>
              </thead>
              <tbody>
                {rebalanceamento.map((r, i) => (
                  <tr key={`${r.nivel}-${r.nome}-${i}`} style={{ borderTop: "1px solid #fed7aa" }}>
                    <td style={{ ...td, color: "#b45309", fontWeight: 700 }}>{r.nivel.toLowerCase()}</td>
                    <td style={{ ...td, fontWeight: 600, color: "#0f172a" }}>
                      {r.nome}
                      <div style={{ fontSize: "11px", color: "#94a3b8", fontWeight: 400 }}>{r.motivo}</div>
                    </td>
                    <td style={{ ...td, textAlign: "right", color: "#475569" }}>{fmtPercentual(r.percentual_atual)}</td>
                    <td style={{ ...td, textAlign: "right", color: "#475569" }}>{fmtPercentual(r.percentual_ideal)}</td>
                    <td style={{ ...td, textAlign: "right", fontWeight: 800, color: "#b45309" }}>
                      {fmtMoeda(r.sugerido_vender, moeda)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p style={{ fontSize: "11px", color: "#b45309", margin: "8px 0 0" }}>
            Total sugerido de venda: <strong>{fmtMoeda(ranking.valor_vendas, moeda)}</strong> — usado como orçamento
            extra do plano. É sugestão: nada é vendido automaticamente, e a decisão continua sua.
          </p>
        </>
      )}
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════
   Onde entra o dinheiro, no nível em que a decisão é tomada: a CLASSE (e a
   subclasse). O ticker é destino, não ponto de partida.
   ══════════════════════════════════════════════════════════════════════ */
function AportePorClasse({ ranking }: { ranking: RankingAportes }) {
  const { classes, valor_aporte, moeda } = ranking;
  if (classes.length === 0) return null;

  const base = (valor_aporte ?? 0) > 0 ? (valor_aporte as number) : 0;
  const ordenadas = [...classes].sort((a, b) => (b.sugerido - a.sugerido) || (b.deficit - a.deficit));

  return (
    <div style={{ marginBottom: "16px" }}>
      <h4 style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a", margin: "0 0 4px" }}>
        Déficit por classe — classes e subclasses
      </h4>
      <p style={{ fontSize: "11.5px", color: "#64748b", margin: "0 0 10px" }}>
        A classe define o quanto; o ativo só escolhe dentro do orçamento dela
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
                {noAlvo
                  ? <span style={{ color: "#b45309" }}> · acima do alvo em {fmtMoeda(c.excesso, moeda)}</span>
                  : <span style={{ color: "#1d4ed8" }}> · falta {fmtMoeda(c.deficit, moeda)}</span>}
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
                        {s.motivo && <span style={{ color: "#94a3b8" }}> · {s.motivo}</span>}
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
   EXPORTAÇÃO DO PLANO — CSV com o que aportar, o que reduzir e por quê.

   Exporta exatamente o que está na tela (mesmos números, mesma explicação):
   não recalcula nada no navegador, para o arquivo nunca divergir do motor.
   ══════════════════════════════════════════════════════════════════════ */
function exportarPlano(ranking: RankingAportes) {
  const { moeda } = ranking;
  const num = (v: number | null | undefined) =>
    (v == null ? "" : v.toFixed(2).replace(".", ","));
  const celula = (v: string | null | undefined) => `"${(v ?? "").replace(/"/g, "'")}"`;

  const linhas: string[] = [];

  linhas.push([
    celula("TIPO"), celula("NIVEL"), celula("ATIVO"), celula("CLASSE"), celula("STATUS"),
    celula("QUALITY"), celula("PRECO_ATUAL"), celula("PRECO_MEDIO"), celula("PRECO_MAXIMO"),
    celula("OPORTUNIDADE"), celula("DEFICIT"), celula("CAPACIDADE"), celula("PRIORITY_SCORE"),
    celula(`VALOR (${moeda})`), celula("MOTIVO"),
  ].join(";"));

  for (const i of ranking.itens) {
    const valor = (i.elegivel && (i.sugestao_aporte ?? 0) > 0) ? i.sugestao_aporte : null;
    linhas.push([
      celula(i.elegivel ? i.acao : "DESCARTADO"),
      celula("ATIVO"),
      celula(i.ticker ?? "(sem ticker)"),
      celula(i.classe),
      celula(i.status),
      celula(num(i.quality_score)),
      celula(num(i.preco_atual)),
      celula(num(i.preco_medio)),
      celula(num(i.preco_maximo_compra)),
      celula(i.oportunidade_preco != null ? num(i.oportunidade_preco * 100) : ""),
      celula(num(i.deficit)),
      celula(num(i.capacidade_aporte)),
      celula(num(i.priority_score)),
      celula(num(valor)),
      celula(i.motivo),
    ].join(";"));
  }

  for (const r of ranking.rebalanceamento ?? []) {
    linhas.push([
      celula("REDUZIR"), celula(r.nivel), celula(r.nome), celula(r.classe), celula(""),
      celula(""), celula(""), celula(""), celula(""), celula(""), celula(""), celula(""), celula(""),
      celula(num(r.sugerido_vender)), celula(r.motivo),
    ].join(";"));
  }

  linhas.push([
    celula("RESUMO"), celula("-"), celula("-"), celula("-"), celula("-"),
    celula("-"), celula("-"), celula("-"), celula("-"), celula("-"), celula("-"), celula("-"),
    celula(`${ranking.total_elegiveis ?? ""} elegíveis / ${ranking.total_descartados ?? ""} descartados`),
    celula(num(ranking.valor_alocado)),
    celula(`aporte ${num(ranking.valor_aporte)} + vendas sugeridas ${num(ranking.valor_vendas)} = orçamento ${num(ranking.valor_orcamento)}; não alocado ${num(ranking.valor_nao_alocado)}`),
  ].join(";"));

  // BOM para o Excel abrir os acentos corretamente.
  const blob = new Blob(["\uFEFF" + linhas.join("\r\n")], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  const hoje = new Date().toISOString().slice(0, 10);
  link.href = url;
  link.download = `plano-aporte-${ranking.carteira_id}-${hoje}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

const th: React.CSSProperties = {
  padding: "9px 10px", fontSize: "11px", fontWeight: 700, color: "#64748b",
  textTransform: "uppercase", letterSpacing: "0.3px",
};
const td: React.CSSProperties = { padding: "9px 10px", fontSize: "13px" };
const naoAvaliado: React.CSSProperties = {
  fontSize: "11px", fontWeight: 600, color: "#94a3b8", background: "#f1f5f9",
  padding: "2px 8px", borderRadius: "9999px",
};

/* AÇÃO recomendada: MANTER ≠ APORTAR — o ativo continua na carteira sem dinheiro novo. */
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
}[a] ?? "#64748b");

const acaoBg = (a: AcaoAtivo): string => ({
  APORTAR: "#ecfdf5",
  MANTER: "#f1f5f9",
  NAO_APORTAR: "#fef2f2",
  AVALIAR: "#eff6ff",
}[a] ?? "#f1f5f9");

/* STATUS de elegibilidade: a PRIMEIRA trava violada na ordem configurada. */
const statusLabel = (s: StatusElegibilidade): string => ({
  ELEGIVEL: "elegível",
  SEM_AVALIACAO: "sem avaliação",
  PRECO_ACIMA_DO_LIMITE: "preço acima do limite",
  CRITERIO_ELIMINATORIO: "critério eliminatório",
  LIMITE_ATINGIDO: "limite atingido",
  MOMENTO_ZERO: "momento zero",
  CLASSE_SEM_CAPACIDADE: "classe sem capacidade",
  SEM_CAPACIDADE: "sem capacidade",
}[s] ?? s);

const statusCor = (s: StatusElegibilidade): string => ({
  ELEGIVEL: "#047857",
  SEM_AVALIACAO: "#64748b",
  PRECO_ACIMA_DO_LIMITE: "#b91c1c",
  CRITERIO_ELIMINATORIO: "#b91c1c",
  LIMITE_ATINGIDO: "#b45309",
  MOMENTO_ZERO: "#b45309",
  CLASSE_SEM_CAPACIDADE: "#64748b",
  SEM_CAPACIDADE: "#64748b",
}[s] ?? "#64748b");

const statusBg = (s: StatusElegibilidade): string => ({
  ELEGIVEL: "#ecfdf5",
  SEM_AVALIACAO: "#f1f5f9",
  PRECO_ACIMA_DO_LIMITE: "#fef2f2",
  CRITERIO_ELIMINATORIO: "#fef2f2",
  LIMITE_ATINGIDO: "#fffbeb",
  MOMENTO_ZERO: "#fffbeb",
  CLASSE_SEM_CAPACIDADE: "#f1f5f9",
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

const alertaIcone = (tipo: string): string => {
  if (tipo === "BLOQUEIO" || tipo === "ATIVO_LIMITE" || tipo === "PRECO_ACIMA") return "⛔";
  if (tipo === "MOMENTO_ZERO") return "⏸";
  if (tipo === "NAO_ALOCADO") return "💤";
  if (tipo === "SEM_AVALIACAO" || tipo === "SEM_SUBCLASSE" || tipo === "SEM_PRECO_MAXIMO") return "⚠";
  if (tipo === "CLASSE_ACIMA" || tipo === "SUBCLASSE_ACIMA") return "📈";
  return "📉";
};

const alertaCor = (tipo: string): string => {
  if (tipo === "BLOQUEIO" || tipo === "ATIVO_LIMITE" || tipo === "PRECO_ACIMA") return "#b91c1c";
  if (tipo === "NAO_ALOCADO" || tipo === "MOMENTO_ZERO") return "#b45309";
  if (tipo === "SEM_AVALIACAO" || tipo === "SEM_SUBCLASSE" || tipo === "SEM_PRECO_MAXIMO") return "#92400e";
  return "#334155";
};
