import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import { PageHeader, EmptyState, Spinner } from "../components/UI";
import { Button } from "../components/Shared";
import PlanejamentoNav from "../components/PlanejamentoNav";
import { boxStyle, miniLabel, controlStyle } from "../components/FormStyles";
import { catInfo } from "../utils/percentual";
import type { CarteiraResumo, Comparativo, CategoriaInvestimento } from "../types/planejamento";
import type { NotasPainel, NotasSalvas } from "../types/notas";

/* ══════════════════════════════════════════════════════════════════════
   NOTAS POR SUBCLASSE — a avaliação em UMA página.

   Um checklist por SUBCLASSE ("Financeiro", "Bens Industriais") e uma nota por
   EMPRESA: as perguntas são as colunas, os ativos são as linhas e cada célula
   recebe uma nota de 0 a 10. Várias empresas do mesmo setor compartilham as
   mesmas perguntas — é o que permite comparar duas delas.

   A nota final de cada ativo (média das respostas, 0–100) é o que o motor de
   aporte usa para decidir quem recebe primeiro. O checklist padrão (5 perguntas)
   é criado sozinho na primeira vez que a subclasse é aberta.
   ══════════════════════════════════════════════════════════════════════ */

type Linha = {
  chave: string;
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string;
  classe: string;
  notas: (number | null)[];
  score: number | null;
};

