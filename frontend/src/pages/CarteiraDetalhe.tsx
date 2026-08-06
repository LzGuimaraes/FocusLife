import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select, NumberInput, DateInput } from "../components/Form";
import { CardGrid, EmptyState, Spinner } from "../components/UI";
import { InvestInfo, totalInvestido, saldoAtual, lucro, rentabilidade, fmtPct, fmtSigned } from "../components/InvestInfo";
import { formatLocalDate, parseLocalDate } from "../utils/date";
import ContaLogsModal from "../components/ContaLogsModal";

/* ── Types ── */
type CatInvest = "RENDA_FIXA" | "TESOURO_DIRETO" | "ACOES" | "FIIS" | "ETFS" | "CRIPTOMOEDAS";
type TipoCarteira = "INVESTIMENTO" | "DESPESAS";

interface Ativo { id: number; nome: string; categoria: "CONTA" | "INVESTIMENTO"; categoriaInvestimento: CatInvest | null; quantidade: number | null; valorUnitario: number | null; precoAtual: number | null; saldo: number; instituicao: string | null; dataAplicacao: string | null; vencimento: string | null; dataVencimento: string | null; rentabilidade: number | null; pago: boolean | null; carteira_investimento_id: number | null; carteira_dividas_id: number | null; }
interface Financa { id: number; nome: string; moeda: string; tipo: TipoCarteira; }
interface FormData { nome: string; categoria: "CONTA" | "INVESTIMENTO"; categoriaInvestimento: CatInvest | ""; quantidade: string; valorUnitario: string; precoAtual: string; saldo: string; instituicao: string; dataAplicacao: string; vencimento: string; dataVencimento: string; rentabilidade: string; carteira_id: string; pago: boolean; }

const moedaS: Record<string, string> = { BRL: "R$", USD: "$", EUR: "€", GBP: "£", JPY: "¥" };
const fmt = (v: number | null | undefined, m: string) => v != null ? `${moedaS[m] || m} ${v.toFixed(2)}` : "-";
const pct = (parte: number, total: number) => total > 0 ? ((parte / total) * 100).toFixed(1) : "0.0";

const catInfo: Record<CatInvest, { icon: string; label: string; color: string; bg: string; autoCalc: boolean }> = {
  RENDA_FIXA: { icon: "📊", label: "Renda Fixa", color: "#3b82f6", bg: "#dbeafe", autoCalc: false },
  TESOURO_DIRETO: { icon: "🏛️", label: "Tesouro Direto", color: "#10b981", bg: "#d1fae5", autoCalc: false },
  ACOES: { icon: "📈", label: "Ações", color: "#6366f1", bg: "#eef2ff", autoCalc: true },
  FIIS: { icon: "🏢", label: "FIIs", color: "#8b5cf6", bg: "#ede9fe", autoCalc: true },
  ETFS: { icon: "📦", label: "ETFs", color: "#06b6d4", bg: "#ecfeff", autoCalc: true },
  CRIPTOMOEDAS: { icon: "₿", label: "Criptomoedas", color: "#f59e0b", bg: "#fef3c7", autoCalc: true },
};

const tipoLabel: Record<TipoCarteira, { icon: string; label: string; color: string; bg: string }> = {
  INVESTIMENTO: { icon: "📈", label: "Investimentos", color: "#8b5cf6", bg: "#f5f3ff" },
  DESPESAS: { icon: "📋", label: "Despesas", color: "#ef4444", bg: "#fef2f2" },
};

const emptyForm: FormData = { nome: "", categoria: "INVESTIMENTO", categoriaInvestimento: "", quantidade: "", valorUnitario: "", precoAtual: "", saldo: "0", instituicao: "", dataAplicacao: "", vencimento: "", dataVencimento: "", rentabilidade: "", carteira_id: "", pago: false };
type Errors = Partial<Record<keyof FormData, string>>;

