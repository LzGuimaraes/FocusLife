import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select } from "../components/Form";
import { EmptyState, Spinner } from "../components/UI";
import { Button, Card, Badge } from "../components/Shared";
import ScoreBadge from "../components/ScoreBadge";
import ChecklistExecucaoModal from "../components/ChecklistExecucaoModal";
import HistoricoAtivoSection from "../components/HistoricoAtivoSection";
import type { AtivoAvaliado, Checklist, ModeloChecklistResumo, Pergunta } from "../types/checklist";
import { tipoInfo } from "../utils/avaliacao";

/* ══════════════════════════════════════════════════════════════════════
   Checklists de um ativo (Módulos 2, 3, 4 e 5).

   A rota aceita duas âncoras:
     • UUID do catálogo  → /avaliacao/<uuid>       (ticker, caso principal)
     • posição do usuário → /avaliacao/p<idNumerico> (renda fixa/sem ticker)
   ══════════════════════════════════════════════════════════════════════ */

export default function AvaliacaoAtivoDetalhe() {
  const { ref } = useParams<{ ref: string }>();
  const navigate = useNavigate();

  const ehPosicao = !!ref && ref.startsWith("p");
  const ativoCadastroId = ehPosicao ? null : (ref ?? null);
  const ativoId = ehPosicao ? Number(ref!.slice(1)) : null;

  const [checklists, setChecklists] = useState<Checklist[]>([]);
  const [consolidado, setConsolidado] = useState<AtivoAvaliado | null>(null);
  const [modelos, setModelos] = useState<ModeloChecklistResumo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [expandido, setExpandido] = useState<number | null>(null);
  const [executando, setExecutando] = useState<Checklist | null>(null);

  const [showNovo, setShowNovo] = useState(false);
  const [modeloId, setModeloId] = useState("");
  const [nome, setNome] = useState("");
  const [salvando, setSalvando] = useState(false);

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const query = ativoCadastroId
        ? `ativo_cadastro_id=${encodeURIComponent(ativoCadastroId)}`
        : `ativo_id=${ativoId}`;
      const [{ data: lista }, resumoRes] = await Promise.all([
        api.get<Checklist[]>(`/checklists/by-ativo?${query}`),
        api.get<AtivoAvaliado[]>("/checklists/resumo"),
      ]);
      setChecklists(lista ?? []);
      const chave = ativoCadastroId ?? `p${ativoId}`;
      setConsolidado((resumoRes.data ?? []).find(
        a => (a.ativo_cadastro_id ?? `p${a.ativo_id}`) === chave) ?? null);
    } catch {
      toast.error("Erro ao carregar os checklists");
    } finally {
      setCarregando(false);
    }
  }, [ativoCadastroId, ativoId]);

  useEffect(() => {
    carregar();
    api.get("/checklist-modelos/all?page=0&size=100")
      .then(r => setModelos(r.data.content ?? []))
      .catch(() => setModelos([]));
  }, [carregar]);

  const criarChecklist = async () => {
    if (!modeloId && !nome.trim()) {
      toast.error("Dê um nome ao checklist ou escolha um modelo.");
      return;
    }
    setSalvando(true);
    try {
      await api.post("/checklists/create", {
        ativo_cadastro_id: ativoCadastroId,
        ativo_id: ativoId,
        nome: nome.trim() === "" ? null : nome.trim(),
        modelo_id: modeloId === "" ? null : Number(modeloId),
        peso: 1,
        ordem: null,
      });
      toast.success("Checklist criado!");
      setShowNovo(false);
      setNome("");
      setModeloId("");
      carregar();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao criar o checklist");
    } finally {
      setSalvando(false);
    }
  };

  const duplicar = (c: Checklist) => {
    toast.promise(api.post(`/checklists/duplicar/${c.id}`, {
      ativo_cadastro_id: ativoCadastroId, ativo_id: ativoId, nome: null, modelo_id: null, peso: 1, ordem: null,
    }), {
      loading: "Duplicando...",
      success: () => { carregar(); return `Cópia de "${c.nome}" criada (sem as respostas).`; },
      error: "Erro ao duplicar",
    });
  };

  const excluir = (c: Checklist) => {
    toast("Excluir este checklist?", {
      description: "As perguntas e as notas registradas nele serão perdidas.",
      action: {
        label: "Sim, excluir",
        onClick: () => toast.promise(api.delete(`/checklists/delete/${c.id}`), {
          loading: "Excluindo...",
          success: () => { carregar(); return "Checklist excluído!"; },
          error: "Erro ao excluir",
        }),
      },
      cancel: { label: "Cancelar", onClick: () => {} },
    });
  };

  const alterarAposExecucao = (atualizado: Checklist) => {
    setChecklists(prev => prev.map(c => (c.id === atualizado.id ? atualizado : c)));
    carregar();
  };

  const titulo = consolidado?.ticker ?? checklists[0]?.ticker ?? "Ativo";

  return (
    <Layout>
      <div style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap", marginBottom: "20px" }}>
        <button onClick={() => navigate("/avaliacao")} aria-label="Voltar"
          style={{ background: "#f1f5f9", border: "none", borderRadius: "8px", padding: "8px 12px", cursor: "pointer", fontSize: "16px", color: "#64748b" }}>←</button>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
            <h1 style={{ fontSize: "24px", fontWeight: 800, color: "#0f172a", margin: 0 }}>{titulo}</h1>
            <ScoreBadge score={consolidado?.quality_score} label="Score" />
          </div>
          <p style={{ fontSize: "13px", color: "#64748b", margin: "4px 0 0" }}>
            {checklists.length} checklist(s)
            {consolidado && ` · ${consolidado.total_respondidas}/${consolidado.total_perguntas} respondidas`}
          </p>
        </div>
        <div style={{ marginLeft: "auto", display: "flex", gap: "8px", flexWrap: "wrap" }}>
          <Button variant="secondary" onClick={() => navigate("/avaliacao/modelos")}>🗂️ Modelos</Button>
          <Button onClick={() => setShowNovo(true)}>+ Novo checklist</Button>
        </div>
      </div>

      {carregando ? <Spinner text="Carregando checklists..." /> : checklists.length === 0 ? (
        <EmptyState icon="📋" title="Nenhum checklist neste ativo"
          text="Crie um checklist a partir de um modelo ou comece em branco e adicione as suas perguntas."
          actionLabel="Novo checklist" onAction={() => setShowNovo(true)} />
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          {checklists.map(c => (
            <Card key={c.id} accent="#8b5cf6">
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "10px", flexWrap: "wrap" }}>
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                    <h3 style={{ fontSize: "16px", fontWeight: 700, color: "#0f172a", margin: 0 }}>{c.nome}</h3>
                    <ScoreBadge score={c.score} size="sm" />
                  </div>
                  <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "8px" }}>
                    <Badge color="#64748b" bg="#f1f5f9">{c.total_respondidas}/{c.total_perguntas} respondidas</Badge>
                    <Badge color="#6366f1" bg="#eef2ff">peso {c.peso}</Badge>
                    {c.total_pontuadas_respondidas === 0 && c.total_perguntas > 0 && (
                      <Badge color="#b45309" bg="#fef3c7">nada pontuado ainda</Badge>
                    )}
                    {c.modelo_origem_id && <Badge color="#8b5cf6" bg="#f5f3ff">do modelo #{c.modelo_origem_id}</Badge>}
                  </div>
                </div>

                <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                  <Button size="sm" onClick={() => setExecutando(c)} disabled={c.total_perguntas === 0}>
                    {c.total_respondidas === 0 ? "▶ Avaliar" : "✏️ Continuar"}
                  </Button>
                  <Button variant="secondary" size="sm" onClick={() => setExpandido(expandido === c.id ? null : c.id)}>
                    {expandido === c.id ? "Ocultar" : "Ver perguntas"}
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => duplicar(c)} title="Duplicar sem copiar as respostas">📄</Button>
                  <Button variant="ghost" size="sm" onClick={() => excluir(c)} style={{ color: "#ef4444" }}>🗑</Button>
                </div>
              </div>

              {expandido === c.id && (
                <div style={{ marginTop: "14px", borderTop: "1px solid #f1f5f9", paddingTop: "12px", display: "flex", flexDirection: "column", gap: "8px" }}>
                  {c.perguntas.length === 0 && (
                    <p style={{ fontSize: "12px", color: "#94a3b8", margin: 0 }}>Sem perguntas.</p>
                  )}
                  {c.perguntas.map((p, i) => <PerguntaDetalhe key={p.id ?? i} pergunta={p} indice={i} />)}
                </div>
              )}
            </Card>
          ))}
        </div>
      )}

      <ChecklistExecucaoModal open={executando != null} checklist={executando}
        onClose={() => setExecutando(null)} onSaved={alterarAposExecucao} />

      {/* Evolução do score deste ativo (Módulo 8) */}
      <HistoricoAtivoSection ativoCadastroId={ativoCadastroId} ativoId={ativoId} />

      <Modal open={showNovo} onClose={() => setShowNovo(false)} title="Novo checklist"
        onSubmit={criarChecklist} submitLabel={salvando ? "Criando..." : "Criar checklist"}>
        <Select label="Modelo de checklist (opcional)" value={modeloId}
          hint="Com modelo, as perguntas são copiadas para este ativo (editar o modelo depois não muda este checklist)."
          onChange={e => setModeloId(e.target.value)}>
          <option value="">— Em branco —</option>
          {modelos.map(m => <option key={m.id} value={m.id}>{m.nome} ({m.total_perguntas} perguntas)</option>)}
        </Select>
        <Input label="Nome do checklist" value={nome}
          hint={modeloId ? "Opcional — o nome do modelo é usado se ficar vazio." : "Obrigatório para checklist em branco."}
          onChange={e => setNome(e.target.value)} placeholder="Ex: Fundamentos" />
        {modeloId === "" && (
          <p style={{ fontSize: "12px", color: "#94a3b8", margin: 0 }}>
            Você poderá adicionar e editar as perguntas na tela do checklist.
          </p>
        )}
      </Modal>
    </Layout>
  );
}

