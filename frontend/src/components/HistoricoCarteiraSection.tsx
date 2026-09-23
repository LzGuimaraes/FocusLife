import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import HistoricoScoreChart from "./HistoricoScoreChart";
import { Button } from "./Shared";
import { boxStyle } from "./FormStyles";
import type { RegistrarResponse, SerieCarteira } from "../types/historico";
import { fmtMoeda } from "../utils/percentual";
import { formatarData } from "./HistoricoAtivoSection";

/* ══════════════════════════════════════════════════════════════════════
   Evolução da carteira (Fase 5, a partir do histórico da Fase 4).

   Mostra a qualidade média dos ativos avaliados, o déficit/excesso total e o
   valor da carteira a cada dia registrado. O sistema não projeta nada: só
   desenha o que foi registrado.
   ══════════════════════════════════════════════════════════════════════ */

export default function HistoricoCarteiraSection({ carteiraId }: { carteiraId: number }) {
  const [serie, setSerie] = useState<SerieCarteira | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [registrando, setRegistrando] = useState(false);

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const { data } = await api.get<SerieCarteira>(`/historico/carteira/${carteiraId}`);
      setSerie(data);
    } catch {
      setSerie(null);
    } finally {
      setCarregando(false);
    }
  }, [carteiraId]);

  useEffect(() => { carregar(); }, [carregar]);

  const registrar = async () => {
    setRegistrando(true);
    try {
      const { data } = await api.post<RegistrarResponse>("/historico/registrar", {
        carteira_investimento_id: carteiraId,
      });
      toast.success(
        `Snapshot de ${data.data_referencia}: ${data.registrados} novo(s)`
        + (data.atualizados > 0 ? ` e ${data.atualizados} atualizado(s)` : "")
        + ` — ${data.tickers.join(", ")}`);
      carregar();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao registrar o snapshot");
    } finally {
      setRegistrando(false);
    }
  };

  const pontos = (serie?.pontos ?? []).map(p => ({
    label: formatarData(p.data),
    valor: p.quality_medio,
  }));

  const ultimo = serie && serie.pontos.length > 0 ? serie.pontos[serie.pontos.length - 1] : null;

  return (
    <div style={boxStyle}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "10px", marginBottom: "12px" }}>
        <div>
          <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>Evolução da carteira</h3>
          <p style={{ fontSize: "12px", color: "#64748b", margin: "2px 0 0" }}>
            {serie?.dias_registrados ?? 0} dia(s) registrado(s) · média simples dos ativos avaliados
          </p>
        </div>
        <Button size="sm" variant="secondary" onClick={registrar} loading={registrando}>
          📸 Registrar snapshot de hoje
        </Button>
      </div>

      {carregando ? (
        <p style={{ fontSize: "12px", color: "#94a3b8", margin: 0 }}>Carregando evolução...</p>
      ) : (
        <>
          <HistoricoScoreChart pontos={pontos} cor="#6366f1" />

          {ultimo && (
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 150px), 1fr))", gap: "10px", marginTop: "14px" }}>
              <Indicador label="Qualidade média" valor={ultimo.quality_medio != null ? `${ultimo.quality_medio.toFixed(1)}%` : "—"} cor="#6366f1" />
              <Indicador label="Déficit total" valor={serie ? fmtMoeda(ultimo.deficit_total, serie.moeda) : "—"} cor="#1d4ed8" />
              <Indicador label="Excesso total" valor={serie ? fmtMoeda(ultimo.excesso_total, serie.moeda) : "—"} cor="#b45309" />
              <Indicador label="Valor da carteira" valor={serie ? fmtMoeda(ultimo.valor_carteira_total, serie.moeda) : "—"} cor="#047857" />
            </div>
          )}
        </>
      )}
    </div>
  );
}

function Indicador({ label, valor, cor }: { label: string; valor: string; cor: string }) {
  return (
    <div style={{ background: "#f8fafc", border: "1px solid #f1f5f9", borderLeft: `3px solid ${cor}`, borderRadius: "10px", padding: "10px 12px" }}>
      <p style={{ fontSize: "10px", fontWeight: 700, color: "#64748b", textTransform: "uppercase", margin: "0 0 2px" }}>{label}</p>
      <p style={{ fontSize: "15px", fontWeight: 800, color: cor, margin: 0 }}>{valor}</p>
    </div>
  );
}