export default function NotasSubclasse() {
  const navigate = useNavigate();
  const [carteiras, setCarteiras] = useState<CarteiraResumo[]>([]);
  const [carteiraId, setCarteiraId] = useState<number | null>(null);
  const [comparativo, setComparativo] = useState<Comparativo | null>(null);
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

  /* ── Comparativo: é dele que saem as subclasses e os ativos de cada uma ── */
  useEffect(() => {
    if (carteiraId == null) return;
    api.get<Comparativo>(`/carteiras-investimento/${carteiraId}/ideal/comparativo`)
      .then(r => setComparativo(r.data))
      .catch(() => setComparativo(null));
  }, [carteiraId]);

  /** Subclasses da carteira (com a classe de cada uma), na ordem da Carteira Ideal. */
  const subclasses = useMemo(() => {
    const lista: { nome: string; classe: CategoriaInvestimento }[] = [];
    for (const c of comparativo?.classes ?? []) {
      for (const s of c.subclasses ?? []) {
        lista.push({ nome: s.nome, classe: c.classe });
      }
    }
    return lista;
  }, [comparativo]);

  /** id da subclasse → nome (o comparativo referencia a subclasse por id). */
  const subclasseDoAtivo = useCallback((subclasseId: number): string | null => {
    for (const c of comparativo?.classes ?? []) {
      for (const s of c.subclasses ?? []) {
        if (s.id === subclasseId) return s.nome;
      }
    }
    return null;
  }, [comparativo]);

  /* ── Escolhe a primeira subclasse ao trocar de carteira ── */
  useEffect(() => {
    if (subclasses.length > 0 && !subclasses.some(s => slugDe(s.nome) === slug)) {
      setSlug(slugDe(subclasses[0].nome));
    }
  }, [subclasses, slug]);

  const subclasseAtual = subclasses.find(s => slugDe(s.nome) === slug) ?? null;

  /* ── Notas da subclasse escolhida ── */
  const carregarNotas = useCallback(async (nome: string, meuSlug: string) => {
    const { data } = await api.get<NotasPainel>(`/notas-subclasse/${meuSlug}`, { params: { nome } });
    setPainel(data);

    // Os ativos vêm do COMPARATIVO (a carteira real, não só quem já foi avaliado);
    // as notas vêm do painel, casadas pelo ativo do catálogo.
    const daSubclasse = (comparativo?.classes ?? [])
      .flatMap(c => (c.ativos ?? []).map(a => ({ ...a, classe: c.classe })))
      .filter(a => a.subclasse_id != null && subclasseDoAtivo(a.subclasse_id) === nome);

    const conjunto = new Map<string, Linha>();
    for (const a of daSubclasse) {
      const chave = a.ativo_cadastro_id ?? `nome:${a.ticker}`;
      conjunto.set(chave, {
        chave,
        ativo_cadastro_id: a.ativo_cadastro_id,
        ativo_id: null,
        ticker: a.ticker,
        classe: a.classe,
        notas: data.perguntas.map(() => null),
        score: null,
      });
    }
    for (const item of data.itens) {
      const chave = item.ativo_cadastro_id ?? `id:${item.ativo_id}`;
      const existente = conjunto.get(chave);
      if (existente) {
        existente.notas = normalizarNotas(item.notas, data.perguntas.length);
        existente.score = item.score;
      } else {
        conjunto.set(chave, {
          chave,
          ativo_cadastro_id: item.ativo_cadastro_id,
          ativo_id: item.ativo_id,
          ticker: item.ticker ?? "—",
          classe: "",
          notas: normalizarNotas(item.notas, data.perguntas.length),
          score: item.score,
        });
      }
    }
    setLinhas([...conjunto.values()]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [comparativo]);

  useEffect(() => {
    if (!subclasseAtual) return;
    carregarNotas(subclasseAtual.nome, slug).catch(() => toast.error("Erro ao carregar as notas"));
  }, [subclasseAtual, slug, carregarNotas]);

  const definirNota = (chave: string, i: number, valor: string) => {
    const numero = valor.trim() === "" ? null : Number(valor.replace(",", "."));
    setLinhas(prev => prev.map(l => (l.chave === chave
      ? { ...l, notas: l.notas.map((n, k) => (k === i ? numero : n)) }
      : l)));
  };

  const salvar = async () => {
    if (!painel || !subclasseAtual) return;
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
        { subclasse_nome: subclasseAtual.nome, itens },
        { params: { nome: subclasseAtual.nome } });
      toast.success(`Notas salvas para ${data.avaliados} ativo(s).`);
      // As notas finais vêm calculadas do servidor: nada de conta paralela na tela.
      setLinhas(prev => prev.map(l => {
        const atual = data.itens.find(i => i.ativo_cadastro_id === l.ativo_cadastro_id
          && (i.ativo_id == null || i.ativo_id === l.ativo_id));
        return atual ? { ...l, score: atual.score, notas: normalizarNotas(atual.notas, l.notas.length) } : l;
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
        <PageHeader icon="📝" title="Notas por subclasse" subtitle="Uma nota por empresa, um checklist por setor" />
        <EmptyState icon="🎯" title="Nenhuma carteira de investimento"
          text="As notas são dadas por subclasse da Carteira Ideal. Crie a carteira e as subclasses primeiro."
          actionLabel="Ir para a Carteira Ideal" onAction={() => navigate("/planejamento/carteira-ideal")} />
      </Layout>
    );
  }

  return (
    <Layout>
      <PlanejamentoNav ativo="notas" />
      <PageHeader icon="📝" title="Notas por subclasse"
        subtitle="Um checklist por subclasse e uma nota para cada empresa — é essa nota que decide a ordem do aporte" />

      {/* ── Carteira + subclasse ── */}
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
          <label style={miniLabel}>Subclasse</label>
          <select value={slug} aria-label="Subclasse" disabled={subclasses.length === 0}
            onChange={e => setSlug(e.target.value)}
            style={{ ...controlStyle, minWidth: "240px", opacity: subclasses.length === 0 ? 0.5 : 1 }}>
            {subclasses.length === 0 && <option value="">— sem subclasse na Carteira Ideal —</option>}
            {subclasses.map(s => (
              <option key={`${s.classe}:${s.nome}`} value={slugDe(s.nome)}>
                {catInfo(s.classe).icon} {s.nome}
              </option>
            ))}
          </select>
        </div>
        {subclasses.length === 0 && (
          <span style={{ fontSize: "12px", color: "#b45309" }}>
            Crie subclasses na Carteira Ideal para avaliar por setor.
          </span>
        )}
      </div>

      {!painel || !subclasseAtual ? (
        <Spinner text="Carregando notas..." />
      ) : linhas.length === 0 ? (
        <EmptyState icon="📝" title="Nenhum ativo nesta subclasse"
          text="Coloque os ativos (ou as metas) nesta subclasse na Carteira Ideal para dar as notas." />
      ) : (
        <div style={boxStyle}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: "10px", flexWrap: "wrap" }}>
            <div>
              <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>
                {subclasseAtual.nome} · {painel.perguntas.length} perguntas
              </h3>
              <p style={{ fontSize: "12px", color: "#64748b", margin: "4px 0 0" }}>
                Nota de 0 a pior / 10 a melhor em cada pergunta. A nota final é a média.
              </p>
            </div>
            <Button onClick={salvar} loading={salvando}>Salvar notas</Button>
          </div>

          <div style={{ overflowX: "auto", marginTop: "14px" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px", minWidth: `${240 + painel.perguntas.length * 118}px` }}>
              <thead>
                <tr style={{ background: "#f8fafc" }}>
                  <th style={{ ...th, textAlign: "left", minWidth: "170px" }}>Empresa</th>
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
                    <td style={{ padding: "6px 10px", fontWeight: 700, color: "#0f172a" }}>{l.ticker}</td>
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
            As perguntas são as mesmas para todas as empresas desta subclasse — é o que permite comparar duas
            empresas do mesmo setor. Quem não tem nota entra depois de quem foi avaliado.
          </p>
        </div>
      )}
    </Layout>
  );
}

/* ── Apoio ── */

/** Nome normalizado da subclasse: a mesma regra do backend. */
function slugDe(nome: string): string {
  return nome.trim().toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
}

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
