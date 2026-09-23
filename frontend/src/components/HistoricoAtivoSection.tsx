import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import HistoricoScoreChart from "./HistoricoScoreChart";
import ScoreBadge from "./ScoreBadge";
import { Button } from "./Shared";
import { boxStyle } from "./FormStyles";
import type { RegistrarResponse, SerieAtivo } from "../types/historico";
import { fmtScore } from "../utils/avaliacao";

/* ══════════════════════════════════════════════════════════════════════
   Histórico de um ativo (Módulo 8).

   Mostra a evolução do Quality Score e permite registrar o retrato do dia.
   O registro é por DIA (idempotente): reavaliar e registrar de novo no mesmo
   dia atualiza a linha, sem duplicar.
   ══════════════════════════════════════════════════════════════════════ */

interface Props {
  ativoCadastroId: string | null;
  ativoId: number | null;
}

export default function HistoricoAtivoSection({ ativoCadastroId, ativoId }: Props) {
  const [serie, setSerie] = useState<SerieAtivo | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [registrando, setRegistrando] = useState(false);

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const query = ativoCadastroId
        ? `ativo_cadastro_id=${encodeURIComponent(ativoCadastroId)}`
        : `ativo_id=${ativoId}`;
      const { data } = await api.get<SerieAtivo>(`/historico/serie?${query}`);
      setSerie(data);
    } catch {
      setSerie(null);
    } finally {
      setCarregando(false);
    }
  }, [ativoCadastroId, ativoId]);

  useEffect(() => { carregar(); }, [carregar]);

  const registrar = async () => {
    setRegistrando(true);
    try {
      const { data } = await api.post<RegistrarResponse>("/historico/registrar", {});
      toast.success(
        `Histórico registrado em ${data.data_referencia}: ${data.registrados} novo(s)`
        + (data.atualizados > 0 ? ` e ${data.atualizados} atualizado(s)` : "")
        + ".");
      carregar();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao registrar o histórico");
    } finally {
      setRegistrando(false);
    }
  };

  const pontos = (serie?.pontos ?? [])
    .filter(p => p.quality_score != null)
    .map(p => ({ label: formatarData(p.data), valor: p.quality_score }));

  return (
    <div style={{ ...boxStyle, marginTop: "16px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "10px", marginBottom: "12px" }}>
        <div>
          <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>Histórico de avaliações</h3>
          <p style={{ fontSize: "12px", color: "#64748b", margin: "2px 0 0" }}>
            Um registro por dia — reavaliar e registrar de novo no mesmo dia atualiza o dia.
          </p>
        </div>
        <div style={{ display: "flex", gap: "8px", alignItems: "center", flexWrap: "wrap" }}>
          {serie && (
            <ScoreBadge score={serie.ultimo_score} label="Atual" size="sm" />
          )}
          <Button size="sm" variant="secondary" onClick={registrar} loading={registrando}>
            📸 Registrar agora
          </Button>
        </div>
      </div>

      {carregando ? (
        <p style={{ fontSize: "12px", color: "#94a3b8", margin: 0 }}>Carregando histórico...</p>
      ) : (
        <>
          <HistoricoScoreChart pontos={pontos} cor="#8b5cf6" />

          {serie && serie.variacao != null && (
            <p style={{ fontSize: "12px", color: "#64748b", margin: "10px 0 0" }}>
              De <strong>{fmtScore(serie.primeiro_score)}</strong> para <strong>{fmtScore(serie.ultimo_score)}</strong>
              {" "}({serie.variacao > 0 ? "+" : ""}{serie.variacao?.toFixed(2)} p.p. em {serie.pontos.length} registro(s))
            </p>
          )}

          {serie && serie.pontos.length > 0 && (
            <div style={{ marginTop: "12px", overflowX: "auto" }}>
              <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "12px", minWidth: "560px" }}>
                <thead>
                  <tr style={{ background: "#f8fafc" }}>
                    {["Data", "Quality", "Contribution", "% atual", "% ideal", "Déficit"].map((h, i) => (
                      <th key={h} style={{ padding: "7px 10px", textAlign: i === 0 ? "left" : "right", fontSize: "10px", fontWeight: 700, color: "#64748b", textTransform: "uppercase" }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {[...serie.pontos].reverse().map(p => (
                    <tr key={p.data} style={{ borderTop: "1px solid #f1f5f9" }}>
                      <td style={{ padding: "7px 10px", fontWeight: 600, color: "#334155" }}>{formatarData(p.data)}</td>
                      <td style={{ padding: "7px 10px", textAlign: "right", fontWeight: 700, color: "#8b5cf6" }}>{fmtScore(p.quality_score)}</td>
                      <td style={{ padding: "7px 10px", textAlign: "right", color: "#475569" }}>{fmtScore(p.contribution_score)}</td>
                      <td style={{ padding: "7px 10px", textAlign: "right", color: "#475569" }}>{p.percentual_atual != null ? `${p.percentual_atual.toFixed(2)}%` : "—"}</td>
                      <td style={{ padding: "7px 10px", textAlign: "right", color: "#475569" }}>{p.percentual_ideal != null ? `${p.percentual_ideal.toFixed(2)}%` : "—"}</td>
                      <td style={{ padding: "7px 10px", textAlign: "right", color: p.deficit ? "#1d4ed8" : "#cbd5e1" }}>
                        {p.deficit != null ? `R$ ${p.deficit.toFixed(2)}` : "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}

/** "2026-09-23" → "23/09" */
export function formatarData(iso: string): string {
  const [, mes, dia] = iso.slice(0, 10).split("-");
  return `${dia}/${mes}`;
}
