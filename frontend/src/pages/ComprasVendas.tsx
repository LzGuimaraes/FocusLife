import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { TrendingUp, TrendingDown, DollarSign, Percent, BarChart3, PieChart } from "lucide-react";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { DateInput, Input, NumberInput, Select } from "../components/Form";
import { Badge, Button } from "../components/Shared";
import { EmptyState, Spinner } from "../components/UI";
import GraficoComprasVendas from "../components/GraficoComprasVendas";
import ResumoPeriodoDonut from "../components/ResumoPeriodoDonut";
import PrecosAlvoSection from "../components/PrecosAlvoSection";
import SeletorAtivo, { type CarteiraOption, type SelecaoAtivo } from "../components/SeletorAtivo";
import { CATEGORIAS, catInfo } from "../utils/percentual";
import { textoParaNum, textoParaNumOuNull } from "../utils/numeros";
import type { CategoriaInvestimento } from "../types/planejamento";
import type {
  OperacaoCompraVenda,
  OperacaoFormPayload,
  ResumoComprasVendas,
} from "../types/comprasVendas";

/* ══════════════════════════════════════════════════════════════════════
   Compras/Vendas — lucro/prejuízo por operação.

   O usuário registra a COMPRA (e, quando vender, a VENDA) de um ativo. O
   backend devolve o lucro/prejuízo em R$, a rentabilidade %, o prazo em dias e
   o resumo/série do gráfico — o front NÃO recalcula dinheiro.
   ══════════════════════════════════════════════════════════════════════ */

