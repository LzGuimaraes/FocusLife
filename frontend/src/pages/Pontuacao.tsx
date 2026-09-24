import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import { PageHeader, EmptyState, Spinner } from "../components/UI";
import { Button, Badge } from "../components/Shared";
import { NumberInput, Select } from "../components/Form";
import RankingAportesTable from "../components/RankingAportesTable";
import PlanejamentoNav from "../components/PlanejamentoNav";
import type { CarteiraResumo } from "../types/planejamento";
import type { RankingAportes, ScoreConfig, ScoreConfigPayload, TetoAtivoModo, TracaMotor } from "../types/aporte";
import { ESTRATEGIAS_APORTE, TRACAS_MOTOR, TRACA_LABEL } from "../types/aporte";
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
  MOMENTO: string;
  PRECO: string;
}

export default function Pontuacao() {
  const navigate = useNavigate();
  const [config, setConfig] = useState<ScoreConfig | null>(null);
  const [pesos, setPesos] = useState<PesosForm>({ QUALITY: "3", DEFICIT: "4", EXCESSO: "2", PRIORIDADE: "1", MOMENTO: "3", PRECO: "2" });
  const [estrategia, setEstrategia] = useState<ScoreConfig["estrategia_aporte"]>("DEFICIT_PROPORCIONAL");
  const [faixas, setFaixas] = useState({ f1: "25", f2: "50", f3: "75", f4: "90" });
  const [redistribuir, setRedistribuir] = useState(true);
  const [rebalancear, setRebalancear] = useState(false);
  const [tetoModo, setTetoModo] = useState<TetoAtivoModo>("TETO_ESTRITO");
  const [precedencia, setPrecedencia] = useState<TracaMotor[]>([...TRACAS_MOTOR]);
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
      MOMENTO: String(cfg.peso_momento),
      PRECO: String(cfg.peso_preco),
    });
    setFaixas({
      f1: String(cfg.momento_faixa_1),
      f2: String(cfg.momento_faixa_2),
      f3: String(cfg.momento_faixa_3),
      f4: String(cfg.momento_faixa_4),
    });
    setRedistribuir(cfg.redistribuir);
    setRebalancear(cfg.rebalancear);
    setTetoModo(cfg.teto_ativo_modo);
    setPrecedencia(ordemDaConfig(cfg.precedencia));
    setEstrategia(cfg.estrategia_aporte);
  };

  /**
   * Converte a ordem salva ("BLOQUEIO,LIMITE,...") numa lista de travas.
   * IDs desconhecidos são ignorados e o que faltar entra no fim — a ordem na
   * tela nunca pode virar uma lista vazia ou com trava desligada.
   */
  const ordemDaConfig = (bruta: string | null): TracaMotor[] => {
    const validas = (bruta ?? "").split(",")
      .map(id => id.trim().toUpperCase())
      .filter((id): id is TracaMotor => (TRACAS_MOTOR as readonly string[]).includes(id));
    const semRepetir = validas.filter((id, i) => validas.indexOf(id) === i);
    return [...semRepetir, ...TRACAS_MOTOR.filter(id => !semRepetir.includes(id))];
  };

  /** Move uma trava para cima/baixo na ordem de precedência (§36). */
  const moverTraca = (indice: number, delta: number) => {
    const destino = indice + delta;
    if (destino < 0 || destino >= precedencia.length) return;
    const copia = [...precedencia];
    [copia[indice], copia[destino]] = [copia[destino], copia[indice]];
    setPrecedencia(copia);
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
      peso_momento: textoParaNum(pesos.MOMENTO),
      peso_preco: textoParaNum(pesos.PRECO),
      momento_faixa_1: textoParaNum(faixas.f1),
      momento_faixa_2: textoParaNum(faixas.f2),
      momento_faixa_3: textoParaNum(faixas.f3),
      momento_faixa_4: textoParaNum(faixas.f4),
      redistribuir,
      rebalancear,
      teto_ativo_modo: tetoModo,
      precedencia: precedencia.join(","),
      estrategia_aporte: estrategia,
    };
    if (payload.peso_quality + payload.peso_deficit + payload.peso_excesso
        + payload.peso_prioridade + payload.peso_momento + payload.peso_preco <= 0) {
      toast.error("Informe ao menos um peso maior que zero.");
      return;
    }
    if (!(payload.momento_faixa_1 < payload.momento_faixa_2
          && payload.momento_faixa_2 < payload.momento_faixa_3
          && payload.momento_faixa_3 < payload.momento_faixa_4)) {
      toast.error("As faixas da nota de momento devem ser crescentes (ex.: 25, 50, 75, 90).");
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
      <PlanejamentoNav ativo="pontuacao" />
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
            <p style={{ fontSize: "11px", fontWeight: 700, color: "#64748b", margin: "0 0 2px" }}>Priority Score</p>
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

          {/* ── Fator de momento: nota 0–100 → fator 0 a 1 (não altera a qualidade) ── */}
          <div style={{ marginTop: "16px", borderTop: "1px solid #f1f5f9", paddingTop: "14px" }}>
            <h4 style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a", margin: "0 0 4px" }}>
              Fator de momento (checklists de Momento)
            </h4>
            <p style={{ fontSize: "11px", color: "#64748b", margin: "0 0 10px" }}>
              A nota de momento vira um fator de <strong>0 a 1</strong> aplicado só à prioridade de aporte.
              Fator 0 = não aportar; 1 = prioridade cheia. A qualidade do ativo não muda.
              Sem checklist de momento, o fator fica neutro (1).
            </p>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 130px), 1fr))", gap: "10px" }}>
              <NumberInput label="Até (fator 0)" value={faixas.f1}
                onChange={v => setFaixas({ ...faixas, f1: apenasNumero(v) })} placeholder="25" />
              <NumberInput label="Até (fator 0,25)" value={faixas.f2}
                onChange={v => setFaixas({ ...faixas, f2: apenasNumero(v) })} placeholder="50" />
              <NumberInput label="Até (fator 0,50)" value={faixas.f3}
                onChange={v => setFaixas({ ...faixas, f3: apenasNumero(v) })} placeholder="75" />
              <NumberInput label="Até (fator 0,75)" value={faixas.f4}
                onChange={v => setFaixas({ ...faixas, f4: apenasNumero(v) })} placeholder="90" />
            </div>
            <p style={{ fontSize: "11px", color: "#94a3b8", margin: "6px 0 0" }}>
              Acima de {faixas.f4 || "90"} → fator 1,00.
            </p>
          </div>

          <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "13px", fontWeight: 600, color: "#374151", marginTop: "14px" }}>
            <input type="checkbox" checked={redistribuir} onChange={e => setRedistribuir(e.target.checked)}
              style={{ width: "18px", height: "18px", accentColor: "#6366f1" }} />
            Redistribuir o valor que não achou destino
          </label>
          <p style={{ fontSize: "11px", color: "#64748b", margin: "4px 0 0" }}>
            Ligado: o que não coube numa classe (todos os ativos no alvo/bloqueados) procura outra classe com déficit.
            Desligado: fica <strong>não alocado</strong>, sempre com o motivo explicado.
          </p>

          {/* ── Modo do teto do ativo (§17) ── */}
          <div style={{ marginTop: "16px", borderTop: "1px solid #f1f5f9", paddingTop: "14px" }}>
            <Select label="Teto do ativo" value={tetoModo}
              hint={tetoModo === "TETO_ESTRITO"
                ? "O ativo só recebe até o próprio alvo (déficit + tolerância dele)."
                : "O ativo também absorve o déficit da classe/subclasse dele (o orçamento da classe continua sendo o limite)."}
              onChange={e => setTetoModo(e.target.value as TetoAtivoModo)}>
              <option value="TETO_ESTRITO">Estrito — só o déficit do próprio ativo</option>
              <option value="TETO_ATE_A_CLASSE">Até a classe — pode absorver o déficit da classe/subclasse</option>
            </Select>
          </div>

          {/* ── Rebalanceamento (§19/§37) ── */}
          <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "13px", fontWeight: 600, color: "#374151", marginTop: "16px" }}>
            <input type="checkbox" checked={rebalancear} onChange={e => setRebalancear(e.target.checked)}
              style={{ width: "18px", height: "18px", accentColor: "#6366f1" }} />
            Rebalanceamento (sugerir vendas do que passou do alvo)
          </label>
          <p style={{ fontSize: "11px", color: "#64748b", margin: "4px 0 0" }}>
            Ligado: o motor mostra o quanto está <strong>acima do alvo + tolerância</strong> em cada nível e usa essa
            venda sugerida como orçamento extra para os déficits — nunca vende nada sozinho, é só sugestão.
            Desligado: o plano usa apenas o valor do aporte.
          </p>

          {/* ── Ordem das travas (§36) ── */}
          <div style={{ marginTop: "16px", borderTop: "1px solid #f1f5f9", paddingTop: "14px" }}>
            <h4 style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a", margin: "0 0 4px" }}>
              Ordem das travas do motor
            </h4>
            <p style={{ fontSize: "11px", color: "#64748b", margin: "0 0 10px" }}>
              As travas são <strong>sempre</strong> aplicadas — a ordem só define como o motivo é explicado na tela.
              Nenhuma regra é escondida nem desligada por esta ordem.
            </p>
            {precedencia.map((id, i) => (
              <div key={id} style={{
                display: "flex", alignItems: "center", gap: "8px", padding: "6px 8px",
                background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "8px", marginBottom: "6px",
              }}>
                <span style={{ fontSize: "11px", fontWeight: 700, color: "#94a3b8", minWidth: "16px" }}>{i + 1}.</span>
                <span style={{ fontSize: "12px", color: "#334155", flex: 1 }}>{TRACA_LABEL[id]}</span>
                <button type="button" title="Subir" aria-label={`Subir ${TRACA_LABEL[id]}`}
                  onClick={() => moverTraca(i, -1)} disabled={i === 0}
                  style={{ border: "1px solid #e2e8f0", background: "#fff", borderRadius: "6px", width: "24px", height: "24px", cursor: i === 0 ? "not-allowed" : "pointer", color: i === 0 ? "#cbd5e1" : "#475569" }}>↑</button>
                <button type="button" title="Descer" aria-label={`Descer ${TRACA_LABEL[id]}`}
                  onClick={() => moverTraca(i, 1)} disabled={i === precedencia.length - 1}
                  style={{ border: "1px solid #e2e8f0", background: "#fff", borderRadius: "6px", width: "24px", height: "24px", cursor: i === precedencia.length - 1 ? "not-allowed" : "pointer", color: i === precedencia.length - 1 ? "#cbd5e1" : "#475569" }}>↓</button>
              </div>
            ))}
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
            Quality Score × Priority Score
          </h3>
          <p style={{ fontSize: "13px", color: "#475569", lineHeight: 1.6, margin: "0 0 12px" }}>
            São duas coisas diferentes, e o sistema mantém as duas separadas:
          </p>
          <ul style={{ fontSize: "13px", color: "#475569", lineHeight: 1.7, margin: 0, paddingLeft: "18px" }}>
            <li><strong>Quality Score</strong> — qualidade do ativo: soma ponderada das notas que <em>você</em> deu nos checklists.</li>
            <li><strong>Fórmula de prioridade</strong> — prioridade <em>entre os ativos elegíveis</em>: qualidade, déficit, excesso, momento, oportunidade de preço e sua prioridade manual, com os pesos acima.</li>
            <li><strong>Elegibilidade</strong> — vem ANTES da fórmula: bloqueio, limite de concentração, preço máximo de compra e momento zero <em>descartam</em> o ativo, por melhor que seja a nota.</li>
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
