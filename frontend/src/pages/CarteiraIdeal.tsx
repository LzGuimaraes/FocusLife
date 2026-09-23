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
import { BarrasAtualIdeal, DistribuicaoAtualIdeal } from "../components/GraficosCarteiraIdeal";
import PlanejamentoNav from "../components/PlanejamentoNav";
import DiagnosticoFinanceiroBanner from "../components/DiagnosticoFinanceiroBanner";
import { boxStyle, controlStyle, miniLabel } from "../components/FormStyles";
import { novaChave } from "../utils/chaves";
import { numParaTexto, textoParaNum } from "../utils/numeros";
import type {
  CarteiraIdeal, CarteiraIdealPayload, CarteiraResumo, Comparativo, Estrategia, MeusAtivos,
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
  const [meusAtivos, setMeusAtivos] = useState<MeusAtivos | null>(null);
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

  /* ── Garante SEMPRE uma carteira válida selecionada ──
     O link do menu/hub (`/planejamento/carteira-ideal`, sem id) e ids antigos
     deixavam `selecionada` nulo: NADA era carregado (configuração, comparativo
     e meus-ativos) e a tela anunciava "esta carteira ainda não tem investimentos
     cadastrados" com a carteira cheia — além de o Salvar virar um clique mudo.
     Agora a primeira carteira é escolhida e a URL é corrigida sozinha. */
  useEffect(() => {
    if (carteiras.length === 0) return;
    if (selecionada != null && carteiras.some(c => c.id === selecionada)) return;
    const primeira = carteiras[0].id;
    setSelecionada(primeira);
    navigate(`/planejamento/carteira-ideal/${primeira}`, { replace: true });
  }, [carteiras, selecionada, navigate]);

  /* ── Carrega a configuração + comparativo da carteira selecionada ── */
  const carregar = useCallback(async (id: number) => {
    setCarregando(true);
    try {
      const [idealRes, compRes, meusRes] = await Promise.all([
        api.get<CarteiraIdeal>(`/carteiras-investimento/${id}/ideal`),
        api.get<Comparativo>(`/carteiras-investimento/${id}/ideal/comparativo`),
        api.get<MeusAtivos>(`/carteiras-investimento/${id}/ideal/meus-ativos`),
      ]);
      const ideal = idealRes.data;
      const meus = meusRes.data;
      setClasses(ideal.classes.map(c => ({
        key: novaChave(),
        classe: c.classe,
        percentual_ideal: numParaTexto(c.percentual_ideal),
        tolerancia: numParaTexto(c.tolerancia),
        limite_maximo: numParaTexto(c.limite_maximo),
        subclasses: (c.subclasses ?? []).map(s => ({
          key: novaChave(),
          id: s.id,
          nome: s.nome,
          percentual_ideal: numParaTexto(s.percentual_ideal),
          tolerancia: numParaTexto(s.tolerancia),
          limite_maximo: numParaTexto(s.limite_maximo),
          setores: (s.setores ?? []).map(st => ({
            key: novaChave(),
            id: st.id,
            nome: st.nome,
            percentual_ideal: numParaTexto(st.percentual_ideal),
            tolerancia: numParaTexto(st.tolerancia),
            limite_maximo: numParaTexto(st.limite_maximo),
          })),
        })),
      })));

      // As metas partem dos ATIVOS DA CARTEIRA: cada ativo que o usuário já tem
      // vira uma linha (já marcada quando existe meta) — e os ativos que só
      // existem no planejamento (ainda não comprados) entram como "planejado".
      const metasExistentes = ideal.metas ?? [];
      const daCarteira: MetaDraft[] = (meus.ativos ?? []).map(a => {
        const meta = a.ativo_cadastro_id
          ? metasExistentes.find(m => m.ativo_cadastro_id === a.ativo_cadastro_id)
          : undefined;
        return {
          key: novaChave(),
          ativo_cadastro_id: a.ativo_cadastro_id ?? "",
          ticker: a.ticker,
          classe: a.classe,
          subclasse_nome: meta?.subclasse_nome ?? "",
          setor_nome: meta?.setor_nome ?? "",
          percentual_ideal: meta ? numParaTexto(meta.percentual_ideal) : "",
          prioridade_manual: String(meta?.prioridade_manual ?? 0),
          incluir: meta != null,
          origem: "carteira",
          vinculado: a.vinculado,
          ativo_ids: a.ativo_ids ?? [],
          sugestao_catalogo_id: a.sugestao_catalogo_id,
          sugestao_catalogo_nome: a.sugestao_catalogo_nome,
          subclasse_id: a.subclasse_id ?? null,
          subclasse_nome_posicao: a.subclasse_nome ?? null,
          setor_id: a.setor_id ?? null,
          setor_nome_posicao: a.setor_nome ?? null,
          tolerancia: numParaTexto(meta?.tolerancia ?? null),
          limite_maximo: numParaTexto(meta?.limite_maximo ?? null),
          percentual_atual: a.percentual_atual,
          valor_atual: a.valor_atual,
        };
      });
      const semPosicao = metasExistentes
        .filter(m => m.ativo_cadastro_id && !(meus.ativos ?? []).some(a => a.ativo_cadastro_id === m.ativo_cadastro_id))
        .map(m => ({
          key: novaChave(),
          ativo_cadastro_id: m.ativo_cadastro_id as string,
          ticker: m.ticker ?? "",
          classe: m.classe,
          subclasse_nome: m.subclasse_nome ?? "",
          setor_nome: "",
          percentual_ideal: numParaTexto(m.percentual_ideal),
          prioridade_manual: String(m.prioridade_manual ?? 0),
          incluir: true,
          origem: "planejado" as const,
          vinculado: true,
          ativo_ids: [],
          sugestao_catalogo_id: null,
          sugestao_catalogo_nome: null,
          subclasse_id: null,
          subclasse_nome_posicao: null,
          setor_id: null,
          setor_nome_posicao: null,
          tolerancia: "",
          limite_maximo: "",
          percentual_atual: null,
          valor_atual: null,
        }));

      setMetas([...daCarteira, ...semPosicao]);
      setMeusAtivos(meus);
      setEstrategiaId(ideal.estrategia_id != null ? String(ideal.estrategia_id) : "");
      setAvisos(ideal.avisos ?? []);
      setComparativo(compRes.data);
    } catch {
      // Nunca deixar dados de OUTRA carteira na tela (e não usar o comparativo
      // antigo): se a requisição falhar, a tela fica em estado neutro.
      setMeusAtivos(null);
      setComparativo(null);
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
    const somaClasses = classes.reduce((s, c) => s + textoParaNum(c.percentual_ideal), 0);
    if (classes.length > 0 && !somaFechada(somaClasses)) {
      return `A soma das classes está em ${somaClasses.toFixed(2).replace(".", ",")}% — precisa fechar em 100%. `
        + "Use o botão \"Ajustar para 100%\" na parte de classes.";
    }
    for (const c of classes) {
      if (c.percentual_ideal.trim() === "") return "Informe o percentual ideal de todas as classes (ou remova a classe).";
      for (const s of c.subclasses) {
        if (!s.nome.trim()) return "Toda subclasse precisa de um nome.";
        if (s.percentual_ideal.trim() === "") return `Informe o percentual da subclasse "${s.nome}".`;
      }
    }
    for (const m of metas.filter(x => x.incluir)) {
      if (!m.ativo_cadastro_id) return "Escolha o ativo de cada linha planejada (ou remova a linha).";
      if (m.percentual_ideal.trim() === "") return `Informe o % ideal de ${m.ticker || "cada ativo com meta"}.`;
      const prioridade = parseInt(m.prioridade_manual, 10);
      if (Number.isNaN(prioridade) || prioridade < 0 || prioridade > 10) {
        return `A prioridade de ${m.ticker} deve ficar entre 0 e 10.`;
      }
    }
    return null;
  };

  const handleSalvar = async () => {
    if (selecionada == null) {
      toast.error("Escolha uma carteira antes de salvar.");
      return;
    }
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
        tolerancia: textoParaNum(c.tolerancia),
        limite_maximo: c.limite_maximo.trim() === "" ? null : textoParaNum(c.limite_maximo),
        ordem: i,
        subclasses: c.subclasses.map((s, j) => ({
          nome: s.nome.trim(),
          percentual_ideal: textoParaNum(s.percentual_ideal),
          tolerancia: textoParaNum(s.tolerancia),
          limite_maximo: s.limite_maximo.trim() === "" ? null : textoParaNum(s.limite_maximo),
          ordem: j,
          setores: s.setores.map((st, k) => ({
            nome: st.nome.trim(),
            percentual_ideal: textoParaNum(st.percentual_ideal),
            tolerancia: textoParaNum(st.tolerancia),
            limite_maximo: st.limite_maximo.trim() === "" ? null : textoParaNum(st.limite_maximo),
            ordem: k,
          })),
        })),
      })),
      metas: metas.filter(m => m.incluir).map((m, i) => ({
        ativo_cadastro_id: m.ativo_cadastro_id,
        classe: m.classe,
        subclasse_nome: m.subclasse_nome || null,
        setor_nome: m.setor_nome || null,
        percentual_ideal: textoParaNum(m.percentual_ideal),
        tolerancia: textoParaNum(m.tolerancia),
        limite_maximo: m.limite_maximo.trim() === "" ? null : textoParaNum(m.limite_maximo),
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

  /* ── Vinculação de posição sem catálogo (chamada pelo editor de metas) ── */
  const vincular = async (ativoIds: number[], ativoCadastroId: string) => {
    try {
      await api.post("/ativos/vincular-catalogo", {
        ativo_ids: ativoIds,
        ativo_cadastro_id: ativoCadastroId,
      });
      toast.success("Posição vinculada ao catálogo! Agora ela pode ter meta por ativo.");
      if (selecionada != null) carregar(selecionada);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao vincular a posição");
    }
  };

  /* ── Classificação de posição sem ticker numa subclasse (chamada pelo MetasEditor) ── */
  const atribuirSubclasse = async (ativoIds: number[], subclasseId: number | null) => {
    try {
      await api.post("/ativos/atribuir-subclasse", {
        ativo_ids: ativoIds,
        subclasse_id: subclasseId,
      });
      toast.success(subclasseId == null
        ? "Classificação removida."
        : "Posição classificada! Agora ela conta no alvo da subclasse.");
      if (selecionada != null) carregar(selecionada);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao classificar a posição");
    }
  };

  /* ── Classificação de posição num SETOR (chamada pelo MetasEditor) ── */
  const atribuirSetor = async (ativoIds: number[], setorId: number | null) => {
    try {
      await api.post("/ativos/atribuir-setor", { ativo_ids: ativoIds, setor_id: setorId });
      toast.success(setorId == null ? "Setor removido." : "Posição classificada no setor!");
      if (selecionada != null) carregar(selecionada);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao classificar o setor");
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
  const carteiraAtual = carteiras.find(c => c.id === selecionada);
  // Só afirma "sem investimentos" com a resposta do servidor em mãos: enquanto
  // nada foi carregado não dá para saber (era isso que gerava o falso aviso).
  const semAtivos = meusAtivos != null && meusAtivos.ativos.length === 0;

  return (
    <Layout>
      <PlanejamentoNav ativo="carteira-ideal" />
      <PageHeader
        icon="🎯"
        title="Carteira Ideal"
        subtitle="Classes, subclasses e metas dos seus ativos — a metodologia é sua, o sistema só organiza e calcula"
      />

      {/* Se a carteira selecionada aparecer vazia, o mais provável é que as
          posições tenham ficado sem vínculo numa migração antiga. */}
      <DiagnosticoFinanceiroBanner mostrarSemPosicoes carteiraId={selecionada}
        onReparado={() => { if (selecionada != null) carregar(selecionada); }} />

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

        <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
          {classes.length > 0 && !somaFechada(soma) && (
            <span style={{ fontSize: "12px", color: "#b45309", fontWeight: 600 }}>
              ⚠ Classes em {soma.toFixed(2).replace(".", ",")}% — use "Ajustar para 100%"
            </span>
          )}
          <Button onClick={handleSalvar} loading={salvando}>Salvar</Button>
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
          {semAtivos && (
            <div style={{ background: "#eef2ff", border: "1px solid #c7d2fe", borderRadius: "10px", padding: "12px 16px" }}>
              <p style={{ fontSize: "13px", color: "#3730a3", margin: 0 }}>
                <strong>Esta carteira ainda não tem investimentos cadastrados.</strong> Você pode aplicar um modelo pronto
                e definir metas desde já, ou cadastrar seus investimentos em Finanças para que eles apareçam aqui automaticamente.
              </p>
              <button type="button" onClick={() => navigate("/financas")}
                style={{ marginTop: "8px", background: "white", border: "1px solid #c7d2fe", borderRadius: "8px", padding: "6px 12px", fontSize: "12px", fontWeight: 700, color: "#4338ca", cursor: "pointer" }}>
                Cadastrar investimentos em Finanças →
              </button>
            </div>
          )}
          <CarteiraIdealEditor classes={classes} onChange={setClasses} />
          <MetasEditor metas={metas} classes={classes} onChange={setMetas}
            onVincular={vincular}
            onAtribuirSubclasse={atribuirSubclasse}
            onAtribuirSetor={atribuirSetor}
            moeda={meusAtivos?.moeda ?? "BRL"}
            valorTotal={meusAtivos?.valor_total ?? 0} />
        </div>
      ) : comparativo ? (
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 340px), 1fr))", gap: "16px" }}>
            <DistribuicaoAtualIdeal comparativo={comparativo} />
            <BarrasAtualIdeal comparativo={comparativo} />
          </div>
          <ComparativoTable comparativo={comparativo} />
        </div>
      ) : (
        <EmptyState icon="📊" title="Sem comparativo" text="Salve a Carteira Ideal para ver a comparação com a carteira atual." />
      )}

      {carteiraAtual && (
        <p style={{ fontSize: "11px", color: "#94a3b8", marginTop: "14px" }}>
          Os valores atuais vêm das posições cadastradas em Finanças → {carteiraAtual.nome}.
          Posições sem ticker (renda fixa, caixinhas) contam no total da classe e podem ser classificadas numa subclasse.
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