/* ── Leitura de uma pergunta respondida ── */
function PerguntaDetalhe({ pergunta, indice }: { pergunta: Pergunta; indice: number }) {
  const info = tipoInfo(pergunta.tipo);
  const resposta = pergunta.valor_numerico != null
    ? `${pergunta.valor_numerico}${pergunta.tipo === "PERCENTUAL" ? "%" : ""}`
    : pergunta.resposta_texto ?? null;

  return (
    <div style={{ display: "flex", gap: "10px", alignItems: "flex-start", flexWrap: "wrap", fontSize: "12px" }}>
      <span style={{ color: "#94a3b8", minWidth: "18px" }}>{indice + 1}.</span>
      <span style={{ fontWeight: 600, color: "#334155", flex: "1 1 240px" }}>{pergunta.titulo}</span>
      <span style={{ color: "#94a3b8", whiteSpace: "nowrap" }}>
        {info.icon} {info.label} · peso {pergunta.peso} · máx {pergunta.nota_maxima}
      </span>
      <span style={{ minWidth: "110px", textAlign: "right", fontWeight: 700, color: pergunta.nota_atribuida != null ? "#047857" : "#cbd5e1" }}>
        {pergunta.nota_atribuida != null ? `nota ${pergunta.nota_atribuida}` : "sem nota"}
      </span>
      {resposta && (
        <span style={{ color: "#64748b", flexBasis: "100%", paddingLeft: "28px" }}>
          Resposta: <strong>{resposta}</strong>
          {pergunta.observacao && <span style={{ color: "#94a3b8" }}> · {pergunta.observacao}</span>}
        </span>
      )}
      {!pergunta.conta_no_score && (
        <span style={{ color: "#94a3b8", flexBasis: "100%", paddingLeft: "28px" }}>
          (informativa — não entra no score)
        </span>
      )}
    </div>
  );
}
