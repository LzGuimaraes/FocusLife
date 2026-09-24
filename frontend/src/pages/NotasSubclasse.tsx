import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import { PageHeader, EmptyState, Spinner } from "../components/UI";
import { Button } from "../components/Shared";
import PlanejamentoNav from "../components/PlanejamentoNav";
import { boxStyle, miniLabel, controlStyle } from "../components/FormStyles";
import { catInfo } from "../utils/percentual";
import type { CarteiraResumo } from "../types/planejamento";
import type { NotaBucket, NotasPainel, NotasSalvas } from "../types/notas";

/* ══════════════════════════════════════════════════════════════════════
   NOTAS POR SUBCLASSE — a avaliação em UMA página.

   Um checklist por BALDE e uma nota por ATIVO: as perguntas são as colunas, os
   ativos são as linhas e cada célula recebe uma nota de 0 a 10. A nota final
   (média das respostas, 0–100) é o que o motor de aporte usa para decidir quem
   recebe primeiro.

   O BALDE é onde se dá nota, e ele tem duas formas:
     • SUBCLASSE — "Financeiro", "Bens Industriais": várias empresas do mesmo
       setor respondem às MESMAS perguntas, o que permite comparar duas delas.
     • CLASSE INTEIRA — cripto, renda fixa, Tesouro e caixinhas não têm setor,
       então o balde é a própria classe. O checklist padrão vem do TIPO do ativo
       ("paga dividendos?" é pergunta de ação, não de título público), e as
       posições sem ticker entram na grade como qualquer outro ativo.

   A lista de baldes vem do servidor (`/notas-subclasse/buckets`), montada a
   partir da Carteira Ideal e da carteira real: nada é adivinhado aqui.
   ══════════════════════════════════════════════════════════════════════ */

type Linha = {
  chave: string;
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string;
  nome: string | null;
  notas: (number | null)[];
  score: number | null;
};

