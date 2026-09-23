import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select } from "../components/Form";
import { PageHeader, CardGrid, EmptyState, Spinner, ProgressBar } from "../components/UI";
import { Card, Badge } from "../components/Shared";
import ScoreBadge from "../components/ScoreBadge";
import PlanejamentoNav from "../components/PlanejamentoNav";
import AtivoAutocomplete, { type AtivoCadastro } from "../components/AtivoAutocomplete";
import type { AtivoAvaliado, ModeloChecklistResumo } from "../types/checklist";
import { miniLabel } from "../components/FormStyles";

/* ══════════════════════════════════════════════════════════════════════
   Avaliação de Ativos (Módulos 2 e 5): lista os ativos que o usuário já
   avalia, com o Quality Score consolidado de cada um.

   O Quality Score é SEMPRE a média ponderada das notas que o usuário
   atribuiu — o sistema não opina sobre o ativo.
   ══════════════════════════════════════════════════════════════════════ */

/** Chave da rota: UUID do catálogo ou "p<id>" para uma posição (renda fixa). */
export const refDoAtivo = (a: AtivoAvaliado): string =>
  a.ativo_cadastro_id ?? `p${a.ativo_id ?? ""}`;

export default function AvaliacaoAtivos() {
  const navigate = useNavigate();
  const [ativos, setAtivos] = useState<AtivoAvaliado[]>([]);
  const [modelos, setModelos] = useState<ModeloChecklistResumo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [ticker, setTicker] = useState("");
  const [ativo, setAtivo] = useState<AtivoCadastro | null>(null);
  const [modeloId, setModeloId] = useState<string>("");
  const [nome, setNome] = useState("");
  const [salvando, setSalvando] = useState(false);

  const carregar = async () => {
    setCarregando(true);
    try {
      const { data } = await api.get<AtivoAvaliado[]>("/checklists/resumo");
      setAtivos(data ?? []);
    } catch {
      toast.error("Erro ao carregar as avaliações");
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    carregar();
    api.get("/checklist-modelos/all?page=0&size=100")
      .then(r => setModelos(r.data.content ?? []))
      .catch(() => setModelos([]));
  }, []);

  const abrirNovo = () => {
    setTicker(""); setAtivo(null); setModeloId(""); setNome("");
    setShowModal(true);
  };

  const criar = async () => {
    if (!ativo) {
      toast.error("Escolha o ativo (ticker) do catálogo.");
      return;
    }
    if (!modeloId && !nome.trim()) {
      toast.error("Dê um nome ao checklist ou escolha um modelo.");
      return;
    }

    setSalvando(true);
    try {
      await api.post("/checklists/create", {
        ativo_cadastro_id: ativo.id,
        ativo_id: null,
        nome: nome.trim() === "" ? null : nome.trim(),
        modelo_id: modeloId === "" ? null : Number(modeloId),
        peso: 1,
        ordem: null,
      });
      toast.success("Checklist criado!");
      setShowModal(false);
      navigate(`/avaliacao/${ativo.id}`);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao criar o checklist");
    } finally {
      setSalvando(false);
    }
  };

  const totalChecklists = ativos.reduce((s, a) => s + a.total_checklists, 0);
  const comScore = ativos.filter(a => a.quality_score != null);
  const mediaGeral = comScore.length > 0
    ? comScore.reduce((s, a) => s + (a.quality_score ?? 0), 0) / comScore.length
    : null;

  return (
    <Layout>
      <PlanejamentoNav ativo="avaliacao" />
      <PageHeader icon="✅" title="Avaliação de Ativos"
        subtitle="Seus checklists e notas por ativo — a metodologia e os critérios são definidos por você"
        actionLabel="Nova Avaliação" onAction={abrirNovo} />

      <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", marginBottom: "18px" }}>
        <button type="button" onClick={() => navigate("/avaliacao/modelos")} style={linkCardStyle}>
          🗂️ Modelos de checklist →
        </button>
        {!carregando && (
          <span style={{ fontSize: "12px", color: "#64748b", alignSelf: "center" }}>
            {ativos.length} ativo(s) · {totalChecklists} checklist(s)
            {mediaGeral != null && ` · média geral ${mediaGeral.toFixed(1)}%`}
          </span>
        )}
      </div>

      {carregando ? <Spinner text="Carregando avaliações..." /> : ativos.length === 0 ? (
        <EmptyState icon="✅" title="Nenhum ativo avaliado"
          text="Crie um checklist para um ativo do catálogo e comece a registrar a sua avaliação."
          actionLabel="Nova Avaliação" onAction={abrirNovo} />
      ) : (
        <CardGrid>
          {ativos.map(a => {
            const pct = a.total_perguntas > 0 ? (a.total_respondidas / a.total_perguntas) * 100 : 0;
            const clicavel = a.ativo_cadastro_id != null;
            return (
              <Card key={refDoAtivo(a)} accent="#8b5cf6"
                onClick={clicavel ? () => navigate(`/avaliacao/${refDoAtivo(a)}`) : undefined}
                style={{ cursor: clicavel ? "pointer" : "default" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", gap: "8px", marginBottom: "10px" }}>
                  <h3 style={{ fontSize: "17px", fontWeight: 800, color: "#0f172a", margin: 0 }}>
                    {a.ticker ?? "Ativo"}
                  </h3>
                  <ScoreBadge score={a.quality_score} />
                </div>

                <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginBottom: "12px" }}>
                  <Badge color="#8b5cf6" bg="#f5f3ff">{a.total_checklists} checklist(s)</Badge>
                  <Badge color="#64748b" bg="#f1f5f9">{a.total_respondidas}/{a.total_perguntas} respondidas</Badge>
                  {!clicavel && <Badge color="#0369a1" bg="#e0f2fe">posição #{a.ativo_id}</Badge>}
                </div>

                <div style={{ marginBottom: "6px" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "4px" }}>
                    <span style={{ fontSize: "11px", fontWeight: 600, color: "#64748b" }}>Preenchimento</span>
                    <span style={{ fontSize: "11px", fontWeight: 700, color: "#475569" }}>{pct.toFixed(0)}%</span>
                  </div>
                  <ProgressBar value={pct} color="#8b5cf6" />
                </div>

                {clicavel && (
                  <p style={{ fontSize: "11px", color: "#6366f1", fontWeight: 600, margin: "10px 0 0" }}>
                    Abrir avaliação →
                  </p>
                )}
              </Card>
            );
          })}
        </CardGrid>
      )}

      <Modal open={showModal} onClose={() => setShowModal(false)} title="Nova Avaliação"
        onSubmit={criar} submitLabel={salvando ? "Criando..." : "Criar checklist"}>
        <div>
          <label style={miniLabel}>Ativo (ticker do catálogo)</label>
          <AtivoAutocomplete value={ticker} onSelect={setAtivo} />
        </div>

        <Select label="Modelo de checklist (opcional)" value={modeloId}
          hint="Escolha um modelo para já vir com as perguntas, ou deixe em branco para montar do zero."
          onChange={e => setModeloId(e.target.value)}>
          <option value="">— Em branco —</option>
          {modelos.map(m => (
            <option key={m.id} value={m.id}>{m.nome} ({m.total_perguntas} perguntas)</option>
          ))}
        </Select>

        <Input label="Nome do checklist" value={nome}
          hint={modeloId ? "Opcional quando há modelo (o nome do modelo é usado)." : "Obrigatório quando o checklist é criado em branco."}
          onChange={e => setNome(e.target.value)}
          placeholder="Ex: Fundamentos" />
      </Modal>
    </Layout>
  );
}

const linkCardStyle: React.CSSProperties = {
  background: "white", border: "1.5px solid #e2e8f0", borderRadius: "10px",
  padding: "8px 14px", fontSize: "12px", fontWeight: 600, color: "#6366f1", cursor: "pointer",
};
