import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import { PageHeader, EmptyState, Spinner } from "../components/UI";
import { Button } from "../components/Shared";
import CarteiraIdealEditor, { type ClasseDraft } from "../components/CarteiraIdealEditor";
import MetasEditor, { type MetaDraft } from "../components/MetasEditor";
import ComparativoTable from "../components/ComparativoTable";
import { boxStyle, controlStyle, miniLabel } from "../components/FormStyles";
import { novaChave } from "../utils/chaves";
import { numParaTexto, textoParaNum } from "../utils/numeros";
import type {
  CarteiraIdeal, CarteiraIdealPayload, CarteiraResumo, Comparativo, Estrategia,
} from "../types/planejamento";
import { somaFechada } from "../utils/percentual";

/* ══════════════════════════════════════════════════════════════════════
   Tela da Carteira Ideal (Módulos 1, 7 e 10 - parte do comparativo).

   Duas abas:
     • Configuração → classes, subclasses e metas (rascunho local + Salvar)
     • Comparativo  → Carteira Atual × Ideal por classe/subclasse/ativo

   O salvamento é REPLACE-ALL no backend: o payload enviado passa a ser a
   configuração inteira. A validação de 100% é feita no servidor (fonte da
   verdade) e duplicada aqui apenas para dar feedback imediato.
   ══════════════════════════════════════════════════════════════════════ */

type Aba = "config" | "comparativo";