export default function CarteiraDetalhe() {
  const { tipo, id } = useParams<{ tipo: string; id: string }>();
  const navigate = useNavigate();
  const carteiraId = parseInt(id || "0");
  // O tipo vem na rota: os IDs das duas tabelas podem colidir (ambas começam em 1)
  const carteiraTipo: TipoCarteira = (tipo || "").toLowerCase() === "investimento" ? "INVESTIMENTO" : "DESPESAS";
  const rotaTipo = carteiraTipo === "INVESTIMENTO" ? "investimento" : "dividas";

  const [ativos, setAtivos] = useState<Ativo[]>([]);
  const [carteira, setCarteira] = useState<Financa | null>(null);
  const [todasCarteiras, setTodasCarteiras] = useState<Financa[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Ativo | null>(null);
  const [form, setForm] = useState<FormData>(emptyForm);
  const [errors, setErrors] = useState<Errors>({});
  const [viewMode, setViewMode] = useState<"cards" | "list">("cards");
  const [logsConta, setLogsConta] = useState<Ativo | null>(null);

  useEffect(() => {
    const base = carteiraTipo === "INVESTIMENTO" ? "/carteiras-investimento" : "/carteiras-dividas";
    Promise.all([
      api.get(`${base}/all/${carteiraId}`),
      api.get(`${base}/all?page=0&size=100`),
    ]).then(([det, list]) => {
      const found: Financa = { id: det.data.id, nome: det.data.nome, moeda: det.data.moeda, tipo: carteiraTipo };
      setCarteira(found);
      const todas: Financa[] = (list.data.content ?? []).map((c: any) => ({ id: c.id, nome: c.nome, moeda: c.moeda, tipo: carteiraTipo }));
      setTodasCarteiras(todas);
    }).catch(() => navigate("/financas"));
  }, [carteiraId, carteiraTipo]);

  useEffect(() => { if (carteiraId) fetchAtivos(); }, [carteiraId]);

  const fetchAtivos = async () => {
    setLoading(true);
    try {
      const r = carteiraTipo === "INVESTIMENTO"
        ? await api.get(`/contas/by-carteira-investimento/${carteiraId}`)
        : await api.get(`/contas/by-carteira-dividas/${carteiraId}`);
      setAtivos(r.data);
    }
    catch { toast.error("Erro ao carregar"); }
    finally { setLoading(false); }
  };

  const walletType = carteira?.tipo;
  const isInvest = walletType === "INVESTIMENTO";
  const isDespesa = walletType === "DESPESAS";
  const itemLabel = isInvest ? "Investimento" : "Despesa";
  const moeda = carteira?.moeda || "BRL";

  /* ── Validação ── */
  const validate = (): boolean => {
    const e: Errors = {};
    if (!form.nome.trim()) e.nome = "O nome é obrigatório.";
    if (!form.dataVencimento) e.dataVencimento = "Informe a data de vencimento.";
    if (isInvest && !form.categoriaInvestimento) e.categoriaInvestimento = "Selecione a categoria.";
    const ci = catInfo[form.categoriaInvestimento as CatInvest];
    if (ci?.autoCalc) { const vu = parseFloat(form.valorUnitario); const q = parseFloat(form.quantidade); if (!form.valorUnitario || isNaN(vu) || vu <= 0) e.valorUnitario = "Informe o preço médio."; if (!form.quantidade || isNaN(q) || q <= 0) e.quantidade = "Informe a quantidade."; }
    else if (isInvest) { const s = parseFloat(form.saldo); if (form.saldo === "" || isNaN(s) || s < 0) e.saldo = "Valor inválido."; }
    if (isDespesa) { const s = parseFloat(form.saldo); if (form.saldo === "" || isNaN(s) || s < 0) e.saldo = "Valor inválido."; }
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    const payload: any = { nome: form.nome, categoria: isInvest ? "INVESTIMENTO" : "CONTA", dataVencimento: formatLocalDate(parseLocalDate(form.dataVencimento)) };
    if (isInvest) payload.carteira_investimento_id = carteiraId;
    else payload.carteira_dividas_id = carteiraId;
    if (isInvest) { payload.categoriaInvestimento = form.categoriaInvestimento; payload.instituicao = form.instituicao || null; payload.dataAplicacao = form.dataAplicacao || null; payload.vencimento = form.vencimento || null; payload.rentabilidade = form.rentabilidade ? parseFloat(form.rentabilidade) : null; payload.precoAtual = form.precoAtual ? parseFloat(form.precoAtual) : null; const ci = catInfo[form.categoriaInvestimento as CatInvest]; if (ci?.autoCalc) { payload.valorUnitario = parseFloat(form.valorUnitario) || 0; payload.quantidade = parseFloat(form.quantidade) || 0; } else { payload.saldo = parseFloat(form.saldo) || 0; } payload.pago = null; /* não se aplica a investimentos */ }
    if (isDespesa) {
      payload.saldo = parseFloat(form.saldo) || 0;
      payload.pago = form.pago;
      // Campos exclusivos de INVESTIMENTO: sempre null numa Despesa.
      payload.categoriaInvestimento = null;
      payload.quantidade = null;
      payload.valorUnitario = null;
      payload.precoAtual = null;
      payload.instituicao = null;
      payload.dataAplicacao = null;
      payload.vencimento = null;
      payload.rentabilidade = null;
    }
    const promise = editing ? api.put(`/contas/alter/${editing.id}`, payload) : api.post("/contas/create", payload);
    toast.promise(promise, { loading: "Salvando...", success: () => { closeModal(); fetchAtivos(); return editing ? "Atualizado!" : "Criado!"; }, error: (err: any) => err?.response?.data?.message || "Erro" });
  };

  const handleDelete = async (id: number) => { toast("Excluir?", { action: { label: "Sim", onClick: () => { toast.promise(api.delete(`/contas/delete/${id}`), { loading: "Excluindo...", success: () => { fetchAtivos(); return "Excluído!"; }, error: "Erro" }); }}, cancel: { label: "Cancelar", onClick: () => {} } }); };

  const togglePago = async (ativo: Ativo) => { const novo = !ativo.pago; try { await api.put(`/contas/alter/${ativo.id}`, { ...ativo, pago: novo }); setAtivos(prev => prev.map(a => a.id === ativo.id ? { ...a, pago: novo } : a)); toast.success(novo ? "Pago!" : "Desmarcado"); } catch { toast.error("Erro"); } };

  const openModal = (a: Ativo | null = null) => { setErrors({}); if (a) { setEditing(a); setForm({ nome: a.nome, categoria: a.categoria, categoriaInvestimento: a.categoriaInvestimento || "", quantidade: a.quantidade?.toString() || "", valorUnitario: a.valorUnitario?.toString() || "", precoAtual: a.precoAtual?.toString() || "", saldo: a.saldo?.toString() || "0", instituicao: a.instituicao || "", dataAplicacao: a.dataAplicacao || "", vencimento: a.vencimento || "", dataVencimento: a.dataVencimento || "", rentabilidade: a.rentabilidade?.toString() || "", carteira_id: carteiraId.toString(), pago: a.pago ?? false }); } else { setEditing(null); setForm({ ...emptyForm, categoria: isInvest ? "INVESTIMENTO" : "CONTA", carteira_id: carteiraId.toString() }); } setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); };

  /* ── Sumários ── */
  const contas = ativos.filter(a => a.categoria === "CONTA");
  const investimentos = ativos.filter(a => a.categoria === "INVESTIMENTO");
  const totalInvestidoCarteira = investimentos.reduce((s, a) => s + totalInvestido(a), 0);
  const valorAtualCarteira = investimentos.reduce((s, a) => s + saldoAtual(a), 0);
  const lucroTotal = valorAtualCarteira - totalInvestidoCarteira;
  const rentTotal = totalInvestidoCarteira > 0 ? ((valorAtualCarteira - totalInvestidoCarteira) / totalInvestidoCarteira) * 100 : 0;
  const pagas = contas.filter(a => a.pago).reduce((s, a) => s + (a.saldo || 0), 0);
  const pendentes = contas.filter(a => !a.pago).reduce((s, a) => s + (a.saldo || 0), 0);
  const totalDespesas = pagas + pendentes;

  const distCategorias = (Object.keys(catInfo) as CatInvest[]).map(ci => { const items = investimentos.filter(a => a.categoriaInvestimento === ci); return { key: ci, ...catInfo[ci], total: items.reduce((s, a) => s + saldoAtual(a), 0), count: items.length }; }).filter(d => d.count > 0);

  const summaryCards = isInvest
    ? [
        { icon: "💰", label: "Total Investido", value: fmt(totalInvestidoCarteira, moeda), color: "#6366f1", bg: "#eef2ff" },
        { icon: "📈", label: "Valor Atual da Carteira", value: fmt(valorAtualCarteira, moeda), color: "#10b981", bg: "#ecfdf5" },
        { icon: lucroTotal >= 0 ? "📈" : "📉", label: "Lucro/Prejuízo Total", value: fmtSigned(lucroTotal, moeda), color: lucroTotal >= 0 ? "#10b981" : "#ef4444", bg: lucroTotal >= 0 ? "#ecfdf5" : "#fef2f2" },
        { icon: "🎯", label: "Rentabilidade Total", value: fmtPct(rentTotal), color: rentTotal > 0 ? "#10b981" : rentTotal < 0 ? "#ef4444" : "#64748b", bg: rentTotal > 0 ? "#ecfdf5" : rentTotal < 0 ? "#fef2f2" : "#f1f5f9" },
      ]
    : [{ icon: "📋", label: "Total Despesas", value: fmt(totalDespesas, moeda), color: "#ef4444", bg: "#fef2f2" }, { icon: "✅", label: "Pagas", value: fmt(pagas, moeda), color: "#10b981", bg: "#d1fae5" }, { icon: "⏳", label: "Pendentes", value: fmt(pendentes, moeda), color: "#f59e0b", bg: "#fef3c7" }];

  if (!carteira && !loading) return <Layout><Spinner text="Redirecionando..." /></Layout>;

  return (
    <Layout>
      {/* ── Header da Carteira ── */}
      <div style={{ marginBottom: "24px" }} className="animate-fade-in">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "12px", marginBottom: "16px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
            <button onClick={() => navigate("/financas")} style={{ background: "#f1f5f9", border: "none", borderRadius: "8px", padding: "8px 12px", cursor: "pointer", fontSize: "16px", color: "#64748b", display: "flex", alignItems: "center" }}>←</button>
            <div>
              <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                <h1 style={{ fontSize: "clamp(20px, 3vw, 26px)", fontWeight: 800, color: "#0f172a", margin: 0 }}>{carteira?.nome || "Carregando..."}</h1>
                {walletType && <span style={{ fontSize: "12px", fontWeight: 700, padding: "4px 12px", borderRadius: "var(--radius-full)", background: tipoLabel[walletType].bg, color: tipoLabel[walletType].color }}>{tipoLabel[walletType].icon} {tipoLabel[walletType].label}</span>}
              </div>
              <p style={{ fontSize: "13px", color: "#64748b", margin: "4px 0 0" }}>{moedaS[moeda] || moeda} {moeda} · {ativos.length} {isInvest ? "investimentos" : "despesas"}</p>
            </div>
          </div>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
            {/* Seletor de carteira */}
            <select value={carteiraId} onChange={e => navigate(`/financas/carteiras/${rotaTipo}/${e.target.value}`)}
              style={{ padding: "8px 32px 8px 12px", fontSize: "13px", border: "1.5px solid #e2e8f0", borderRadius: "8px", background: "white", color: "#0f172a", cursor: "pointer", appearance: "none", backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%236b7280' d='M6 8L1.5 3h9z'/%3E%3C/svg%3E")`, backgroundRepeat: "no-repeat", backgroundPosition: "right 10px center", fontWeight: 500 }}>
              {todasCarteiras.map(f => <option key={f.id} value={f.id}>{f.tipo === "INVESTIMENTO" ? "📈" : "📋"} {f.nome}</option>)}
            </select>
            {/* Alternância de visualização: Cards ↔ Lista */}
            <div style={{ display: "flex", gap: "4px", background: "#eef2f7", padding: "3px", borderRadius: "10px", alignSelf: "center" }}>
              <button onClick={() => setViewMode("cards")} style={viewBtn(viewMode === "cards")} aria-label="Visualizar em cards">▦ Cards</button>
              <button onClick={() => setViewMode("list")} style={viewBtn(viewMode === "list")} aria-label="Visualizar em lista">☰ Lista</button>
            </div>
            <button onClick={() => openModal()}
              style={{ padding: "10px 18px", background: isInvest ? "#8b5cf6" : "#ef4444", color: "white", border: "none", borderRadius: "8px", cursor: "pointer", fontSize: "13px", fontWeight: 600, transition: "all 0.15s ease", whiteSpace: "nowrap" }}
              onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-1px)"; e.currentTarget.style.boxShadow = "0 4px 12px rgba(0,0,0,0.15)"; }}
              onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "none"; }}>
              + {isInvest ? "Novo Investimento" : "Nova Despesa"}
            </button>
          </div>
        </div>

        {/* Sumário */}
        {!loading && ativos.length > 0 && (
          <div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 180px), 1fr))", gap: "clamp(8px, 1.5vw, 12px)", marginBottom: isInvest && distCategorias.length > 0 ? "16px" : "0" }}>
              {summaryCards.map((sc, i) => (
                <div key={i} style={{ background: "white", borderRadius: "12px", padding: "clamp(12px, 2vw, 16px)", boxShadow: "0 1px 3px rgba(0,0,0,0.06)", display: "flex", flexDirection: "column", gap: "4px", borderLeft: `4px solid ${sc.color}` }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "6px" }}><span style={{ fontSize: "16px" }}>{sc.icon}</span><span style={{ fontSize: "11px", fontWeight: 600, color: "#64748b" }}>{sc.label}</span></div>
                  <span style={{ fontSize: "clamp(15px, 2.2vw, 18px)", fontWeight: 800, color: sc.color }}>{sc.value}</span>
                </div>
              ))}
            </div>

            {/* Distribuição % */}
            {isInvest && distCategorias.length > 0 && (
              <div style={{ background: "white", borderRadius: "12px", padding: "clamp(14px, 2vw, 18px)", boxShadow: "0 1px 3px rgba(0,0,0,0.06)" }}>
                <h3 style={{ fontSize: "14px", fontWeight: 700, color: "#0f172a", marginBottom: "12px" }}>📊 Distribuição do Patrimônio</h3>
                <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                  {distCategorias.map(d => (
                    <div key={d.key} style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                      <span style={{ fontSize: "13px", fontWeight: 600, color: d.color, minWidth: "110px" }}>{d.icon} {d.label}</span>
                      <div style={{ flex: 1, height: "10px", background: "#f1f5f9", borderRadius: "5px", overflow: "hidden" }}><div style={{ height: "100%", width: `${Math.min(100, parseFloat(pct(d.total, valorAtualCarteira)))}%`, background: d.color, borderRadius: "5px", transition: "width 0.5s ease" }} /></div>
                      <span style={{ fontSize: "12px", fontWeight: 700, color: "#0f172a", minWidth: "50px", textAlign: "right" }}>{pct(d.total, valorAtualCarteira)}%</span>
                      <span style={{ fontSize: "12px", color: "#64748b", minWidth: "80px", textAlign: "right" }}>{fmt(d.total, moeda)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* ── Lista de Ativos ── */}
      {loading ? <Spinner text={`Carregando ${isInvest ? "investimentos" : "despesas"}...`} /> :
        ativos.length === 0 ? <EmptyState icon={isInvest ? "📈" : "📋"} title={`Nenhum ${itemLabel.toLowerCase()}`} text={`Crie seu primeiro ${itemLabel.toLowerCase()} nesta carteira!`} actionLabel={`Criar ${itemLabel}`} onAction={() => openModal()} /> :
        viewMode === "cards" ? (
        <CardGrid>{ativos.map(a => {
          const ci = catInfo[a.categoriaInvestimento as CatInvest];
          const isConta = a.categoria === "CONTA";
          const isInv = a.categoria === "INVESTIMENTO";
          return (
            <div key={a.id} style={{ ...cardStyle, borderLeftColor: isConta ? (a.pago ? "#10b981" : "#f59e0b") : (ci?.color || "#8b5cf6") }}
              onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-3px)"; e.currentTarget.style.boxShadow = "0 10px 25px rgba(0,0,0,0.1)"; }}
              onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 1px 2px rgba(0,0,0,0.05)"; }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "8px", gap: "6px", flexWrap: "wrap" }}>
                {isInv && ci && <span style={{ fontSize: "11px", fontWeight: 600, padding: "4px 10px", borderRadius: "var(--radius-full)", background: ci.bg, color: ci.color }}>{ci.icon} {ci.label}</span>}
                {isConta && <span style={{ fontSize: "12px", fontWeight: 600, padding: "4px 10px", borderRadius: "var(--radius-full)", background: "#dbeafe", color: "#1d4ed8" }}>💳 Conta</span>}
                {isConta && <span style={{ fontSize: "11px", fontWeight: 700, padding: "4px 10px", borderRadius: "var(--radius-full)", background: a.pago ? "#d1fae5" : "#fef3c7", color: a.pago ? "#047857" : "#b45309" }}>{a.pago ? "✅ Pago" : "⏳ Pendente"}</span>}
              </div>
              <h3 style={{ fontSize: "16px", fontWeight: 600, marginBottom: "6px" }}>{a.nome}</h3>
              <InvestInfo ativo={a} moeda={moeda} />
              <p style={{ fontSize: "20px", fontWeight: 800, color: isConta ? (a.pago ? "#10b981" : "#f59e0b") : "#10b981", marginBottom: "14px" }}>{fmt(saldoAtual(a), moeda)}</p>
              <div style={{ display: "flex", gap: "8px", marginTop: "auto", flexWrap: "wrap" }}>
                {isConta && <button onClick={() => togglePago(a)} style={{ ...btnSm, background: a.pago ? "#fef3c7" : "#d1fae5", color: a.pago ? "#b45309" : "#047857", fontWeight: 700 }}>{a.pago ? "↩ Desmarcar" : "✓ Pagar"}</button>}
                <button onClick={() => openModal(a)} style={btnSm}>✏️ Editar</button>
                <button onClick={() => handleDelete(a.id)} style={{ ...btnSm, background: "#fee2e2", color: "#ef4444" }}>🗑</button>
              </div>
            </div>
          );
        })}
        </CardGrid>
        ) : (
          <div className="animate-fade-in" style={{ background: "white", borderRadius: "12px", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", overflow: "hidden" }}>
            <div style={{ overflowX: "auto" }}>
              <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px" }}>
                <thead>
                  <tr style={{ background: "#f8fafc" }}>
                    <th style={thStyle}>Ativo</th>
                    <th style={thStyle}>Detalhes</th>
                    <th style={thStyle}>Vencimento</th>
                    <th style={{ ...thStyle, textAlign: "right" }}>Valor</th>
                    <th style={thStyle}>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {ativos.map(a => {
                    const ci = catInfo[a.categoriaInvestimento as CatInvest];
                    const isConta = a.categoria === "CONTA";
                    const isInv = a.categoria === "INVESTIMENTO";
                    const temQtd = isInv && a.quantidade != null && a.valorUnitario != null;
                    const vencida = a.dataVencimento != null && a.dataVencimento < formatLocalDate(new Date());
                    return (
                      <tr key={a.id} style={{ borderTop: "1px solid #f1f5f9", background: vencida ? "#fef2f2" : undefined }}>
                        <td style={tdStyle}>
                          <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                            <span style={{ fontWeight: 600, color: "#0f172a" }}>{a.nome}</span>
                            {isInv && ci && <span style={{ fontSize: "11px", fontWeight: 600, padding: "3px 9px", borderRadius: "var(--radius-full)", background: ci.bg, color: ci.color }}>{ci.icon} {ci.label}</span>}
                            {isConta && <span style={{ fontSize: "11px", fontWeight: 600, padding: "3px 9px", borderRadius: "var(--radius-full)", background: "#dbeafe", color: "#1d4ed8" }}>💳 Conta</span>}
                            {isConta && <span style={{ fontSize: "11px", fontWeight: 700, padding: "3px 9px", borderRadius: "var(--radius-full)", background: a.pago ? "#d1fae5" : "#fef3c7", color: a.pago ? "#047857" : "#b45309" }}>{a.pago ? "✅ Pago" : "⏳ Pendente"}</span>}
                          </div>
                        </td>
                        <td style={tdStyle}>
                          <div style={{ display: "flex", flexDirection: "column", gap: "2px" }}>
                            {temQtd && <span style={{ fontSize: "12px", color: "#64748b" }}>📦 {a.quantidade} un · 💵 {fmt(a.valorUnitario, moeda)}/un</span>}
                            {isInv && a.precoAtual != null && (
                              <span style={{ fontSize: "12px", fontWeight: 600, color: a.precoAtual >= (a.valorUnitario || 0) ? "#10b981" : "#ef4444" }}>📊 Atual: {fmt(a.precoAtual, moeda)}</span>
                            )}
                            {isInv && temQtd && <span style={{ fontSize: "12px", color: "#475569" }}>💰 Total Investido: <b>{fmt(totalInvestido(a), moeda)}</b></span>}
                            {isInv && temQtd && <span style={{ fontSize: "12px", color: "#475569" }}>📈 Saldo Atual: <b style={{ color: "#10b981" }}>{fmt(saldoAtual(a), moeda)}</b></span>}
                            {isInv && temQtd && <span style={{ fontSize: "12px", color: "#475569" }}>📊 Lucro/Prejuízo: <b style={{ color: lucro(a) > 0 ? "#10b981" : lucro(a) < 0 ? "#ef4444" : "#64748b" }}>{fmtSigned(lucro(a), moeda)}</b></span>}
                            {isInv && temQtd && (() => { const r = rentabilidade(a); const c = r > 0 ? "#10b981" : r < 0 ? "#ef4444" : "#64748b"; const arrow = r > 0 ? "▲" : r < 0 ? "▼" : "•"; return <span style={{ fontSize: "12px", fontWeight: 700, color: c }}>🎯 Rentabilidade: {arrow} {fmtPct(r)}</span>; })()}
                            {isInv && !temQtd && a.precoAtual == null && a.instituicao && <span style={{ fontSize: "12px", color: "#64748b" }}>🏦 {a.instituicao}</span>}
                            {isInv && !temQtd && a.precoAtual == null && a.dataAplicacao && <span style={{ fontSize: "12px", color: "#64748b" }}>🗓 {a.dataAplicacao}</span>}
                          </div>
                        </td>                        <td style={tdStyle}>
                          <span style={{ fontSize: "13px", fontWeight: 600, color: vencida ? "#ef4444" : "#475569", whiteSpace: "nowrap" }}>
                            {a.dataVencimento ? parseLocalDate(a.dataVencimento).toLocaleDateString('pt-BR') : "-"}
                          </span>
                        </td>                        <td style={{ ...tdStyle, textAlign: "right" }}>
                          <span style={{ fontSize: "15px", fontWeight: 800, color: isConta ? (a.pago ? "#10b981" : "#f59e0b") : "#10b981", whiteSpace: "nowrap" }}>{fmt(saldoAtual(a), moeda)}</span>
                        </td>
                        <td style={tdStyle}>
                          <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                            {isConta && <button onClick={() => togglePago(a)} style={{ ...btnSm, background: a.pago ? "#fef3c7" : "#d1fae5", color: a.pago ? "#b45309" : "#047857", fontWeight: 700 }}>{a.pago ? "↩ Desmarcar" : "✓ Pagar"}</button>}
                            <button onClick={() => openModal(a)} style={btnSm}>✏️ Editar</button>
                            <button onClick={() => setLogsConta(a)} style={btnSm} title="Ver logs">📜</button>
                            <button onClick={() => handleDelete(a.id)} style={{ ...btnSm, background: "#fee2e2", color: "#ef4444" }}>🗑</button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )
      }

      {/* ── Modal ── */}
      <Modal open={showModal} onClose={closeModal} title={editing ? `Editar ${itemLabel}` : `Novo ${itemLabel}`} onSubmit={handleSubmit} submitLabel="Salvar" width="540px">
        <Input label="Nome / Ticker / Ativo" required value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} placeholder={isInvest ? "Ex: PETR4, BTC, Tesouro Selic..." : "Ex: Aluguel, Internet..."} error={errors.nome} />
        <DateInput label="Data de Vencimento" value={form.dataVencimento} onChange={e => setForm({ ...form, dataVencimento: e.target.value })} error={errors.dataVencimento} required />

        {isDespesa && (
          <div style={{ display: "flex", alignItems: "center", gap: "10px", padding: "10px 14px", background: form.pago ? "#d1fae5" : "#fef3c7", borderRadius: "10px", border: `1.5px solid ${form.pago ? "#10b981" : "#f59e0b"}` }}>
            <input type="checkbox" id="pago-check" checked={form.pago} onChange={e => setForm({ ...form, pago: e.target.checked })} style={{ width: "18px", height: "18px", accentColor: "#10b981", cursor: "pointer" }} />
            <label htmlFor="pago-check" style={{ fontSize: "14px", fontWeight: 600, color: form.pago ? "#047857" : "#b45309", cursor: "pointer" }}>{form.pago ? "✅ Pago" : "⏳ Pendente"} (opcional)</label>
          </div>
        )}

        {isInvest && (
          <Select label="Categoria de Investimento" required value={form.categoriaInvestimento} onChange={e => { setForm({ ...form, categoriaInvestimento: e.target.value as CatInvest | "", quantidade: "", valorUnitario: "", precoAtual: "", saldo: "0", instituicao: "", dataAplicacao: "", vencimento: "", rentabilidade: "" }); }} error={errors.categoriaInvestimento}>
            <option value="">Selecione...</option>
            {(Object.keys(catInfo) as CatInvest[]).map(ci => <option key={ci} value={ci}>{catInfo[ci].icon} {catInfo[ci].label}</option>)}
          </Select>
        )}

        {form.categoriaInvestimento && isInvest && (() => {
          const ci = catInfo[form.categoriaInvestimento as CatInvest];
          if (!ci) return null;
          return (<>
            {form.categoriaInvestimento === "RENDA_FIXA" && (<><Input label="Instituição Financeira (opcional)" value={form.instituicao} onChange={e => setForm({ ...form, instituicao: e.target.value })} placeholder="Ex: Banco do Brasil" /><div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}><DateInput label="Data da Aplicação (opcional)" value={form.dataAplicacao} onChange={e => setForm({ ...form, dataAplicacao: e.target.value })} /><NumberInput label="Rentabilidade % (opcional)" decimal value={form.rentabilidade} onChange={v => setForm({ ...form, rentabilidade: v })} placeholder="Ex: 12.5" /></div><DateInput label="Vencimento (opcional)" value={form.vencimento} onChange={e => setForm({ ...form, vencimento: e.target.value })} /><NumberInput label="Valor Aplicado" required decimal value={form.saldo} onChange={v => setForm({ ...form, saldo: v })} error={errors.saldo} placeholder="1000.00" /></>)}
            {form.categoriaInvestimento === "TESOURO_DIRETO" && (<><div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}><DateInput label="Data da Compra (opcional)" value={form.dataAplicacao} onChange={e => setForm({ ...form, dataAplicacao: e.target.value })} /><DateInput label="Vencimento (opcional)" value={form.vencimento} onChange={e => setForm({ ...form, vencimento: e.target.value })} /></div><NumberInput label="Quantidade de Títulos (opcional)" value={form.quantidade} onChange={v => setForm({ ...form, quantidade: v })} placeholder="Ex: 5" /><NumberInput label="Valor Investido" required decimal value={form.saldo} onChange={v => setForm({ ...form, saldo: v })} error={errors.saldo} placeholder="1000.00" /></>)}
            {(form.categoriaInvestimento === "ACOES" || form.categoriaInvestimento === "FIIS" || form.categoriaInvestimento === "ETFS") && (<><Input label={form.categoriaInvestimento === "ACOES" ? "Nome da Empresa (opcional)" : "Nome do Fundo (opcional)"} value={form.instituicao} onChange={e => setForm({ ...form, instituicao: e.target.value })} /><div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}><NumberInput label="Preço Médio" required decimal value={form.valorUnitario} onChange={v => setForm({ ...form, valorUnitario: v })} error={errors.valorUnitario} placeholder="Ex: 35.50" /><NumberInput label="Quantidade" required value={form.quantidade} onChange={v => setForm({ ...form, quantidade: v })} error={errors.quantidade} placeholder="Ex: 100" /></div><NumberInput label="Preço Atual (opcional)" decimal value={form.precoAtual} onChange={v => setForm({ ...form, precoAtual: v })} placeholder="Ex: 38.20" />{form.valorUnitario && form.quantidade && (<div style={{ padding: "10px 14px", background: "#ecfdf5", borderRadius: "10px", border: "1.5px solid #10b981", display: "flex", justifyContent: "space-between", alignItems: "center" }}><span style={{ fontSize: "13px", fontWeight: 600, color: "#047857" }}>💰 Valor da Posição</span><span style={{ fontSize: "18px", fontWeight: 800, color: "#10b981" }}>{fmt(parseFloat(form.valorUnitario) * parseFloat(form.quantidade || "0"), moeda)}</span></div>)}</>)}
            {form.categoriaInvestimento === "CRIPTOMOEDAS" && (<><div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}><NumberInput label="Preço Médio" required decimal value={form.valorUnitario} onChange={v => setForm({ ...form, valorUnitario: v })} error={errors.valorUnitario} placeholder="Ex: 350000" /><NumberInput label="Quantidade" required highPrecision value={form.quantidade} onChange={v => setForm({ ...form, quantidade: v })} error={errors.quantidade} placeholder="Ex: 0.05" /></div><NumberInput label="Preço Atual (opcional)" decimal value={form.precoAtual} onChange={v => setForm({ ...form, precoAtual: v })} placeholder="Ex: 365000" />{form.valorUnitario && form.quantidade && (<div style={{ padding: "10px 14px", background: "#ecfdf5", borderRadius: "10px", border: "1.5px solid #10b981", display: "flex", justifyContent: "space-between", alignItems: "center" }}><span style={{ fontSize: "13px", fontWeight: 600, color: "#047857" }}>💰 Valor da Posição</span><span style={{ fontSize: "18px", fontWeight: 800, color: "#10b981" }}>{fmt(parseFloat(form.valorUnitario) * parseFloat(form.quantidade || "0"), moeda)}</span></div>)}</>)}
          </>);
        })()}

        {isDespesa && (<div style={{ display: "grid", gridTemplateColumns: "1fr", gap: "14px" }}><NumberInput label="Valor" required decimal value={form.saldo} onChange={v => setForm({ ...form, saldo: v })} error={errors.saldo} placeholder="0.00" hint="Valor da conta/despesa neste vencimento" /></div>)}
      </Modal>
      {logsConta && (
        <ContaLogsModal open contaId={logsConta.id} contaNome={logsConta.nome} onClose={() => setLogsConta(null)} />
      )}
    </Layout>
  );
}

const cardStyle: React.CSSProperties = { background: "white", borderRadius: "12px", padding: "20px", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", transition: "all 0.25s ease", display: "flex", flexDirection: "column", borderLeft: "4px solid #6366f1" };
const btnSm: React.CSSProperties = { padding: "7px 14px", borderRadius: "8px", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "#f1f5f9", color: "#64748b" };
const viewBtn = (active: boolean): React.CSSProperties => ({ padding: "7px 14px", borderRadius: "8px", border: "none", cursor: "pointer", fontSize: "13px", fontWeight: 600, background: active ? "white" : "transparent", color: active ? "#6366f1" : "#64748b", boxShadow: active ? "0 1px 3px rgba(0,0,0,0.12)" : "none", transition: "all 0.15s ease" });
const thStyle: React.CSSProperties = { padding: "12px 16px", fontSize: "11px", fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.04em", whiteSpace: "nowrap" };
const tdStyle: React.CSSProperties = { padding: "12px 16px", borderTop: "1px solid #f1f5f9", verticalAlign: "middle" };