const fmtDinheiro = (v: number | null | undefined) =>
  v == null ? "—" : `R$ ${v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const fmtPct = (v: number | null | undefined) =>
  v == null ? "—" : `${v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;
const fmtData = (iso: string | null) =>
  iso ? iso.split("-").reverse().join("/") : "—";

/** Número → texto pt-BR com 2 casas (para campos de valor/quantidade). */
const brl = (n: number) => n.toFixed(2).replace(".", ",");

interface FormState {
  data_compra: string;
  data_venda: string;
  quantidade: string;
  quantidade_vendida: string;
  valor_inicial: string;
  valor_venda: string;
  preco_unit_compra: string;
  preco_unit_venda: string;
}

const formVazio: FormState = {
  data_compra: "", data_venda: "", quantidade: "",
  quantidade_vendida: "", valor_inicial: "", valor_venda: "",
  preco_unit_compra: "", preco_unit_venda: "",
};

const CLASSE_OPCOES = CATEGORIAS.filter(c => c.key !== "OUTROS");

export default function ComprasVendas() {
  const navigate = useNavigate();

  const [carteiras, setCarteiras] = useState<CarteiraOption[]>([]);
  const [operacoes, setOperacoes] = useState<OperacaoCompraVenda[]>([]);
  const [resumo, setResumo] = useState<ResumoComprasVendas | null>(null);
  const [loading, setLoading] = useState(true);

  // Filtros (globais do usuário; carteira é opcional)
  const [filtroCarteira, setFiltroCarteira] = useState("");
  const [filtroDe, setFiltroDe] = useState("");
  const [filtroAte, setFiltroAte] = useState("");

  // Modal de operação
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<OperacaoCompraVenda | null>(null);
  const [form, setForm] = useState<FormState>(formVazio);
  const [selecao, setSelecao] = useState<SelecaoAtivo | null>(null);
  const [classe, setClasse] = useState<CategoriaInvestimento | null>(null);

  useEffect(() => {
    api.get("/carteiras-investimento/all?page=0&size=100")
      .then(r => {
        const lista: CarteiraOption[] = (r.data.content ?? []).map((c: any) => ({ id: c.id, nome: c.nome }));
        setCarteiras(lista);
      })
      .catch(() => setCarteiras([]));
  }, []);

  const paramsFiltro = useCallback((comPagina: boolean) => {
    const p = new URLSearchParams();
    if (filtroCarteira) p.set("carteira_investimento_id", filtroCarteira);
    if (filtroDe) p.set("de", filtroDe);
    if (filtroAte) p.set("ate", filtroAte);
    if (comPagina) { p.set("page", "0"); p.set("size", "100"); }
    return p.toString();
  }, [filtroCarteira, filtroDe, filtroAte]);

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const [ops, res] = await Promise.all([
        api.get(`/compras-vendas/all?${paramsFiltro(true)}`),
        api.get(`/compras-vendas/resumo?${paramsFiltro(false)}`),
      ]);
      setOperacoes(ops.data.content ?? []);
      setResumo(res.data);
    } catch {
      toast.error("Erro ao carregar as operações");
      setOperacoes([]);
      setResumo(null);
    } finally {
      setLoading(false);
    }
  }, [paramsFiltro]);

  useEffect(() => { carregar(); }, [carregar]);

  const limparFiltros = () => {
    setFiltroCarteira("");
    setFiltroDe("");
    setFiltroAte("");
  };

  /* ── Modal ── */
  const abrirNova = () => {
    setEditing(null);
    setForm(formVazio);
    setSelecao(null);
    setClasse(null);
    setShowModal(true);
  };

  const abrirEdicao = (op: OperacaoCompraVenda) => {
    setEditing(op);
    setSelecao(null);
    setClasse(op.classe);
    setForm({
      data_compra: op.data_compra ?? "",
      data_venda: op.data_venda ?? "",
      quantidade: op.quantidade != null ? String(op.quantidade) : "",
      quantidade_vendida: op.quantidade_vendida != null ? String(op.quantidade_vendida) : "",
      valor_inicial: op.valor_inicial != null ? String(op.valor_inicial) : "",
      valor_venda: op.valor_venda != null ? String(op.valor_venda) : "",
      preco_unit_compra: op.valor_unitario_compra != null ? brl(op.valor_unitario_compra) : "",
      preco_unit_venda: op.valor_unitario_venda != null ? brl(op.valor_unitario_venda) : "",
    });
    setShowModal(true);
  };

  const fechar = () => { setShowModal(false); setEditing(null); };

  /* Preço unitário ↔ total: um campo alimenta o outro pela QUANTIDADE.
     Assim o usuário digita o preço por unidade (o que vê na corretora) e o
     total sai na hora (e vice-versa) — e a tela mostra os dois. */
  const setQuantidade = (v: string) => {
    setForm(f => {
      const q = textoParaNumOuNull(v);
      const uc = textoParaNumOuNull(f.preco_unit_compra);
      const uv = textoParaNumOuNull(f.preco_unit_venda);
      return {
        ...f,
        quantidade: v,
        valor_inicial: (q != null && q > 0 && uc != null) ? brl(uc * q) : f.valor_inicial,
        valor_venda: (q != null && q > 0 && uv != null) ? brl(uv * q) : f.valor_venda,
      };
    });
  };

  const setUnitCompra = (v: string) => {
    setForm(f => {
      const q = textoParaNumOuNull(f.quantidade);
      const u = textoParaNumOuNull(v);
      return { ...f, preco_unit_compra: v,
        valor_inicial: (q != null && q > 0 && u != null) ? brl(u * q) : f.valor_inicial };
    });
  };

  const setUnitVenda = (v: string) => {
    setForm(f => {
      const q = textoParaNumOuNull(f.quantidade);
      const u = textoParaNumOuNull(v);
      return { ...f, preco_unit_venda: v,
        valor_venda: (q != null && q > 0 && u != null) ? brl(u * q) : f.valor_venda };
    });
  };

  const setTotalCompra = (v: string) => {
    setForm(f => {
      const q = textoParaNumOuNull(f.quantidade);
      const t = textoParaNumOuNull(v);
      return { ...f, valor_inicial: v,
        preco_unit_compra: (q != null && q > 0 && t != null) ? brl(t / q) : f.preco_unit_compra };
    });
  };

  const setTotalVenda = (v: string) => {
    setForm(f => {
      const q = textoParaNumOuNull(f.quantidade);
      const t = textoParaNumOuNull(v);
      return { ...f, valor_venda: v,
        preco_unit_venda: (q != null && q > 0 && t != null) ? brl(t / q) : f.preco_unit_venda };
    });
  };

  const handleSalvar = async () => {
    // A referência do ativo vem da nova seleção OU, na edição sem troca, da original.
    const referencia = selecao ?? (editing ? {
      ativo_cadastro_id: editing.ativo_cadastro_id,
      ativo_id: editing.ativo_id,
      ticker: editing.ticker,
      nome: editing.nome,
      classe: editing.classe,
      carteira_investimento_id: editing.carteira_investimento_id,
    } : null);

    // Validações com feedback (o botão nunca fica mudo).
    if (!referencia) { toast.error("Escolha o ativo (catálogo ou posição) da operação."); return; }
    if (!form.data_compra) { toast.error("Informe a data da compra."); return; }
    if (form.valor_inicial.trim() === "") { toast.error("Informe o valor inicial (custo total da compra)."); return; }

    const valorInicial = textoParaNum(form.valor_inicial);
    const valorVenda = textoParaNumOuNull(form.valor_venda);
    const quantidade = textoParaNumOuNull(form.quantidade);
    const quantidadeVendida = textoParaNumOuNull(form.quantidade_vendida);

    if (valorInicial < 0) { toast.error("O valor inicial não pode ser negativo."); return; }
    if (valorVenda != null && valorVenda < 0) { toast.error("O valor da venda não pode ser negativo."); return; }
    if (valorVenda != null && !form.data_venda) { toast.error("Informe a data da venda."); return; }
    if (valorVenda == null && form.data_venda) { toast.error("Informe o valor da venda (ou limpe a data)."); return; }
    if (form.data_venda && form.data_venda < form.data_compra) {
      toast.error("A data da venda não pode ser anterior à data da compra."); return;
    }
    if (quantidade != null && quantidadeVendida != null && quantidadeVendida > quantidade) {
      toast.error("A quantidade vendida não pode ser maior que a comprada."); return;
    }

    const payload: OperacaoFormPayload = {
      carteira_investimento_id: referencia.carteira_investimento_id ?? null,
      ativo_cadastro_id: referencia.ativo_cadastro_id ?? null,
      ativo_id: referencia.ativo_id ?? null,
      ticker: referencia.ticker,
      nome: referencia.nome,
      classe: classe ?? referencia.classe ?? null,
      data_compra: form.data_compra,
      data_venda: form.data_venda || null,
      quantidade,
      quantidade_vendida: quantidadeVendida,
      valor_inicial: valorInicial,
      valor_venda: valorVenda,
    };

    try {
      if (editing) {
        await api.put(`/compras-vendas/alter/${editing.id}`, payload);
        toast.success("Operação atualizada!");
      } else {
        await api.post("/compras-vendas/create", payload);
        toast.success("Operação registrada!");
      }
      fechar();
      await carregar();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao salvar a operação");
    }
  };

  const handleDelete = (op: OperacaoCompraVenda) => {
    toast("Excluir esta operação?", {
      action: {
        label: "Sim",
        onClick: () => {
          toast.promise(api.delete(`/compras-vendas/delete/${op.id}`), {
            loading: "Excluindo...",
            success: () => { carregar(); return "Operação excluída!"; },
            error: "Erro ao excluir",
          });
        },
      },
      cancel: { label: "Cancelar", onClick: () => {} },
    });
  };

  const carteiraNome = (id: number | null) =>
    id == null ? "—" : (carteiras.find(c => c.id === id)?.nome ?? `#${id}`);

  const lucroPositivo = (resumo?.lucro_total ?? 0) >= 0;
  const serieResumo = resumo?.serie ?? [];
  const ultimoPonto = serieResumo.length ? serieResumo[serieResumo.length - 1] : null;
  const maiorBruto = serieResumo.length ? Math.max(...serieResumo.map(s => s.valor_bruto)) : null;
  const percentuaisResumo = serieResumo
    .map(s => s.valor_percentual_acumulado)
    .filter((v): v is number => v != null);
  const melhorPct = percentuaisResumo.length ? Math.max(...percentuaisResumo) : null;

  return (
    <Layout>
      <button type="button" onClick={() => navigate("/financas")}
        style={{ background: "none", border: "none", color: "#6366f1", fontSize: "13px", fontWeight: 600, cursor: "pointer", marginBottom: "10px", padding: 0 }}>
        ← Finanças
      </button>

      {/* ── Cabeçalho da Visão Geral ── */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "16px", marginBottom: "20px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "14px", minWidth: 0 }}>
          <span style={{ width: "48px", height: "48px", borderRadius: "14px", display: "grid", placeItems: "center", color: "white", flexShrink: 0, background: "linear-gradient(135deg, #7c74ff, #5b54d6)", boxShadow: "0 8px 20px rgba(99,102,241,0.35)" }}>
            <TrendingUp size={22} />
          </span>
          <div style={{ minWidth: 0 }}>
            <h1 style={{ fontSize: "22px", fontWeight: 800, color: "#0f172a", letterSpacing: "-0.02em", lineHeight: 1.15 }}>Visão Geral do Investimento</h1>
            <p style={{ fontSize: "13px", color: "#64748b", marginTop: "2px" }}>Acompanhe o desenvolvimento da sua carteira em um só lugar.</p>
          </div>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
          {ultimoPonto && (
            <span style={{ display: "inline-flex", alignItems: "center", gap: "7px", fontSize: "12.5px", fontWeight: 600, color: "#475569", background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "9999px", padding: "8px 14px" }}>
              📅 Último período: <strong style={{ color: "#0f172a" }}>{fmtDinheiro(ultimoPonto.valor_bruto)}</strong>
              {ultimoPonto.valor_percentual_acumulado != null && (
                <span style={{ color: "#047857" }}>· {fmtPct(ultimoPonto.valor_percentual_acumulado)}</span>
              )}
            </span>
          )}
          <Button onClick={abrirNova}>+ Nova Operação</Button>
        </div>
      </div>

      {/* ── Filtros ── */}
      <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", alignItems: "flex-end", marginBottom: "20px" }}>
        <div style={{ flex: "1 1 200px", minWidth: "180px" }}>
          <Select label="Carteira" value={filtroCarteira} onChange={e => setFiltroCarteira(e.target.value)}>
            <option value="">Todas as carteiras</option>
            {carteiras.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
          </Select>
        </div>
        <div style={{ flex: "1 1 150px", minWidth: "140px" }}>
          <DateInput label="De" value={filtroDe} onChange={e => setFiltroDe(e.target.value)} />
        </div>
        <div style={{ flex: "1 1 150px", minWidth: "140px" }}>
          <DateInput label="Até" value={filtroAte} onChange={e => setFiltroAte(e.target.value)} />
        </div>
        <Button variant="secondary" onClick={limparFiltros}>Limpar</Button>
      </div>

      {/* ── Visão geral: KPIs + andamento + resumo do período + destaques ── */}
      {!loading && resumo && (
        <>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 210px), 1fr))", gap: "12px", marginBottom: "16px" }}>
            <KpiCard icone={<DollarSign size={20} />} cor="var(--color-primary)" bg="var(--color-primary-light)"
              label="Total investido" valor={fmtDinheiro(resumo.total_investido)} nota="desde o início" />
            <KpiCard icone={<TrendingUp size={20} />} cor="var(--color-info)" bg="var(--color-info-light)"
              label="Total vendido" valor={fmtDinheiro(resumo.total_vendido)} nota="desde o início" />
            <KpiCard icone={lucroPositivo ? <TrendingUp size={20} /> : <TrendingDown size={20} />}
              cor={lucroPositivo ? "var(--color-success)" : "var(--color-danger)"}
              bg={lucroPositivo ? "var(--color-success-light)" : "var(--color-danger-light)"}
              label="Resultado (realizado)" valor={fmtDinheiro(resumo.lucro_total)} nota="lucro total" />
            <KpiCard icone={<Percent size={20} />}
              cor={(resumo.rentabilidade_percentual ?? 0) >= 0 ? "var(--color-success)" : "var(--color-danger)"}
              bg={(resumo.rentabilidade_percentual ?? 0) >= 0 ? "var(--color-success-light)" : "var(--color-danger-light)"}
              label="Rentabilidade" valor={fmtPct(resumo.rentabilidade_percentual)} nota="sobre o capital investido" />
          </div>

          <div style={{ display: "flex", flexWrap: "wrap", gap: "16px", alignItems: "flex-start", marginBottom: "24px" }}>
            {/* Andamento das operações */}
            <div style={{ flex: "2 1 460px", minWidth: 0, ...cardEstilo }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "10px", flexWrap: "wrap", marginBottom: "12px" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                  <span style={{ width: "30px", height: "30px", borderRadius: "9px", display: "grid", placeItems: "center", color: "var(--color-primary)", background: "var(--color-primary-light)" }}>
                    <BarChart3 size={16} />
                  </span>
                  <h2 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>Andamento das operações</h2>
                </div>
                <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                  <Chip>{resumo.qtd_operacoes} operação(ões)</Chip>
                  <Chip>{resumo.qtd_abertas} aberta(s)</Chip>
                  <Chip>{resumo.qtd_fechadas} fechada(s)</Chip>
                </div>
              </div>
              <GraficoComprasVendas serie={resumo.serie} />
            </div>

            {/* Coluna: resumo do período + destaques */}
            <div style={{ flex: "1 1 320px", minWidth: 0, display: "flex", flexDirection: "column", gap: "16px" }}>
              <div style={cardEstilo}>
                <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "12px" }}>
                  <span style={{ width: "30px", height: "30px", borderRadius: "9px", display: "grid", placeItems: "center", color: "#8b5cf6", background: "rgba(139,92,246,0.16)" }}>
                    <PieChart size={16} />
                  </span>
                  <h2 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>Resumo do período</h2>
                </div>
                <ResumoPeriodoDonut totalVendido={resumo.total_vendido} lucro={resumo.lucro_total}
                  rentabilidade={resumo.rentabilidade_percentual} />
              </div>

              <div style={cardEstilo}>
                <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "6px" }}>
                  <span style={{ width: "30px", height: "30px", borderRadius: "9px", display: "grid", placeItems: "center", color: "var(--color-primary)", background: "var(--color-primary-light)" }}>
                    <TrendingUp size={16} />
                  </span>
                  <h2 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>Destaques</h2>
                </div>
                <LinhaDestaque rotulo="Maior valor no período" valor={fmtDinheiro(maiorBruto)} />
                <LinhaDestaque rotulo="Melhor rentabilidade" valor={fmtPct(melhorPct)} />
                <LinhaDestaque rotulo="Operações no período" valor={String(resumo.qtd_operacoes)} ultima />
              </div>
            </div>
          </div>
        </>
      )}

      {/* ── Tabela de operações ── */}
      {loading ? (
        <Spinner text="Carregando operações..." />
      ) : operacoes.length === 0 ? (
        <EmptyState icon="🔄" title="Nenhuma operação" text="Registre a compra de um ativo para começar a acompanhar o lucro."
          actionLabel="Nova Operação" onAction={abrirNova} />
      ) : (
        <div style={{ overflowX: "auto", background: "white", borderRadius: "16px", border: "1px solid #eef2f7", boxShadow: "0 1px 3px rgba(15,23,42,0.05)", marginBottom: "24px" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px", minWidth: "900px" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "#64748b", fontSize: "11px", textTransform: "uppercase", letterSpacing: "0.3px" }}>
                <th style={th}>Ativo</th>
                <th style={th}>Classe</th>
                <th style={th}>Compra</th>
                <th style={th}>Venda</th>
                <th style={th}>Qtd</th>
                <th style={th}>Valor compra</th>
                <th style={th}>Valor venda</th>
                <th style={th}>Lucro/Prejuízo</th>
                <th style={th}>Rentab.</th>
                <th style={th}>Prazo</th>
                <th style={th} />
              </tr>
            </thead>
            <tbody>
              {operacoes.map(op => {
                const lucro = op.lucro;
                const corLucro = lucro == null ? "#64748b" : lucro > 0 ? "#047857" : lucro < 0 ? "#b91c1c" : "#64748b";
                const info = op.classe ? catInfo(op.classe) : null;
                return (
                  <tr key={op.id} style={{ borderTop: "1px solid #f1f5f9" }}>
                    <td style={td}>
                      <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <strong>{op.ticker}</strong>
                        <Badge color={op.aberta ? "#b45309" : "#047857"} bg={op.aberta ? "#fef3c7" : "#d1fae5"}>
                          {op.aberta ? "ABERTA" : "FECHADA"}
                        </Badge>
                      </div>
                      <span style={{ fontSize: "11px", color: "#94a3b8" }}>{carteiraNome(op.carteira_investimento_id)}</span>
                    </td>
                    <td style={td}>{info ? <Badge color={info.color} bg={info.bg}>{info.icon} {info.label}</Badge> : "—"}</td>
                    <td style={td}>{fmtData(op.data_compra)}</td>
                    <td style={td}>{fmtData(op.data_venda)}</td>
                    <td style={td}>{op.quantidade ?? "—"}</td>
                    <td style={td}>
                      {fmtDinheiro(op.valor_inicial)}
                      {op.valor_unitario_compra != null && (
                        <span style={subLinha}>{fmtDinheiro(op.valor_unitario_compra)}/un.</span>
                      )}
                    </td>
                    <td style={td}>
                      {fmtDinheiro(op.valor_venda)}
                      {op.valor_unitario_venda != null && (
                        <span style={subLinha}>{fmtDinheiro(op.valor_unitario_venda)}/un.</span>
                      )}
                    </td>
                    <td style={{ ...td, color: corLucro, fontWeight: 700 }}>{fmtDinheiro(lucro)}</td>
                    <td style={{ ...td, color: corLucro, fontWeight: 700 }}>{fmtPct(op.rentabilidade_percentual)}</td>
                    <td style={td}>{op.aberta ? "—" : op.prazo_dias != null ? `${op.prazo_dias} dias` : "—"}</td>
                    <td style={{ ...td, textAlign: "right", whiteSpace: "nowrap" }}>
                      <Button variant="ghost" size="sm" onClick={() => abrirEdicao(op)} title="Editar">✎</Button>
                      <Button variant="ghost" size="sm" onClick={() => handleDelete(op)} style={{ color: "#ef4444" }} title="Excluir">🗑</Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <PrecosAlvoSection carteiras={carteiras} />

      {/* ── Modal de operação ── */}
      <Modal open={showModal} onClose={fechar} title={editing ? "Editar Operação" : "Nova Operação"}
        onSubmit={handleSalvar} submitLabel="Salvar" width="640px">
        {editing && selecao == null && (
          <div style={{ padding: "10px 14px", background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "10px", fontSize: "12.5px", color: "#475569" }}>
            Ativo desta operação: <strong>{editing.ticker}</strong>. Escolha outro abaixo para trocar.
          </div>
        )}

        <SeletorAtivo carteiras={carteiras} onSelecionar={sel => {
          setSelecao(sel);
          if (sel?.classe) setClasse(sel.classe);
        }} />

        <div style={{ display: "flex", gap: "12px", flexWrap: "wrap" }}>
          <div style={{ flex: "1 1 160px" }}>
            <Select label="Classe" value={classe ?? ""} onChange={e => setClasse((e.target.value || null) as CategoriaInvestimento | null)}>
              <option value="">— Sem classe —</option>
              {CLASSE_OPCOES.map(c => <option key={c.key} value={c.key}>{c.icon} {c.label}</option>)}
            </Select>
          </div>
          <div style={{ flex: "1 1 160px" }}>
            <DateInput label="Data da compra" required value={form.data_compra}
              onChange={e => setForm({ ...form, data_compra: e.target.value })} />
          </div>
        </div>

        <div style={{ display: "flex", gap: "12px", flexWrap: "wrap" }}>
          <div style={{ flex: "1 1 130px" }}>
            <NumberInput label="Quantidade" highPrecision value={form.quantidade}
              onChange={setQuantidade} placeholder="Ex: 79" />
          </div>
          <div style={{ flex: "1 1 130px" }}>
            <NumberInput label="Preço unitário (compra)" decimal value={form.preco_unit_compra}
              onChange={setUnitCompra} placeholder="R$ por unidade" />
          </div>
          <div style={{ flex: "1 1 130px" }}>
            <NumberInput label="Total da compra (R$)" decimal value={form.valor_inicial}
              onChange={setTotalCompra} placeholder="Custo total pago" />
          </div>
        </div>

        <div style={{ display: "flex", gap: "12px", flexWrap: "wrap" }}>
          <div style={{ flex: "1 1 130px" }}>
            <DateInput label="Data da venda (vazio se aberta)" value={form.data_venda}
              onChange={e => setForm({ ...form, data_venda: e.target.value })} />
          </div>
          <div style={{ flex: "1 1 130px" }}>
            <NumberInput label="Preço unitário (venda)" decimal value={form.preco_unit_venda}
              onChange={setUnitVenda} placeholder="R$ por unidade" />
          </div>
          <div style={{ flex: "1 1 130px" }}>
            <NumberInput label="Total da venda (R$)" decimal value={form.valor_venda}
              onChange={setTotalVenda} placeholder="Total recebido" />
          </div>
        </div>

        <Input label="Quantidade vendida (deixe vazio para venda total)" value={form.quantidade_vendida}
          onChange={e => setForm({ ...form, quantidade_vendida: e.target.value.replace(/[^0-9.,]/g, "") })}
          placeholder="Preencha só na venda PARCIAL" />

        <p style={{ fontSize: "11.5px", color: "#94a3b8", margin: 0 }}>
          Digite o <strong>preço unitário</strong> e o <strong>total</strong> sai da multiplicação pela quantidade (e vice-versa).
          O lucro/prejuízo, a rentabilidade e o prazo são calculados no servidor — na venda parcial o custo é rateado pela quantidade vendida.
        </p>
      </Modal>
    </Layout>
  );
}

const th: React.CSSProperties = { padding: "10px 12px", fontWeight: 700 };
const td: React.CSSProperties = { padding: "10px 12px", color: "#0f172a", verticalAlign: "top" };
const subLinha: React.CSSProperties = { display: "block", fontSize: "11px", color: "#94a3b8", fontWeight: 400 };

/* ── Peças da Visão Geral (KPIs, chips, destaques) ── */
const cardEstilo: React.CSSProperties = {
  background: "white", borderRadius: "16px", padding: "20px 22px",
  boxShadow: "0 1px 3px rgba(15,23,42,0.05)", border: "1px solid #eef2f7",
};

function KpiCard({ icone, cor, bg, label, valor, nota }: {
  icone: React.ReactNode; cor: string; bg: string; label: string; valor: string; nota: string;
}) {
  return (
    <div style={{ ...cardEstilo, padding: "16px 18px", display: "flex", alignItems: "flex-start", gap: "13px" }}>
      <span style={{ width: "44px", height: "44px", borderRadius: "12px", display: "grid", placeItems: "center", flexShrink: 0, color: cor, background: bg }}>
        {icone}
      </span>
      <div style={{ display: "flex", flexDirection: "column", gap: "1px", minWidth: 0 }}>
        <span style={{ fontSize: "12.5px", fontWeight: 600, color: "#64748b" }}>{label}</span>
        <span style={{ fontSize: "21px", fontWeight: 800, color: "#0f172a", letterSpacing: "-0.02em", lineHeight: 1.2 }}>{valor}</span>
        <span style={{ fontSize: "11.5px", color: "#94a3b8" }}>{nota}</span>
      </div>
    </div>
  );
}

function Chip({ children }: { children: React.ReactNode }) {
  return (
    <span style={{ fontSize: "11px", fontWeight: 600, color: "#64748b", background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "9999px", padding: "3px 10px", whiteSpace: "nowrap" }}>
      {children}
    </span>
  );
}

function LinhaDestaque({ rotulo, valor, ultima }: { rotulo: string; valor: string; ultima?: boolean }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "12px", padding: "9px 0", borderBottom: ultima ? "none" : "1px solid #f1f5f9" }}>
      <span style={{ fontSize: "12.5px", color: "#64748b" }}>{rotulo}</span>
      <span style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a", fontVariantNumeric: "tabular-nums" }}>{valor}</span>
    </div>
  );
}
