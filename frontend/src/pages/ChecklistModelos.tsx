import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select, TextArea } from "../components/Form";
import { PageHeader, CardGrid, EmptyState, Spinner } from "../components/UI";
import { Button, Card, Badge } from "../components/Shared";
import PlanejamentoNav from "../components/PlanejamentoNav";
import ChecklistBuilder from "../components/ChecklistBuilder";
import {
  draftsParaPayloads, perguntaParaDraft, validarPerguntaDraft, type PerguntaDraft,
} from "../components/PerguntaEditor";
import type {
  ModeloChecklist, ModeloChecklistPayload, ModeloChecklistResumo, TipoChecklist,
} from "../types/checklist";
import { TIPOS_CHECKLIST } from "../types/checklist";

/* ══════════════════════════════════════════════════════════════════════
   Modelos de checklist (Módulo 4): templates reutilizáveis.

   Um modelo é o "molde" (ex.: checklist padrão de ações). Aplicá-lo a um
   ativo copia as perguntas para aquele ativo — depois disso, cada um evolui
   de forma independente.
   ══════════════════════════════════════════════════════════════════════ */

const TIPOS_ALVO = [
  { value: "", label: "Qualquer tipo de ativo" },
  { value: "ACAO", label: "📈 Ação" },
  { value: "FII", label: "🏢 FII" },
  { value: "ETF", label: "📦 ETF" },
  { value: "BDR", label: "🌎 BDR" },
  { value: "CRIPTOMOEDA", label: "₿ Criptomoeda" },
];

const tipoAlvoLabel = (v: string | null) =>
  TIPOS_ALVO.find(t => t.value === (v ?? ""))?.label ?? "Qualquer tipo";

interface FormData {
  nome: string;
  descricao: string;
  tipo_alvo: string;
  tipo: TipoChecklist;
  ativa: boolean;
}

const emptyForm: FormData = { nome: "", descricao: "", tipo_alvo: "", tipo: "QUALIDADE", ativa: true };

