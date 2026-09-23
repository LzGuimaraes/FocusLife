import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/api";
import type { CarteiraResumo, ResumoIdeal } from "../types/planejamento";
import { catInfo, fmtMoeda, fmtPercentual, somaFechada } from "../utils/percentual";

/* ══════════════════════════════════════════════════════════════════════
   Widget do Dashboard: Carteira Atual × Carteira Ideal (Módulo 10).

   Mostra o resumo por classe e o déficit/excesso de cada uma. Quando a
   carteira ainda não tem Carteira Ideal definida, convida o usuário a
   configurá-la (em vez de mostrar uma tabela vazia).
   ══════════════════════════════════════════════════════════════════════ */

export default function ResumoCarteiraIdealCard() {
  const navigate = useNavigate();
  const [carteiras, setCarteiras] = useState<CarteiraResumo[]>([]);
  const [selecionada, setSelecionada] = useState<number | null>(null);
  const [resumo, setResumo] = useState<ResumoIdeal | null>(null);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    api.get("/carteiras-investimento/all?page=0&size=100")
      .then(r => {
        const lista: CarteiraResumo[] = r.data.content ?? [];
        setCarteiras(lista);
        if (lista.length > 0) setSelecionada(lista[0].id);
        else setCarregando(false);
      })
      .catch(() => setCarregando(false));
  }, []);

  useEffect(() => {
    if (selecionada == null) return;
    setCarregando(true);
    api.get<ResumoIdeal>(`/carteiras-investimento/${selecionada}/ideal/resumo`)
      .then(r => setResumo(r.data))
      .catch(() => setResumo(null))
      .finally(() => setCarregando(false));
  }, [selecionada]);

  const semCarteira = carteiras.length === 0;
  const semIdeal = !semCarteira && resumo != null && resumo.total_classes === 0;

  return (
    <div style={{
      background: "white", borderRadius: "12px", padding: "clamp(14px, 2vw, 20px)",
      boxShadow: "0 1px 3px rgba(0,0,0,0.05)", border: "1px solid #f1f5f9", marginBottom: "clamp(16px, 3vw, 28px)",
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "10px", marginBottom: "14px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
          <h2 style={{ fontSize: "clamp(15px, 1.8vw, 18px)", fontWeight: 700, color: "#0f172a", margin: 0 }}>
            🎯 Carteira Atual × Ideal
          </h2>
          {carteiras.length > 1 && (
            <select value={selecionada ?? ""} aria-label="Carteira"
              onChange={e => setSelecionada(Number(e.target.value))}
              style={{ padding: "6px 10px", fontSize: "12px", borderRadius: "8px", border: "1.5px solid #e2e8f0", background: "white", color: "#0f172a" }}>
              {carteiras.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
          )}
          {resumo && resumo.total_classes > 0 && (
            <span style={{
              fontSize: "11px", fontWeight: 700, padding: "3px 10px", borderRadius: "9999px",
              background: somaFechada(resumo.soma_percentuais_ideal) ? "#d1fae5" : "#fef3c7",
              color: somaFechada(resumo.soma_percentuais_ideal) ? "#047857" : "#b45309",
            }}>
              Ideal {fmtPercentual(resumo.soma_percentuais_ideal)}
            </span>
          )}
        </div>
        {!semCarteira && (
          <button type="button"
            onClick={() => navigate(`/planejamento/carteira-ideal/${selecionada}`)}
            style={{ background: "transparent", border: "none", color: "#6366f1", fontSize: "12px", fontWeight: 700, cursor: "pointer" }}>
            {semIdeal ? "Definir Carteira Ideal →" : "Ver planejamento →"}
          </button>
        )}
      </div>

      {carregando && <p style={{ fontSize: "13px", color: "#94a3b8", margin: 0 }}>Carregando...</p>}

      {!carregando && semCarteira && (
        <p style={{ fontSize: "13px", color: "#64748b", margin: 0 }}>
          Você ainda não tem uma carteira de investimento.{" "}
          <button type="button" onClick={() => navigate("/financas")}
            style={{ background: "transparent", border: "none", color: "#6366f1", fontWeight: 700, cursor: "pointer", padding: 0, fontSize: "13px" }}>
            Criar em Finanças →
          </button>
        </p>
      )}

      {!carregando && semIdeal && (
        <p style={{ fontSize: "13px", color: "#64748b", margin: 0 }}>
          Esta carteira ainda não tem uma Carteira Ideal definida.{" "}
          <button type="button" onClick={() => navigate(`/planejamento/carteira-ideal/${selecionada}`)}
            style={{ background: "transparent", border: "none", color: "#6366f1", fontWeight: 700, cursor: "pointer", padding: 0, fontSize: "13px" }}>
            Configurar agora →
          </button>
        </p>
      )}

      {!carregando && resumo && resumo.total_classes > 0 && (
        <>
          <p style={{ fontSize: "12px", color: "#64748b", margin: "0 0 12px" }}>
            Valor total: <strong>{fmtMoeda(resumo.valor_total, resumo.moeda)}</strong> · {resumo.total_ativos_com_meta} meta(s) de ativos
          </p>
          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            {resumo.classes.map(c => {
              const info = catInfo(c.classe);
              const dif = c.percentual_atual - c.percentual_ideal;
              const desvio = Math.abs(dif) < 0.005;
              return (
                <div key={c.classe} style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
                  <span style={{ fontSize: "14px", width: "18px", textAlign: "center" }}>{info.icon}</span>
                  <span style={{ fontSize: "13px", fontWeight: 600, color: "#334155", minWidth: "110px" }}>{info.label}</span>
                  <span style={{ fontSize: "12px", color: "#64748b", minWidth: "150px" }}>
                    {fmtPercentual(c.percentual_atual)} <span style={{ color: "#cbd5e1" }}>de</span> {fmtPercentual(c.percentual_ideal)}
                  </span>
                  <span style={{
                    fontSize: "11px", fontWeight: 700, padding: "3px 10px", borderRadius: "9999px",
                    background: desvio ? "#f1f5f9" : c.excesso > 0 ? "#fef3c7" : "#dbeafe",
                    color: desvio ? "#64748b" : c.excesso > 0 ? "#b45309" : "#1d4ed8",
                  }}>
                    {desvio
                      ? "na meta"
                      : c.excesso > 0
                        ? `excesso ${fmtMoeda(c.excesso, resumo.moeda)}`
                        : `déficit ${fmtMoeda(c.deficit, resumo.moeda)}`}
                  </span>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
