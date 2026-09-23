import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import { fmtMoeda, fmtPercentual, catInfo } from "../utils/percentual";
import { boxStyle } from "./FormStyles";
import type { AporteRegistro } from "../types/aporte";
import type { CategoriaInvestimento } from "../types/planejamento";

/* ══════════════════════════════════════════════════════════════════════
   Histórico de aportes executados (§24).

   Registra o que o usuário REALMENTE fez — data, valor, preço/quantidade e a
   participação do ativo antes/depois. Serve de registro e alimenta o motor com
   o sinal de concentração recente ("já recebeu R$ X nos últimos 30 dias").
   ══════════════════════════════════════════════════════════════════════ */

interface Props {
  carteiraId: number;
  /** Muda quando um aporte é registrado, para o histórico recarregar. */
  recarregar?: number;
}

export default function HistoricoAportesSection({ carteiraId, recarregar = 0 }: Props) {
  const [itens, setItens] = useState<AporteRegistro[]>([]);
  const [carregando, setCarregando] = useState(true);

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const { data } = await api.get<AporteRegistro[]>("/aportes/historico", {
        params: { carteira_investimento_id: carteiraId },
      });
      setItens(data);
    } catch {
      setItens([]);
    } finally {
      setCarregando(false);
    }
  }, [carteiraId]);

  useEffect(() => { carregar(); }, [carregar, recarregar]);

  const excluir = (id: number) =>
    toast("Excluir este registro de aporte?", {
      action: {
        label: "Sim", onClick: () => toast.promise(api.delete(`/aportes/historico/${id}`), {
          loading: "Excluindo...",
          success: () => { carregar(); return "Registro excluído!"; },
          error: "Erro ao excluir",
        }),
      },
      cancel: { label: "Cancelar", onClick: () => {} },
    });

  const total = itens.reduce((s, i) => s + (i.valor ?? 0), 0);

  return (
    <div style={boxStyle}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: "10px", flexWrap: "wrap", marginBottom: "10px" }}>
        <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>
          Histórico de aportes
        </h3>
        {itens.length > 0 && (
          <span style={{ fontSize: "12px", color: "#64748b" }}>
            {itens.length} lançamento(s) · total {fmtMoeda(total, "BRL")}
          </span>
        )}
      </div>

      {carregando ? (
        <p style={{ fontSize: "12px", color: "#94a3b8", margin: 0 }}>Carregando…</p>
      ) : itens.length === 0 ? (
        <p style={{ fontSize: "13px", color: "#94a3b8", margin: 0 }}>
          Nenhum aporte registrado ainda. Quando você executar a sugestão, clique em
          <strong> Registrar aporte</strong> — o motor passa a avisar quando o mesmo ativo
          recebe dinheiro várias vezes em pouco tempo.
        </p>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px", minWidth: "620px" }}>
            <thead>
              <tr style={{ background: "#f8fafc" }}>
                <th style={{ ...th, textAlign: "left" }}>Data</th>
                <th style={{ ...th, textAlign: "left" }}>Ativo</th>
                <th style={{ ...th, textAlign: "right" }}>Valor</th>
                <th style={{ ...th, textAlign: "right" }}>% antes → depois</th>
                <th style={{ ...th, textAlign: "left" }}>Obs.</th>
                <th style={th} />
              </tr>
            </thead>
            <tbody>
              {itens.map(i => (
                <tr key={i.id} style={{ borderTop: "1px solid #f1f5f9" }}>
                  <td style={{ ...td, color: "#64748b", whiteSpace: "nowrap" }}>
                    {formatarData(i.data)}
                  </td>
                  <td style={{ ...td, fontWeight: 700, color: "#0f172a" }}>
                    {i.ticker ?? "—"}
                    {i.classe && (
                      <span style={{ marginLeft: "8px", fontSize: "11px", fontWeight: 600, color: "#64748b" }}>
                        {iconeClasse(i.classe)}
                      </span>
                    )}
                  </td>
                  <td style={{ ...td, textAlign: "right", fontWeight: 700, color: "#047857" }}>
                    {fmtMoeda(i.valor, "BRL")}
                  </td>
                  <td style={{ ...td, textAlign: "right", color: "#475569" }}>
                    {i.percentual_antes != null ? fmtPercentual(i.percentual_antes) : "—"}
                    {" → "}
                    {i.percentual_depois != null ? fmtPercentual(i.percentual_depois) : "—"}
                  </td>
                  <td style={{ ...td, color: "#94a3b8", fontSize: "12px" }}>{i.observacao ?? ""}</td>
                  <td style={{ ...td, textAlign: "right" }}>
                    <button type="button" onClick={() => excluir(i.id)}
                      aria-label={`Excluir aporte de ${i.ticker ?? "ativo"}`}
                      style={{ background: "transparent", border: "none", cursor: "pointer", color: "#ef4444" }}>
                      🗑
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <p style={{ fontSize: "11px", color: "#94a3b8", margin: "10px 0 0" }}>
        O histórico é informativo: ele mostra a concentração recente por ativo, sem alterar
        automaticamente a sua estratégia.
      </p>
    </div>
  );
}

function iconeClasse(classe: string): string {
  const info = catInfo(classe as CategoriaInvestimento);
  return info ? `${info.icon} ${info.label}` : classe;
}

function formatarData(iso: string): string {
  const [a, m, d] = iso.split("-");
  return d && m && a ? `${d}/${m}/${a}` : iso;
}

const th: React.CSSProperties = {
  padding: "9px 10px", fontSize: "11px", fontWeight: 700, color: "#64748b",
  textTransform: "uppercase", letterSpacing: "0.3px",
};

const td: React.CSSProperties = { padding: "8px 10px" };
