import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import { PageHeader, EmptyState, Spinner } from "../components/UI";
import { Button, Badge } from "../components/Shared";
import { NumberInput, Select } from "../components/Form";
import RankingAportesTable from "../components/RankingAportesTable";
import type { CarteiraResumo } from "../types/planejamento";
import type { RankingAportes, ScoreConfig, ScoreConfigPayload } from "../types/aporte";
import { ESTRATEGIAS_APORTE } from "../types/aporte";
import { boxStyle, miniLabel, controlStyle } from "../components/FormStyles";
import { apenasNumero, textoParaNum } from "../utils/numeros";

/* ══════════════════════════════════════════════════════════════════════
   Pontuação (Módulos 6, 7 e 9 — Fase 3).

   Aqui o usuário define os PESOS da fórmula de prioridade de aporte e vê o
   efeito imediato no ranking dos seus ativos. A fórmula é fixa e mostrada na
   tela (α·Qualidade + β·Déficit − γ·Excesso + δ·Prioridade); o sistema não
   esconde o cálculo nem decide nada pelo usuário.
   ══════════════════════════════════════════════════════════════════════ */

interface PesosForm {
  QUALITY: string;
  DEFICIT: string;
  EXCESSO: string;
  PRIORIDADE: string;
}

export default function Pontuacao() {
  const navigate = useNavigate();
  const [config, setConfig] = useState<ScoreConfig | null>(null);
  const [pesos, setPesos] = useState<PesosForm>({ QUALITY: "3", DEFICIT: "4", EXCESSO: "2", PRIORIDADE: "1" });
  const [estrategia, setEstrategia] = useState<ScoreConfig["estrategia_aporte"]>("DEFICIT_PROPORCIONAL");
  const [carteiras, setCarteiras] = useState<CarteiraResumo[]>([]);
  const [carteiraId, setCarteiraId] = useState<number | null>(null);
  const [valorAporte, setValorAporte] = useState("");
  const [ranking, setRanking] = useState<RankingAportes | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [carregandoRanking, setCarregandoRanking] = useState(false);
  const [salvando, setSalvando] = useState(false);

  /* ── Configuração + carteiras (uma vez) ── */
  useEffect(() => {
    Promise.all([
      api.get<ScoreConfig>("/score-config"),
      api.get("/carteiras-investimento/all?page=0&size=100"),
    ]).then(([cfgRes, cartRes]) => {
      aplicarConfig(cfgRes.data);
      const lista: CarteiraResumo[] = cartRes.data.content ?? [];
      setCarteiras(lista);
      if (lista.length > 0) setCarteiraId(lista[0].id);
    }).catch(() => toast.error("Erro ao carregar a configuração"))
      .finally(() => setCarregando(false));
  }, []);

  const aplicarConfig = (cfg: ScoreConfig) => {
    setConfig(cfg);
    setPesos({
      QUALITY: String(cfg.peso_quality),
      DEFICIT: String(cfg.peso_deficit),
      EXCESSO: String(cfg.peso_excesso),
      PRIORIDADE: String(cfg.peso_prioridade),
    });
    setEstrategia(cfg.estrategia_aporte);
  };

  /* ── Ranking (carteira, valor e pesos salvos) ── */
  const carregarRanking = useCallback(async (id: number, valor: string) => {
    setCarregandoRanking(true);
    try {
      const params = new URLSearchParams({ carteira_investimento_id: String(id) });
      if (valor.trim() !== "" && textoParaNum(valor) > 0) {
        params.set("valor", String(textoParaNum(valor)));
      }
      const { data } = await api.get<RankingAportes>(`/aportes/ranking?${params.toString()}`);
      setRanking(data);
    } catch {
      setRanking(null);
      toast.error("Erro ao carregar a prioridade de aporte");
    } finally {
      setCarregandoRanking(false);
    }
  }, []);

  useEffect(() => {
    if (carteiraId != null) carregarRanking(carteiraId, valorAporte);
  }, [carteiraId, carregarRanking]);

  const salvar = async () => {
    const payload: ScoreConfigPayload = {
      peso_quality: textoParaNum(pesos.QUALITY),
      peso_deficit: textoParaNum(pesos.DEFICIT),
      peso_excesso: textoParaNum(pesos.EXCESSO),
      peso_prioridade: textoParaNum(pesos.PRIORIDADE),
      estrategia_aporte: estrategia,
    };
    if (payload.peso_quality + payload.peso_deficit + payload.peso_excesso + payload.peso_prioridade <= 0) {
      toast.error("Informe ao menos um peso maior que zero.");
      return;
    }
    setSalvando(true);
    try {
      const { data } = await api.put<ScoreConfig>("/score-config", payload);
      aplicarConfig(data);
      toast.success("Pesos salvos!");
      if (carteiraId != null) carregarRanking(carteiraId, valorAporte);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao salvar os pesos");
    } finally {
      setSalvando(false);
    }
  };

  const restaurar = () => {
    toast("Voltar aos pesos padrão?", {
      description: "Os pesos personalizados serão descartados.",
      action: {
        label: "Sim, restaurar",
        onClick: () => toast.promise(api.delete<ScoreConfig>("/score-config"), {
          loading: "Restaurando...",
          success: ({ data }) => {
            aplicarConfig(data);
            if (carteiraId != null) carregarRanking(carteiraId, valorAporte);
            return "Pesos padrão restaurados!";
          },
          error: "Erro ao restaurar",
        }),
      },
      cancel: { label: "Cancelar", onClick: () => {} },
    });
  };

  const formula = config?.termos
    .map(t => ({ ...t, valor: textoParaNum(pesos[t.termo]) }))
    .map(t => `${t.termo === "EXCESSO" ? "−" : "+"} ${t.valor}·${t.label.split(" ")[0]}`)
    .join(" ")
    .replace(/^\+ /, "");

  if (carregando) return <Layout><Spinner text="Carregando..." /></Layout>;

  return (
    <Layout>
      <PageHeader icon="⚙️" title="Pontuação"
        subtitle="Defina os pesos da sua prioridade de aporte — o sistema só aplica o que você configurar" />

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 420px), 1fr))", gap: "16px", marginBottom: "16px" }}>
        {/* ── Pesos ── */}
        <div style={boxStyle}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "8px", marginBottom: "12px" }}>
            <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>Pesos da fórmula</h3>
            {config && (
              <Badge color={config.personalizada ? "#6366f1" : "#64748b"} bg={config.personalizada ? "#eef2ff" : "#f1f5f9"}>
                {config.personalizada ? "Personalizada" : "Padrão do sistema"}
              </Badge>
            )}
          </div>

          <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "10px", padding: "10px 14px", marginBottom: "14px" }}>
            <p style={{ fontSize: "11px", fontWeight: 700, color: "#64748b", margin: "0 0 2px" }}>Contribution Score</p>
            <code style={{ fontSize: "12px", color: "#334155" }}>
              100 × ( {formula} ) ÷ soma dos pesos
            </code>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 190px), 1fr))", gap: "12px" }}>
            {config?.termos.map(t => (
              <NumberInput key={t.termo} label={t.label} hint={t.descricao}
                value={pesos[t.termo]}
                onChange={v => setPesos(prev => ({ ...prev, [t.termo]: apenasNumero(v) }))}
                placeholder={String(t.peso_padrao)} />
            ))}
          </div>

          <div style={{ marginTop: "14px" }}>
            <Select label="Como dividir o valor do aporte" value={estrategia}
              hint={ESTRATEGIAS_APORTE.find(e => e.value === estrategia)?.descricao}
              onChange={e => setEstrategia(e.target.value as ScoreConfig["estrategia_aporte"])}>
              {ESTRATEGIAS_APORTE.map(e => <option key={e.value} value={e.value}>{e.label}</option>)}
            </Select>
          </div>

          <div style={{ display: "flex", gap: "10px", marginTop: "16px", flexWrap: "wrap" }}>
            <Button onClick={salvar} loading={salvando}>Salvar pesos</Button>
            <Button variant="secondary" onClick={restaurar} disabled={!config?.personalizada}>
              Restaurar padrão
            </Button>
          </div>
        </div>

        {/* ── Como ler o resultado ── */}
        <div style={boxStyle}>
          <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: "0 0 10px" }}>
            Quality Score × Contribution Score
          </h3>
          <p style={{ fontSize: "13px", color: "#475569", lineHeight: 1.6, margin: "0 0 12px" }}>
            São duas coisas diferentes, e o sistema mantém as duas separadas:
          </p>
          <ul style={{ fontSize: "13px", color: "#475569", lineHeight: 1.7, margin: 0, paddingLeft: "18px" }}>
            <li><strong>Quality Score</strong> — qualidade do ativo: soma ponderada das notas que <em>você</em> deu nos checklists.</li>
            <li><strong>Contribution Score</strong> — prioridade de aporte: combina qualidade, distância da meta, excesso e sua prioridade manual, com os pesos acima.</li>
          </ul>
          <p style={{ fontSize: "12px", color: "#94a3b8", marginTop: "12px", marginBottom: 0 }}>
            O sistema não escolhe ativos nem atribui notas: ele organiza, calcula e ordena a sua metodologia.
          </p>
        </div>
      </div>

      {/* ── Prévia do ranking ── */}
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
          <label style={miniLabel}>Valor do aporte (opcional)</label>
          <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
            <input value={valorAporte} inputMode="decimal" placeholder="ex: 1000"
              aria-label="Valor do aporte"
              onChange={e => setValorAporte(apenasNumero(e.target.value))}
              style={{ ...controlStyle, width: "140px", textAlign: "right" }} />
            <Button variant="secondary" size="sm"
              onClick={() => carteiraId != null && carregarRanking(carteiraId, valorAporte)}>
              Simular
            </Button>
            {valorAporte !== "" && (
              <Button variant="ghost" size="sm" onClick={() => {
                setValorAporte("");
                if (carteiraId != null) carregarRanking(carteiraId, "");
              }}>Limpar</Button>
            )}
            <Button variant="ghost" size="sm" onClick={() => navigate("/planejamento/aportes")}>
              Abrir Próximos Aportes →
            </Button>
          </div>
        </div>
      </div>

      {carteiras.length === 0 ? (
        <EmptyState icon="💰" title="Nenhuma carteira de investimento"
          text="A prioridade de aporte é calculada por carteira. Crie uma carteira em Finanças e defina a Carteira Ideal."
          actionLabel="Ir para Finanças" onAction={() => navigate("/financas")} />
      ) : carregandoRanking ? (
        <Spinner text="Calculando prioridade..." />
      ) : ranking ? (
        <RankingAportesTable ranking={ranking} titulo="Prévia — prioridade de aporte" />
      ) : null}
    </Layout>
  );
}