export default function NotasSubclasse() {
  const navigate = useNavigate();
  const [carteiras, setCarteiras] = useState<CarteiraResumo[]>([]);
  const [carteiraId, setCarteiraId] = useState<number | null>(null);
  const [buckets, setBuckets] = useState<NotaBucket[]>([]);
  const [slug, setSlug] = useState<string>("");
  const [painel, setPainel] = useState<NotasPainel | null>(null);
  const [linhas, setLinhas] = useState<Linha[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);

  /* ── Carteiras (uma vez) ── */
  useEffect(() => {
    api.get("/carteiras-investimento/all?page=0&size=100")
      .then(r => {
        const lista: CarteiraResumo[] = r.data.content ?? [];
        setCarteiras(lista);
        if (lista.length > 0) setCarteiraId(lista[0].id);
      })
      .catch(() => toast.error("Erro ao carregar carteiras"))
      .finally(() => setCarregando(false));
  }, []);

  /* ── Baldes: subclasses + classes que não usam subclasse (cripto, RF...) ── */
  useEffect(() => {
    if (carteiraId == null) return;
    api.get<NotaBucket[]>("/notas-subclasse/buckets", { params: { carteira_investimento_id: carteiraId } })
      .then(r => {
        setBuckets(r.data);
        setSlug(prev => (r.data.some(b => b.slug === prev) ? prev : (r.data[0]?.slug ?? "")));
      })
      .catch(() => setBuckets([]));
  }, [carteiraId]);

  const bucketAtual = buckets.find(b => b.slug === slug) ?? null;

  /* ── Notas do balde escolhido ── */
  const carregarNotas = useCallback(async (alvo: NotaBucket) => {
    const { data } = await api.get<NotasPainel>(`/notas-subclasse/${alvo.slug}`, {
      params: { nome: alvo.nome, carteira_investimento_id: carteiraId },
    });
    setPainel(data);
    setLinhas(data.itens.map(item => ({
      chave: item.ativo_cadastro_id ?? `pos:${item.ativo_id}`,
      ativo_cadastro_id: item.ativo_cadastro_id,
      ativo_id: item.ativo_id,
      ticker: item.ticker ?? item.nome ?? "—",
      nome: item.nome,
      notas: normalizarNotas(item.notas, data.perguntas.length),
      score: item.score,
    })));
  }, [carteiraId]);

  useEffect(() => {
    if (!bucketAtual) return;
    carregarNotas(bucketAtual).catch(() => toast.error("Erro ao carregar as notas"));
  }, [bucketAtual, carregarNotas]);

  const definirNota = (chave: string, i: number, valor: string) => {
    const numero = valor.trim() === "" ? null : Number(valor.replace(",", "."));
    setLinhas(prev => prev.map(l => (l.chave === chave
      ? { ...l, notas: l.notas.map((n, k) => (k === i ? numero : n)) }
      : l)));
  };

  const salvar = async () => {
    if (!painel || !bucketAtual) return;
    setSalvando(true);
    try {
      const itens = linhas
        .filter(l => l.notas.some(n => n != null))
        .map(l => ({ ativo_cadastro_id: l.ativo_cadastro_id, ativo_id: l.ativo_id, notas: l.notas }));
      if (itens.length === 0) {
        toast.error("Dê pelo menos uma nota antes de salvar.");
        return;
      }
      const { data } = await api.put<NotasSalvas>(`/notas-subclasse/${slug}`,
        { subclasse_nome: bucketAtual.nome, itens },
        { params: { nome: bucketAtual.nome, carteira_investimento_id: carteiraId } });
      toast.success(`Notas salvas para ${data.avaliados} ativo(s).`);
      // As notas finais vêm calculadas do servidor: nada de conta paralela na tela.
      const porChave = new Map(data.itens.map(i => [i.ativo_cadastro_id ?? `pos:${i.ativo_id}`, i]));
      setLinhas(prev => prev.map(l => {
        const atual = porChave.get(l.chave);
        return atual
          ? { ...l, score: atual.score, notas: normalizarNotas(atual.notas, l.notas.length) }
          : l;
      }));
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao salvar as notas");
    } finally {
      setSalvando(false);
    }
  };

  if (carregando) return <Layout><Spinner text="Carregando..." /></Layout>;

  if (carteiras.length === 0) {
    return (
      <Layout>
        <PageHeader icon="📝" title="Notas" subtitle="Um checklist por tipo de ativo, uma nota por ativo" />
        <EmptyState icon="🎯" title="Nenhuma carteira de investimento"
          text="As notas são dadas por subclasse (ou por classe, quando o ativo não tem setor). Crie a carteira primeiro."
          actionLabel="Ir para a Carteira Ideal" onAction={() => navigate("/planejamento/carteira-ideal")} />
      </Layout>
    );
  }

  return (
    <Layout>
      <PlanejamentoNav ativo="notas" />
      <PageHeader icon="📝" title="Notas por tipo de ativo"
        subtitle="Um checklist por subclasse — e, para cripto, renda fixa e Tesouro, o checklist do tipo do ativo" />

      {/* ── Carteira + balde ── */}
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
          <label style={miniLabel}>Onde avaliar</label>
          <select value={slug} aria-label="Subclasse ou tipo de ativo" disabled={buckets.length === 0}
            onChange={e => setSlug(e.target.value)}
            style={{ ...controlStyle, minWidth: "280px", opacity: buckets.length === 0 ? 0.5 : 1 }}>
            {buckets.length === 0 && <option value="">— nada para avaliar nesta carteira —</option>}
            {buckets.map(b => (
              <option key={b.slug} value={b.slug}>
                {catInfo(b.classe).icon} {b.classe_inteira ? `${b.classe_label} (sem subclasse)` : b.nome}
                {` · ${b.qtd_ativos} ativo(s)`}
              </option>
            ))}
          </select>
        </div>
        {bucketAtual?.classe_inteira && (
          <span style={{ fontSize: "12px", color: "#0369a1", maxWidth: "420px" }}>
            {bucketAtual.classe_label} não se divide em setores: o checklist é o do tipo do ativo e vale para toda a classe.
          </span>
        )}
        {buckets.length === 0 && (
          <span style={{ fontSize: "12px", color: "#b45309" }}>
            Monte a Carteira Ideal (classes, subclasses e metas) para os ativos aparecerem aqui.
          </span>
        )}
      </div>

      {!painel || !bucketAtual ? (
        <Spinner text="Carregando notas..." />
      ) : linhas.length === 0 ? (
        <EmptyState icon="📝" title="Nenhum ativo aqui ainda"
          text={bucketAtual.classe_inteira
            ? `Nenhum ativo de ${bucketAtual.classe_label} na carteira.`
            : "Coloque os ativos (ou as metas) nesta subclasse na Carteira Ideal para dar as notas."} />
      ) : (
        <div style={boxStyle}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: "10px", flexWrap: "wrap" }}>
            <div>
              <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>
                {catInfo(painel.classe).icon} {painel.subclasse_nome} · {painel.perguntas.length} perguntas
              </h3>
              <p style={{ fontSize: "12px", color: "#64748b", margin: "4px 0 0" }}>
                Nota de 0 (pior) a 10 (melhor) em cada pergunta. A nota final é a média das respostas.
              </p>
            </div>
            <Button onClick={salvar} loading={salvando}>Salvar notas</Button>
          </div>

          <div style={{ overflowX: "auto", marginTop: "14px" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px", minWidth: `${260 + painel.perguntas.length * 118}px` }}>
              <thead>
                <tr style={{ background: "#f8fafc" }}>
                  <th style={{ ...th, textAlign: "left", minWidth: "190px" }}>Ativo</th>
                  {painel.perguntas.map(p => (
                    <th key={p.id} style={{ ...th, textAlign: "center", maxWidth: "150px" }} title={p.titulo}>
                      {p.titulo}
                    </th>
                  ))}
                  <th style={{ ...th, textAlign: "right" }}>Nota</th>
                </tr>
              </thead>
              <tbody>
                {linhas.map(l => (
                  <tr key={l.chave} style={{ borderTop: "1px solid #f1f5f9" }}>
                    <td style={{ padding: "6px 10px", color: "#0f172a" }}>
                      <span style={{ fontWeight: 700 }}>{l.ticker}</span>
                      {l.nome && l.nome.trim() !== l.ticker.trim() && (
                        <span style={{ display: "block", fontSize: "11.5px", color: "#64748b" }}>{l.nome}</span>
                      )}
                    </td>
                    {painel.perguntas.map((p, i) => (
                      <td key={p.id} style={{ padding: "6px 8px", textAlign: "center" }}>
                        <input value={l.notas[i] ?? ""} inputMode="decimal"
                          aria-label={`${p.titulo} — ${l.ticker}`}
                          placeholder="—"
                          onChange={e => definirNota(l.chave, i, e.target.value.replace(/[^0-9.,]/g, ""))}
                          style={{ ...controlStyle, width: "58px", textAlign: "center", fontSize: "12px" }} />
                      </td>
                    ))}
                    <td style={{ padding: "6px 10px", textAlign: "right", fontWeight: 800,
                      color: l.score != null ? scoreCor(l.score) : "#cbd5e1" }}>
                      {l.score != null ? `${l.score.toFixed(1)}%` : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <p style={{ fontSize: "11.5px", color: "#94a3b8", margin: "12px 0 0" }}>
            {painel.classe_inteira
              ? `As perguntas são o checklist padrão de ${painel.classe_label} (o mesmo para todos os ativos do tipo).`
              : "As perguntas são as mesmas para todas as empresas desta subclasse — é o que permite comparar duas empresas do mesmo setor."}
            {" "}Quem não tem nota entra depois de quem foi avaliado.
          </p>
        </div>
      )}
    </Layout>
  );
}

/* ── Apoio ── */

/** Garante que a lista de notas tenha o tamanho das perguntas. */
function normalizarNotas(notas: (number | null)[] | undefined, tamanho: number): (number | null)[] {
  const base = notas ?? [];
  return Array.from({ length: tamanho }, (_, i) => base[i] ?? null);
}

const scoreCor = (score: number): string =>
  score >= 70 ? "#047857" : score >= 40 ? "#b45309" : "#b91c1c";

const th = {
  padding: "8px 10px", fontSize: "10.5px", fontWeight: 700, color: "#64748b",
  textTransform: "uppercase" as const, letterSpacing: "0.3px",
};