export default function ChecklistModelos() {
  const [modelos, setModelos] = useState<ModeloChecklistResumo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editando, setEditando] = useState<ModeloChecklist | null>(null);
  const [form, setForm] = useState<FormData>(emptyForm);
  const [perguntas, setPerguntas] = useState<PerguntaDraft[]>([]);
  const [erroNome, setErroNome] = useState<string>();

  const carregar = async () => {
    setCarregando(true);
    try {
      const { data } = await api.get("/checklist-modelos/all?page=0&size=100");
      setModelos(data.content ?? []);
    } catch {
      toast.error("Erro ao carregar os modelos");
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => { carregar(); }, []);

  const abrirNovo = () => {
    setEditando(null);
    setForm(emptyForm);
    setPerguntas([]);
    setErroNome(undefined);
    setShowModal(true);
  };

  const abrirEdicao = async (id: number) => {
    try {
      const { data } = await api.get<ModeloChecklist>(`/checklist-modelos/all/${id}`);
      setEditando(data);
      setForm({
        nome: data.nome,
        descricao: data.descricao ?? "",
        tipo_alvo: data.tipo_alvo ?? "",
        tipo: data.tipo ?? "QUALIDADE",
        ativa: data.ativa,
      });
      setPerguntas((data.perguntas ?? []).map(perguntaParaDraft));
      setErroNome(undefined);
      setShowModal(true);
    } catch {
      toast.error("Erro ao abrir o modelo");
    }
  };

  const handleSubmit = async () => {
    if (!form.nome.trim()) {
      setErroNome("Informe o nome do modelo.");
      return;
    }
    for (const p of perguntas) {
      const problema = validarPerguntaDraft(p);
      if (problema) {
        toast.error(problema);
        return;
      }
    }

    const payload: ModeloChecklistPayload = {
      nome: form.nome.trim(),
      descricao: form.descricao.trim() === "" ? null : form.descricao.trim(),
      tipo_alvo: form.tipo_alvo === "" ? null : form.tipo_alvo,
      tipo: form.tipo,
      ativa: form.ativa,
      perguntas: draftsParaPayloads(perguntas),
    };

    setSalvando(true);
    try {
      if (editando) {
        await api.put(`/checklist-modelos/alter/${editando.id}`, payload);
      } else {
        await api.post("/checklist-modelos/create", payload);
      }
      toast.success(editando ? "Modelo atualizado!" : "Modelo criado!");
      setShowModal(false);
      carregar();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao salvar o modelo");
    } finally {
      setSalvando(false);
    }
  };

  const duplicar = (m: ModeloChecklistResumo) => {
    toast.promise(api.post(`/checklist-modelos/duplicar/${m.id}`), {
      loading: "Duplicando...",
      success: () => { carregar(); return `Cópia de "${m.nome}" criada!`; },
      error: "Erro ao duplicar",
    });
  };

  const excluir = (m: ModeloChecklistResumo) => {
    toast("Excluir este modelo?", {
      description: "Os checklists já criados a partir dele não são afetados.",
      action: {
        label: "Sim, excluir",
        onClick: () => toast.promise(api.delete(`/checklist-modelos/delete/${m.id}`), {
          loading: "Excluindo...",
          success: () => { carregar(); return "Modelo excluído!"; },
          error: "Erro ao excluir",
        }),
      },
      cancel: { label: "Cancelar", onClick: () => {} },
    });
  };

  return (
    <Layout>
      <PlanejamentoNav ativo="modelos" />
      <PageHeader icon="🗂️" title="Modelos de Checklist"
        subtitle="Crie o molde da sua avaliação uma vez e aplique em quantos ativos quiser"
        actionLabel="Novo Modelo" onAction={abrirNovo} />

      {carregando ? <Spinner text="Carregando modelos..." /> : modelos.length === 0 ? (
        <EmptyState icon="🗂️" title="Nenhum modelo ainda"
          text="Um modelo agrupa as perguntas que você usa para avaliar um ativo (ex.: Fundamentos, Dividendos, Riscos)."
          actionLabel="Criar Modelo" onAction={abrirNovo} />
      ) : (
        <CardGrid>
          {modelos.map(m => (
            <Card key={m.id} accent="#8b5cf6">
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", gap: "8px", marginBottom: "10px" }}>
                <Badge color="#8b5cf6" bg="#f5f3ff">{tipoAlvoLabel(m.tipo_alvo)}</Badge>
                {!m.ativa && <Badge color="#64748b" bg="#f1f5f9">Inativo</Badge>}
              </div>
              <h3 style={{ fontSize: "16px", fontWeight: 700, color: "#0f172a", marginBottom: "4px" }}>{m.nome}</h3>
              {m.descricao && (
                <p style={{ fontSize: "12px", color: "#64748b", lineHeight: 1.5, marginBottom: "10px" }}>{m.descricao}</p>
              )}
              <p style={{ fontSize: "12px", color: "#94a3b8", marginBottom: "14px" }}>
                {m.total_perguntas} pergunta(s)
              </p>
              <div style={{ display: "flex", gap: "8px", marginTop: "auto", flexWrap: "wrap" }}>
                <Button variant="secondary" size="sm" onClick={() => abrirEdicao(m.id)}>✏️ Editar</Button>
                <Button variant="ghost" size="sm" onClick={() => duplicar(m)} title="Duplicar modelo">📄</Button>
                <Button variant="ghost" size="sm" onClick={() => excluir(m)} style={{ color: "#ef4444" }}>🗑</Button>
              </div>
            </Card>
          ))}
        </CardGrid>
      )}

      <Modal open={showModal} onClose={() => setShowModal(false)} width="760px"
        title={editando ? `Editar "${editando.nome}"` : "Novo Modelo de Checklist"}
        onSubmit={handleSubmit} submitLabel={salvando ? "Salvando..." : "Salvar modelo"}>
        <Input label="Nome do modelo" value={form.nome} error={erroNome} required
          onChange={e => setForm({ ...form, nome: e.target.value })}
          placeholder="Ex: Checklist padrão de ações" />
        <TextArea label="Descrição" value={form.descricao}
          onChange={e => setForm({ ...form, descricao: e.target.value })}
          placeholder="Para que serve este modelo?" />
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
          <Select label="Tipo de ativo sugerido" value={form.tipo_alvo}
            onChange={e => setForm({ ...form, tipo_alvo: e.target.value })}>            {TIPOS_ALVO.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
          </Select>
          <Select label="Eixo do checklist" value={form.tipo}
            hint={TIPOS_CHECKLIST.find(t => t.value === form.tipo)?.descricao}
            onChange={e => setForm({ ...form, tipo: e.target.value as TipoChecklist })}>
            {TIPOS_CHECKLIST.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
          </Select>
        </div>

        <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "13px", fontWeight: 600, color: "#374151" }}>
          <input type="checkbox" checked={form.ativa} onChange={e => setForm({ ...form, ativa: e.target.checked })}
            style={{ width: "18px", height: "18px", accentColor: "#6366f1" }} />
          Modelo ativo
        </label>

        <ChecklistBuilder perguntas={perguntas} onChange={setPerguntas}
          titulo="Perguntas do modelo"
          descricao="Estas perguntas serão copiadas para o checklist de cada ativo que usar este modelo." />
      </Modal>
    </Layout>
  );
}