export default function CarteiraIdealPage() {
  const { carteiraId } = useParams<{ carteiraId: string }>();
  const navigate = useNavigate();

  const [carteiras, setCarteiras] = useState<CarteiraResumo[]>([]);
  const [estrategias, setEstrategias] = useState<Estrategia[]>([]);
  const [selecionada, setSelecionada] = useState<number | null>(carteiraId ? Number(carteiraId) : null);
  const [aba, setAba] = useState<Aba>("config");
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);

  const [classes, setClasses] = useState<ClasseDraft[]>([]);
  const [metas, setMetas] = useState<MetaDraft[]>([]);
  const [estrategiaId, setEstrategiaId] = useState<string>("");

  const [comparativo, setComparativo] = useState<Comparativo | null>(null);
  const [avisos, setAvisos] = useState<string[]>([]);

  /* ── Carrega a lista de carteiras + estratégias uma vez ── */
  useEffect(() => {
    api.get("/carteiras-investimento/all?page=0&size=100")
      .then(r => setCarteiras(r.data.content ?? []))
      .catch(() => toast.error("Erro ao carregar carteiras"))
      .finally(() => setCarregando(false));

    api.get("/estrategias/all?page=0&size=100")
      .then(r => setEstrategias(r.data.content ?? []))
      .catch(() => setEstrategias([]));
  }, []);

  /* ── Carrega a configuração + comparativo da carteira selecionada ── */
  const carregar = useCallback(async (id: number) => {
    setCarregando(true);
    try {
      const [idealRes, compRes] = await Promise.all([
        api.get<CarteiraIdeal>(`/carteiras-investimento/${id}/ideal`),
        api.get<Comparativo>(`/carteiras-investimento/${id}/ideal/comparativo`),
      ]);
      const ideal = idealRes.data;
      setClasses(ideal.classes.map(c => ({
        key: novaChave(),
        classe: c.classe,
        percentual_ideal: numParaTexto(c.percentual_ideal),
        subclasses: (c.subclasses ?? []).map(s => ({
          key: novaChave(),
          nome: s.nome,
          percentual_ideal: numParaTexto(s.percentual_ideal),
        })),
      })));
      setMetas(ideal.metas.map(m => ({
        key: novaChave(),
        ativo_cadastro_id: m.ativo_cadastro_id ?? "",
        ticker: m.ticker ?? "",
        classe: m.classe,
        subclasse_nome: m.subclasse_nome ?? "",
        percentual_ideal: numParaTexto(m.percentual_ideal),
        prioridade_manual: String(m.prioridade_manual ?? 0),
      })));
      setEstrategiaId(ideal.estrategia_id != null ? String(ideal.estrategia_id) : "");
      setAvisos(ideal.avisos ?? []);
      setComparativo(compRes.data);
    } catch {
      toast.error("Erro ao carregar a Carteira Ideal");
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    if (selecionada != null) carregar(selecionada);
  }, [selecionada, carregar]);

  /* ── Validação local (o backend valida de novo) ── */
  const validar = (): string | null => {
    if (classes.length === 0) return "Adicione ao menos uma classe à Carteira Ideal.";
    for (const c of classes) {
      if (c.percentual_ideal.trim() === "") return "Informe o percentual ideal de todas as classes.";
      for (const s of c.subclasses) {
        if (!s.nome.trim()) return "Toda subclasse precisa de um nome.";
        if (s.percentual_ideal.trim() === "") return `Informe o percentual da subclasse "${s.nome}".`;
      }
    }
    const soma = classes.reduce((s, c) => s + textoParaNum(c.percentual_ideal), 0);
    if (!somaFechada(soma)) return "A soma das classes deve ser 100%.";
    for (const m of metas) {
      if (!m.ativo_cadastro_id) return "Selecione o ativo (ticker) de todas as metas.";
      if (m.percentual_ideal.trim() === "") return `Informe o percentual ideal da meta de ${m.ticker}.`;
      const prioridade = parseInt(m.prioridade_manual, 10);
      if (Number.isNaN(prioridade) || prioridade < 0 || prioridade > 10) {
        return `A prioridade de ${m.ticker} deve ficar entre 0 e 10.`;
      }
    }
    return null;
  };

  const handleSalvar = async () => {
    if (selecionada == null) return;
    const problema = validar();
    if (problema) {
      toast.error(problema);
      setAba("config");
      return;
    }

    const payload: CarteiraIdealPayload = {
      estrategia_id: estrategiaId ? Number(estrategiaId) : null,
      classes: classes.map((c, i) => ({
        classe: c.classe,
        percentual_ideal: textoParaNum(c.percentual_ideal),
        ordem: i,
        subclasses: c.subclasses.map((s, j) => ({
          nome: s.nome.trim(),
          percentual_ideal: textoParaNum(s.percentual_ideal),
          ordem: j,
        })),
      })),
      metas: metas.map((m, i) => ({
        ativo_cadastro_id: m.ativo_cadastro_id,
        classe: m.classe,
        subclasse_nome: m.subclasse_nome || null,
        percentual_ideal: textoParaNum(m.percentual_ideal),
        prioridade_manual: parseInt(m.prioridade_manual, 10) || 0,
        ordem: i,
      })),
    };

    setSalvando(true);
    try {
      const { data } = await api.put<CarteiraIdeal>(`/carteiras-investimento/${selecionada}/ideal`, payload);
      setAvisos(data.avisos ?? []);
      const comp = await api.get<Comparativo>(`/carteiras-investimento/${selecionada}/ideal/comparativo`);
      setComparativo(comp.data);
      toast.success("Carteira Ideal salva!");
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao salvar a Carteira Ideal");
    } finally {
      setSalvando(false);
    }
  };

  /* ── Estados de tela ── */
  if (carregando && carteiras.length === 0) {
    return <Layout><Spinner text="Carregando..." /></Layout>;
  }

  if (carteiras.length === 0) {
    return (
      <Layout>
        <PageHeader icon="🎯" title="Carteira Ideal" subtitle="Defina a distribuição alvo da sua carteira" />
        <EmptyState
          icon="💼"
          title="Nenhuma carteira de investimento"
          text="A Carteira Ideal é definida por carteira de investimento. Crie uma carteira em Finanças para começar."
          actionLabel="Ir para Finanças"
          onAction={() => navigate("/financas")}
        />
      </Layout>
    );
  }

  const soma = classes.reduce((s, c) => s + textoParaNum(c.percentual_ideal), 0);
  const podeSalvar = !salvando && (classes.length === 0 || somaFechada(soma));
  const carteiraAtual = carteiras.find(c => c.id === selecionada);

  return (
    <Layout>
      <PageHeader
        icon="🎯"
        title="Carteira Ideal"
        subtitle="Classes, subclasses e metas de ativos — a metodologia é sua, o sistema só organiza e calcula"
      />

      {/* ── Toolbar ── */}
      <div style={{ ...boxStyle, display: "flex", gap: "10px", alignItems: "flex-end", flexWrap: "wrap", marginBottom: "16px" }}>
        <div>
          <label style={miniLabel}>Carteira</label>
          <select value={selecionada ?? ""} aria-label="Carteira de investimento"
            onChange={e => {
              const id = Number(e.target.value);
              setSelecionada(id);
              navigate(`/planejamento/carteira-ideal/${id}`, { replace: true });
            }}
            style={{ ...controlStyle, minWidth: "200px" }}>
            {carteiras.map(c => <option key={c.id} value={c.id}>{c.nome} ({c.moeda})</option>)}
          </select>
        </div>

        <div>
          <label style={miniLabel}>Estratégia (opcional)</label>
          <select value={estrategiaId} aria-label="Estratégia de investimentos"
            onChange={e => setEstrategiaId(e.target.value)}
            style={{ ...controlStyle, minWidth: "180px" }}>
            <option value="">— Nenhuma —</option>
            {estrategias.map(e => <option key={e.id} value={e.id}>{e.nome}</option>)}
          </select>
        </div>

        <div style={{ display: "flex", gap: "4px", background: "#eef2f7", padding: "3px", borderRadius: "10px" }}>
          <button type="button" onClick={() => setAba("config")} style={abaBtn(aba === "config")}>⚙️ Configuração</button>
          <button type="button" onClick={() => setAba("comparativo")} style={abaBtn(aba === "comparativo")}>📊 Comparativo</button>
        </div>

        <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: "10px" }}>
          {classes.length > 0 && !somaFechada(soma) && (
            <span style={{ fontSize: "12px", color: "#b45309", fontWeight: 600 }}>
              ⚠ A soma das classes está em {soma.toFixed(2).replace(".", ",")}%
            </span>
          )}
          <Button onClick={handleSalvar} loading={salvando} disabled={!podeSalvar}>Salvar</Button>
        </div>
      </div>

      {avisos.length > 0 && aba === "config" && (
        <div style={{ background: "#fffbeb", border: "1px solid #fde68a", borderRadius: "10px", padding: "10px 14px", marginBottom: "16px" }}>
          {avisos.map((a, i) => (
            <p key={i} style={{ fontSize: "12px", color: "#92400e", margin: i === 0 ? 0 : "4px 0 0" }}>⚠ {a}</p>
          ))}
        </div>
      )}

      {carregando ? <Spinner text="Carregando carteira..." /> : aba === "config" ? (
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <CarteiraIdealEditor classes={classes} onChange={setClasses} />
          <MetasEditor metas={metas} classes={classes} onChange={setMetas} />
        </div>
      ) : comparativo ? (
        <ComparativoTable comparativo={comparativo} />
      ) : (
        <EmptyState icon="📊" title="Sem comparativo" text="Salve a Carteira Ideal para ver a comparação com a carteira atual." />
      )}

      {carteiraAtual && (
        <p style={{ fontSize: "11px", color: "#94a3b8", marginTop: "14px" }}>
          Os valores atuais vêm das posições cadastradas em Finanças → {carteiraAtual.nome}.
          Posições sem vínculo com o catálogo (ex.: renda fixa) entram na classe, mas não em metas por ticker.
        </p>
      )}
    </Layout>
  );
}

const abaBtn = (ativo: boolean): React.CSSProperties => ({
  padding: "7px 14px", borderRadius: "8px", border: "none", cursor: "pointer",
  fontSize: "12px", fontWeight: 600, whiteSpace: "nowrap",
  background: ativo ? "white" : "transparent",
  color: ativo ? "#0f172a" : "#64748b",
  boxShadow: ativo ? "0 1px 2px rgba(0,0,0,0.08)" : "none",
});
