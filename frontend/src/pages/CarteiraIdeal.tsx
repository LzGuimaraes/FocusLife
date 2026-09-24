import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import { PageHeader, EmptyState, Spinner } from "../components/UI";
import CarteiraIdealEditor, { type ClasseDraft, ajustarClassesPara100 } from "../components/CarteiraIdealEditor";
import MetasEditor, { type MetaDraft } from "../components/MetasEditor";
import ComparativoTable from "../components/ComparativoTable";
import { BarrasAtualIdeal, DistribuicaoAtualIdeal, DonutsAtualIdeal } from "../components/GraficosCarteiraIdeal";
import PlanejamentoNav from "../components/PlanejamentoNav";
import DiagnosticoFinanceiroBanner from "../components/DiagnosticoFinanceiroBanner";
import {
  controlStyle, miniLabel, noticeCard, numGrande, overline, primaryBtn,
  secondaryBtn, segmentBtn, segmented,
} from "../components/FormStyles";
import { novaChave } from "../utils/chaves";
import { numParaTexto, textoParaNum } from "../utils/numeros";
import { catInfo, fmtPercentual, somaFechada, CATEGORIAS } from "../utils/percentual";
import type {
  CarteiraIdeal, CarteiraIdealPayload, CarteiraResumo, Comparativo, Estrategia, MeusAtivos,
} from "../types/planejamento";

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
  /** Pendências aparecem logo abaixo do cabeçalho (e podem ser recolhidas). */
  const [alertasAbertos, setAlertasAbertos] = useState(true);
  /** Payload gravado no servidor: base para saber se há alteração não salva. */
  const [assinaturaSalva, setAssinaturaSalva] = useState<string>("");

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
      const classesCarregadas: ClasseDraft[] = ideal.classes.map(c => ({
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
      }));

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
          preco_maximo_compra: numParaTexto(meta?.preco_maximo_compra ?? null),
          percentual_atual: a.percentual_atual,
          valor_atual: a.valor_atual,
          preco_atual: a.preco_atual,
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
          preco_maximo_compra: numParaTexto(m.preco_maximo_compra ?? null),
          percentual_atual: null,
          valor_atual: null,
          preco_atual: null,
        }));

      const metasCarregadas: MetaDraft[] = [...daCarteira, ...semPosicao];
      setClasses(classesCarregadas);
      setMetas(metasCarregadas);
      setMeusAtivos(meus);
      setEstrategiaId(ideal.estrategia_id != null ? String(ideal.estrategia_id) : "");
      setAvisos(ideal.avisos ?? []);
      setComparativo(compRes.data);
      // Fotografia do que está GRAVADO: qualquer diferença a partir daqui é
      // alteração não salva (o botão flutuante de salvar aparece sozinho).
      setAssinaturaSalva(JSON.stringify(montarPayload(
        classesCarregadas, metasCarregadas, ideal.estrategia_id != null ? String(ideal.estrategia_id) : "")));
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

    const payload = montarPayload(classes, metas, estrategiaId);

    setSalvando(true);
    try {
      const { data } = await api.put<CarteiraIdeal>(`/carteiras-investimento/${selecionada}/ideal`, payload);
      setAvisos(data.avisos ?? []);
      const comp = await api.get<Comparativo>(`/carteiras-investimento/${selecionada}/ideal/comparativo`);
      setComparativo(comp.data);
      // O que está na tela é o que acabou de ser gravado: some o aviso de
      // "alterações não salvas" (mesma normalização do payload).
      setAssinaturaSalva(JSON.stringify(payload));
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

  /** Leva a soma das classes para 100% (mesma regra de sempre, agora no cabeçalho). */
  const ajustarPara100 = () => {
    if (classes.length === 0) {
      toast.info("Adicione ou aplique um modelo antes de ajustar.");
      return;
    }
    const ajustadas = ajustarClassesPara100(classes);
    if (ajustadas == null) {
      toast.info("A soma das classes já está em 100%.");
      return;
    }
    setClasses(ajustadas);
    toast.success(soma > 100
      ? "Percentuais reduzidos proporcionalmente para somar 100%."
      : `Completei os ${(100 - soma).toFixed(2).replace(".", ",")}% que faltavam com "Outros".`);
  };

  /** % atual de cada classe (vem do comparativo) — só leitura, para os cards. */
  const atualPorClasse = Object.fromEntries(
    (comparativo?.classes ?? []).map(c => [c.classe, c.percentual_atual]),
  );

  /**
   * ALTERAÇÕES NÃO SALVAS: compara o payload que seria enviado agora com o que
   * está gravado no servidor (mesma função `montarPayload`). É o que faz o
   * botão flutuante de salvar aparecer só quando há algo para salvar.
   */
  const sujo = assinaturaSalva !== ""
    && JSON.stringify(montarPayload(classes, metas, estrategiaId)) !== assinaturaSalva;

  /** Descartar volta para o que está no servidor (com confirmação — é perda de edição). */
  const descartarAlteracoes = () => {
    toast("Descartar as alterações não salvas?", {
      description: "A configuração volta para o que está gravado no servidor.",
      action: {
        label: "Sim, descartar",
        onClick: () => { if (selecionada != null) carregar(selecionada); },
      },
      cancel: { label: "Cancelar", onClick: () => {} },
    });
  };

  /**
   * PENDÊNCIAS em cards (§1 das diretrizes): o que antes era texto solto na
   * tela agora tem tom (erro/atenção/informação), título, explicação e —
   * quando existe — o botão que resolve.
   */
  const alertas: { id: string; tom: "erro" | "atencao" | "info"; cor: string; titulo: string; texto?: string; acaoLabel?: string; acao?: () => void }[] = [];
  if (classes.length > 0 && !somaFechada(soma)) {
    alertas.push({
      id: "soma",
      tom: "erro",
      cor: "#b91c1c",
      titulo: `As classes somam ${fmtPercentual(soma)} e precisam fechar em 100%`,
      texto: soma > 100
        ? "O excedente é reduzido proporcionalmente entre as classes."
        : "A diferença é completada em \"Outros\" — depois você pode redistribuir.",
      acaoLabel: "⚖️ Ajustar para 100%",
      acao: ajustarPara100,
    });
  }
  for (const c of classes) {
    const somaSub = c.subclasses.reduce((s, x) => s + textoParaNum(x.percentual_ideal), 0);
    // Mesma regra da tela de classes: o % da subclasse é uma FATIA DA CLASSE,
    // então a soma dela precisa fechar em 100% DA CLASSE (e não da carteira).
    if (c.subclasses.length > 0 && somaSub > 100.01) {
      alertas.push({
        id: `sub-${c.key}`,
        tom: "atencao",
        cor: "#92400e",
        titulo: `Subclasses de ${catInfo(c.classe).label} somam ${fmtPercentual(somaSub)}`,
        texto: "O percentual da subclasse é uma fatia da classe: o detalhamento precisa fechar em 100% da classe.",
      });
    }
  }
  if (aba === "config") {
    avisos.forEach((a, i) => {
      alertas.push({ id: `aviso-${i}`, tom: "atencao", cor: "#92400e", titulo: humanizarClasse(a) });
    });
  }

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

      {/* ── BARRA DE AÇÕES ──
          Progresso da soma das classes e as duas ações que fecham o fluxo. */}
      <div style={barraAcoes}>
        <div style={{ display: "flex", gap: "16px", alignItems: "flex-end", flexWrap: "wrap" }}>
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

          {/* Progresso da soma das classes: o número manda, a barra confirma. */}
          {classes.length > 0 && (
            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
              <div>
                <span style={overline}>Soma das classes</span>
                <div style={{ display: "flex", alignItems: "baseline", gap: "5px", marginTop: "1px" }}>
                  <span style={{ ...numGrande, fontSize: "19px", color: somaFechada(soma) ? "#047857" : "#b45309" }}>
                    {fmtPercentual(soma)}
                  </span>
                  <span style={{ fontSize: "12.5px", fontWeight: 700, color: "#94a3b8" }}>/ 100%</span>
                </div>
              </div>
              <div style={{ width: "84px", height: "6px", borderRadius: "4px", background: "#f1f5f9", overflow: "hidden" }}>
                <div style={{
                  width: `${Math.min(100, soma)}%`, height: "100%", borderRadius: "4px",
                  background: somaFechada(soma) ? "#10b981" : "#f59e0b", transition: "width 0.35s ease",
                }} />
              </div>
            </div>
          )}

          <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
            {alertas.length > 0 && (
              <button type="button" onClick={() => setAlertasAbertos(v => !v)} aria-expanded={alertasAbertos}
                style={{
                  ...secondaryBtn,
                  display: "inline-flex", alignItems: "center", gap: "7px",
                  color: alertas.some(a => a.tom === "erro") ? "#b91c1c" : "#92400e",
                  borderColor: alertas.some(a => a.tom === "erro") ? "#fecaca" : "#fde68a",
                  background: alertas.some(a => a.tom === "erro") ? "#fef2f2" : "#fffbeb",
                }}>
                {alertas.some(a => a.tom === "erro") ? "⛔" : "⚠️"} {alertas.length} pendência{alertas.length > 1 ? "s" : ""}
                <span style={{ fontSize: "10px" }}>{alertasAbertos ? "▲" : "▼"}</span>
              </button>
            )}
            <button type="button" onClick={ajustarPara100} style={secondaryBtn}>⚖️ Ajustar para 100%</button>
            <button type="button" onClick={handleSalvar} disabled={salvando} style={primaryBtn}>
              {salvando ? "Salvando…" : "Salvar"}
            </button>
          </div>
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "12px", flexWrap: "wrap", marginTop: "14px" }}>
          <div style={segmented}>
            <button type="button" onClick={() => setAba("config")} style={segmentBtn(aba === "config")}
              aria-pressed={aba === "config"}>⚙️ Configuração</button>
            <button type="button" onClick={() => setAba("comparativo")} style={segmentBtn(aba === "comparativo")}
              aria-pressed={aba === "comparativo"}>📊 Comparativo</button>
          </div>
          <p style={{ fontSize: "11.5px", color: "#94a3b8", margin: 0 }}>
            O salvamento é da configuração inteira — nada é enviado até você clicar em Salvar.
          </p>
        </div>
      </div>

      {/* ── Pendências em CARDS expansíveis (substituem o texto solto) ── */}
      {alertasAbertos && alertas.length > 0 && (
        <div style={{ display: "flex", flexDirection: "column", gap: "10px", marginBottom: "18px" }}>
          {alertas.map(a => (
            <div key={a.id} style={noticeCard(a.tom === "erro" ? "#dc2626" : a.tom === "atencao" ? "#d97706" : "#6366f1",
              a.tom === "erro" ? "#fef2f2" : a.tom === "atencao" ? "#fffbeb" : "#eef2ff",
              a.tom === "erro" ? "#fecaca" : a.tom === "atencao" ? "#fde68a" : "#c7d2fe")}>
              <div style={{ display: "flex", gap: "11px", alignItems: "flex-start" }}>
                <span style={{ fontSize: "16px", lineHeight: 1.2 }}>
                  {a.tom === "erro" ? "⛔" : a.tom === "atencao" ? "⚠️" : "ℹ️"}
                </span>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ margin: 0, fontSize: "13.5px", fontWeight: 800, color: a.cor }}>{a.titulo}</p>
                  {a.texto && (
                    <p style={{ margin: "4px 0 0", fontSize: "12.5px", color: a.cor, lineHeight: 1.5 }}>{a.texto}</p>
                  )}
                  {a.acaoLabel && (
                    <button type="button" onClick={a.acao}
                      style={{ marginTop: "10px", ...secondaryBtn, padding: "6px 12px", fontSize: "12px" }}>
                      {a.acaoLabel}
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {carregando ? <Spinner text="Carregando carteira..." /> : aba === "config" ? (
        <div style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
          {semAtivos && (
            <div style={noticeCard("#6366f1", "#eef2ff", "#c7d2fe")}>
              <div style={{ display: "flex", gap: "11px", alignItems: "flex-start" }}>
                <span style={{ fontSize: "17px", lineHeight: 1.2 }}>📥</span>
                <div>
                  <p style={{ fontSize: "13.5px", fontWeight: 800, color: "#3730a3", margin: 0 }}>
                    Esta carteira ainda não tem investimentos cadastrados
                  </p>
                  <p style={{ fontSize: "12.5px", color: "#4338ca", margin: "4px 0 0", lineHeight: 1.5 }}>
                    Você pode aplicar um modelo pronto e definir metas desde já, ou cadastrar seus investimentos em
                    Finanças para que eles apareçam aqui automaticamente.
                  </p>
                  <button type="button" onClick={() => navigate("/financas")}
                    style={{ marginTop: "10px", ...secondaryBtn, padding: "6px 12px", fontSize: "12px" }}>
                    Cadastrar investimentos em Finanças →
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Visualização gráfica integrada: os dois retratos lado a lado antes
              de qualquer edição — dá o contexto antes de mexer nos números. */}
          {comparativo && <DonutsAtualIdeal comparativo={comparativo} />}

          <CarteiraIdealEditor classes={classes} onChange={setClasses} atualPorClasse={atualPorClasse} />
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

      {/* ── Salvar flutuante ──
          Só aparece com alteração pendente, no canto — não ocupa a tela como a
          barra fixa ocupava. */}
      {sujo && !carregando && (
        <div style={pilulaSalvar}>
          <span style={{ display: "inline-flex", alignItems: "center", gap: "7px", fontSize: "12.5px", fontWeight: 700, color: "#4338ca" }}>
            <span style={{ width: "8px", height: "8px", borderRadius: "50%", background: "#f59e0b" }} />
            Alterações não salvas
          </span>
          <button type="button" onClick={descartarAlteracoes} title="Descartar alterações"
            aria-label="Descartar alterações não salvas"
            style={{ ...secondaryBtn, padding: "7px 11px", fontSize: "13px", borderRadius: "9999px" }}>
            ↺
          </button>
          <button type="button" onClick={handleSalvar} disabled={salvando}
            style={{ ...primaryBtn, borderRadius: "9999px", padding: "9px 16px" }}>
            {salvando ? "Salvando…" : "💾 Salvar"}
          </button>
        </div>
      )}
    </Layout>
  );
}

/* Barra de ações do topo: progresso da soma das classes + ações do fluxo.
   Fica no fluxo normal da página (NÃO é fixa): cabeçalho grudado roubava
   altura de tela e atrapalhava a leitura da configuração. */
const barraAcoes: React.CSSProperties = {
  background: "white", borderRadius: "16px", padding: "16px 20px",
  border: "1px solid #eef2f7", boxShadow: "0 1px 3px rgba(15,23,42,0.05)",
  marginBottom: "18px",
};

/**
 * Os avisos do servidor citam a classe pelo nome do enum ("FIIS", "RENDA_FIXA"):
 * aqui é só APRESENTAÇÃO, trocando o token pelo rótulo que o usuário conhece.
 */
const humanizarClasse = (texto: string): string =>
  CATEGORIAS.reduce((t, c) => t.split(c.key).join(c.label), texto);

/* Botão flutuante de salvar: aparece só quando existe alteração não salva, no
   canto inferior direito — discreto e sem cobrir o conteúdo (a barra do topo
   não é fixa de propósito). */
const pilulaSalvar: React.CSSProperties = {
  position: "fixed", right: "22px", bottom: "22px", zIndex: 60,
  display: "flex", alignItems: "center", gap: "12px",
  background: "white", border: "1px solid #e0e7ff", borderRadius: "9999px",
  padding: "8px 10px 8px 16px", boxShadow: "0 12px 28px -10px rgba(15,23,42,0.35)",
};

/**
 * PAYLOAD da Carteira Ideal — a MESMA normalização usada para salvar e para
 * saber se há alteração não salva (comparação por JSON). Fica em um só lugar
 * justamente para o aviso de "não salvo" nunca divergir do que é enviado.
 */
function montarPayload(classes: ClasseDraft[], metas: MetaDraft[], estrategiaId: string): CarteiraIdealPayload {
  return {
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
      preco_maximo_compra: m.preco_maximo_compra.trim() === "" ? null : textoParaNum(m.preco_maximo_compra),
      prioridade_manual: parseInt(m.prioridade_manual, 10) || 0,
      ordem: i,
    })),
  };
}
