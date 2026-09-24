import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import { PageHeader, EmptyState, Spinner } from "../components/UI";
import { Button } from "../components/Shared";
import RankingAportesTable from "../components/RankingAportesTable";
import DonutAporte from "../components/GraficosAporte";
import HistoricoAportesSection from "../components/HistoricoAportesSection";
import HistoricoCarteiraSection from "../components/HistoricoCarteiraSection";
import PlanejamentoNav from "../components/PlanejamentoNav";
import type { CarteiraResumo } from "../types/planejamento";
import type { RankingAportes } from "../types/aporte";
import { boxStyle, controlStyle, miniLabel } from "../components/FormStyles";
import { apenasNumero, textoParaNum } from "../utils/numeros";
import { fmtMoeda } from "../utils/percentual";
/* ══════════════════════════════════════════════════════════════════════
   Próximos Aportes (Módulos 7, 8 e 9 — Fase 5).

   A tela operacional: escolhe a carteira, informa quanto pretende aportar e
   vê a ordem de prioridade + quanto iria para cada ativo, segundo a SUA
   configuração de pesos (tela de Pontuação). Abaixo, a evolução da carteira
   registrada no histórico.
   ══════════════════════════════════════════════════════════════════════ */

export default function ProximosAportes() {
  const navigate = useNavigate();
  const [carteiras, setCarteiras] = useState<CarteiraResumo[]>([]);
  const [carteiraId, setCarteiraId] = useState<number | null>(null);
  const [valor, setValor] = useState("");
  const [ranking, setRanking] = useState<RankingAportes | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [calculando, setCalculando] = useState(false);
  const [registrando, setRegistrando] = useState(false);
  const [versaoHistorico, setVersaoHistorico] = useState(0);
  /** Margem operacional do motor (relativa à meta): 3% a 5%. */
  const [margem, setMargem] = useState<number | null>(null);

  useEffect(() => {
    api.get("/aportes/config")
      .then(r => setMargem(Number(r.data.margem_percentual)))
      .catch(() => setMargem(null));
  }, []);

  /** Salva a margem e recalcula: ela muda o LIMITE de todos os ativos. */
  const salvarMargem = async (novo: number) => {
    setMargem(novo);
    try {
      await api.put("/aportes/config", { margem_percentual: novo });
      toast.success(`Margem operacional: ${String(novo).replace(".", ",")}% (limite do ativo = meta + ${String(novo).replace(".", ",")}%)`);
      if (carteiraId != null) carregar(carteiraId, valor);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao salvar a margem operacional");
    }
  };

  useEffect(() => {
    api.get("/carteiras-investimento/all?page=0&size=100")
      .then(r => {
        const lista: CarteiraResumo[] = r.data.content ?? [];
        setCarteiras(lista);
        if (lista.length > 0) setCarteiraId(lista[0].id);
        else setCarregando(false);
      })
      .catch(() => { setCarregando(false); toast.error("Erro ao carregar carteiras"); });
  }, []);

  const carregar = useCallback(async (id: number, valorTexto: string) => {
    setCalculando(true);
    try {
      const params = new URLSearchParams({ carteira_investimento_id: String(id) });
      if (valorTexto.trim() !== "" && textoParaNum(valorTexto) > 0) {
        params.set("valor", String(textoParaNum(valorTexto)));
      }
      const { data } = await api.get<RankingAportes>(`/aportes/ranking?${params.toString()}`);
      setRanking(data);
    } catch {
      setRanking(null);
      toast.error("Erro ao calcular a prioridade de aporte");
    } finally {
      setCalculando(false);
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    if (carteiraId != null) carregar(carteiraId, valor);
  }, [carteiraId, carregar]);

  const top = ranking?.itens.find(i => (i.sugestao_aporte ?? 0) > 0) ?? ranking?.itens[0];
  const moeda = ranking?.moeda ?? "BRL";

  /**
   * Registra no histórico o aporte que o usuário EXECUTOU, a partir da sugestão
   * calculada. O motor passa a enxergar a concentração recente (§24).
   */
  const registrarAporte = async () => {
    if (carteiraId == null || !ranking) return;
    const itens = ranking.itens
      .filter(i => (i.sugestao_aporte ?? 0) > 0)
      .map(i => ({
        ativo_cadastro_id: i.ativo_cadastro_id,
        ativo_id: null,
        ticker: i.ticker,
        classe: i.classe,
        valor: i.sugestao_aporte as number,
      }));
    if (itens.length === 0) {
      toast.error("Não há sugestão para registrar — calcule um valor de aporte primeiro.");
      return;
    }
    setRegistrando(true);
    try {
      const { data } = await api.post("/aportes/registrar", {
        carteira_investimento_id: carteiraId,
        observacao: "Registrado a partir da sugestão do sistema",
        itens,
      });
      toast.success(`Aporte registrado! ${data.registrados} lançamento(s).`);
      setVersaoHistorico(v => v + 1);
      carregar(carteiraId, valor);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao registrar o aporte");
    } finally {
      setRegistrando(false);
    }
  };

  if (carregando) return <Layout><Spinner text="Carregando..." /></Layout>;

  if (carteiras.length === 0) {
    return (
      <Layout>
        <PageHeader icon="💸" title="Próximos Aportes" subtitle="Priorize onde investir o próximo dinheiro" />
        <EmptyState icon="💰" title="Nenhuma carteira de investimento"
          text="A prioridade de aporte é calculada por carteira. Crie uma carteira em Finanças e defina a Carteira Ideal."
          actionLabel="Ir para Finanças" onAction={() => navigate("/financas")} />
      </Layout>
    );
  }

  return (
    <Layout>
      <PlanejamentoNav ativo="aportes" />
      <PageHeader icon="💸" title="Próximos Aportes"
        subtitle="Onde falta, quem pode receber e quanto cabe até o limite"
        actionLabel="Dar as notas" onAction={() => navigate("/avaliacao/notas")} />

      {/* ── Controles ── */}
      <div style={{ ...boxStyle, display: "flex", gap: "12px", alignItems: "flex-end", flexWrap: "wrap", marginBottom: "16px" }}>
        <div>
          <label style={miniLabel}>Carteira</label>
          <select value={carteiraId ?? ""} aria-label="Carteira de investimento"
            onChange={e => setCarteiraId(Number(e.target.value))}
            style={{ ...controlStyle, minWidth: "200px" }}>
            {carteiras.map(c => <option key={c.id} value={c.id}>{c.nome} ({c.moeda})</option>)}
          </select>
        </div>
        <div>
          <label style={miniLabel}>Quanto você pretende aportar</label>
          <div style={{ display: "flex", gap: "8px", alignItems: "center", flexWrap: "wrap" }}>
            <input value={valor} inputMode="decimal" placeholder="ex: 1500"
              aria-label="Valor do aporte" onChange={e => setValor(apenasNumero(e.target.value))}
              onKeyDown={e => { if (e.key === "Enter" && carteiraId != null) carregar(carteiraId, valor); }}
              style={{ ...controlStyle, width: "150px", textAlign: "right" }} />
            <Button onClick={() => carteiraId != null && carregar(carteiraId, valor)} loading={calculando}>
              Calcular
            </Button>
            {valor !== "" && (
              <Button variant="ghost" size="sm" onClick={() => {
                setValor("");
                if (carteiraId != null) carregar(carteiraId, "");
              }}>Limpar</Button>
            )}
          </div>
        </div>
        {top && (top.sugestao_aporte ?? 0) > 0 && (
          <div style={{ marginLeft: "auto", textAlign: "right" }}>
            <p style={{ fontSize: "11px", fontWeight: 700, color: "#64748b", textTransform: "uppercase", margin: 0 }}>
              Comece por
            </p>
            <p style={{ fontSize: "15px", fontWeight: 800, color: "#047857", margin: "2px 0 0" }}>
              {top.ticker} · {fmtMoeda(top.sugestao_aporte, moeda)}
            </p>
          </div>
        )}
        <div style={{ marginLeft: top ? undefined : "auto" }}>
          <label style={miniLabel}>Margem operacional</label>
          <select value={margem ?? 5} aria-label="Margem operacional do motor de aporte"
            onChange={e => salvarMargem(Number(e.target.value))}
            title="O quanto um ativo pode passar da meta antes de o motor considerá-lo cheio (relativo à meta)"
            style={{ ...controlStyle, minWidth: "130px" }}>
            {[3, 4, 5].map(m => (
              <option key={m} value={m}>{m}% sobre a meta</option>
            ))}
          </select>
          <p style={{ fontSize: "10.5px", color: "#94a3b8", margin: "4px 0 0", maxWidth: "220px" }}>
            Meta de 5% com margem de {margem ?? 5}% → limite de
            {" "}{(((margem ?? 5) / 100) + 1) * 5}%. Estar na meta não bloqueia o aporte.
          </p>
        </div>
      </div>

      {calculando ? (
        <Spinner text="Calculando prioridade..." />
      ) : ranking ? (
        <>
          <DonutAporte ranking={ranking} />
          <div style={{ marginTop: "16px" }}>
            <RankingAportesTable ranking={ranking} />
          </div>
          {ranking.itens.some(i => (i.sugestao_aporte ?? 0) > 0) && (
            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "10px" }}>
              <Button onClick={registrarAporte} loading={registrando}>
                ✅ Registrar aporte executado
              </Button>
            </div>
          )}
        </>
      ) : null}

      <div style={{ marginTop: "16px" }}>
        <HistoricoAportesSection carteiraId={carteiraId as number} recarregar={versaoHistorico} />
      </div>

      <div style={{ marginTop: "16px" }}>
        <HistoricoCarteiraSection carteiraId={carteiraId as number} />
      </div>

      <p style={{ fontSize: "11px", color: "#94a3b8", marginTop: "14px" }}>
        O sistema não recomenda ativos: ele ordena e rateia o aporte conforme os pesos, metas e notas que você definiu.
      </p>
    </Layout>
  );
}
