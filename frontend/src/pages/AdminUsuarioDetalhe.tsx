import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { isAxiosError } from "axios";
import api from "../api/api";
import Layout from "../components/Layout";
import { PageHeader, Spinner, EmptyState, ErrorState } from "../components/UI";

/* Resposta de GET /admin/users/{id} (AdminUserDetailDTO no backend).
   Só exibimos os campos cadastrais permitidos + o resumo de contagens —
   credenciais nunca são serializadas pelo backend (garantia estrutural). */
interface ResumoDTO {
  financas: {
    carteirasInvestimento: number;
    carteirasDividas: number;
    ativos: number;
    despesas: number;
    operacoesCompraVenda: number;
    precosAlvo: number;
  };
  planejamento: {
    estrategias: number;
    metasDeAtivos: number;
    checklistsModelo: number;
    checklistsAtivo: number;
    aporteConfigurado: boolean;
    aportesRegistrados: number;
    registrosHistorico: number;
  };
  estudos: { materias: number; estudos: number; sessoesEstudo: number };
  tarefas: number;
  treino: { treinos: number; sessoes: number };
}

interface AdminUserDetail {
  id: number;
  nome: string;
  email: string;
  role: string;
  ativo: boolean;
  resumo: ResumoDTO;
}

/* Módulos administrativos com tela de registros implementada. O resumo não
   traz a contagem de tb_metas (metasDeAtivos é de outro recurso), então o
   card de Metas aparece sem número. */
const moduloBadge = (chave: string) =>
  chave === "tarefas"
    ? { background: "var(--color-info-light)", color: "var(--color-info)" }
    : chave === "metas"
      ? { background: "var(--color-warning-light)", color: "var(--color-warning)" }
      : { background: "var(--color-primary-light)", color: "var(--color-primary)" };

export default function AdminUsuarioDetalhe() {
  const { id } = useParams();
  const usuarioId = Number(id);
  const navigate = useNavigate();
  const [usuario, setUsuario] = useState<AdminUserDetail | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<"geral" | "nao_encontrado" | null>(null);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    let ativo = true;
    (async () => {
      setCarregando(true);
      setErro(null);
      try {
        const res = await api.get(`/admin/users/${usuarioId}`);
        if (ativo) setUsuario(res.data);
      } catch (e) {
        if (ativo) setErro(isAxiosError(e) && e.response?.status === 404 ? "nao_encontrado" : "geral");
      } finally {
        if (ativo) setCarregando(false);
      }
    })();
    return () => { ativo = false; };
  }, [usuarioId, tentativa]);

  const voltarParaLista = () => navigate("/admin");

  const modulos = usuario
    ? [
        { chave: "tarefas", icone: "✓", nome: "Tarefas", descricao: "Tarefas cadastradas", contagem: usuario.resumo.tarefas },
        { chave: "metas", icone: "🎯", nome: "Metas", descricao: "Metas cadastradas", contagem: null },
        { chave: "estudos", icone: "📖", nome: "Estudos", descricao: "Registros de estudo", contagem: usuario.resumo.estudos.estudos },
      ]
    : [];

  return (
    <Layout>
      <button type="button" onClick={voltarParaLista}
        style={{ background: "none", border: "none", color: "var(--color-primary)", fontSize: "13px", fontWeight: 600, cursor: "pointer", marginBottom: "10px", padding: 0 }}>
        ← Todos os usuários
      </button>

      {carregando ? (
        <Spinner text="Carregando detalhes do usuário..." />
      ) : erro === "nao_encontrado" ? (
        <EmptyState icon="🔍" title="Usuário não encontrado" text="O usuário consultado não existe ou foi removido." actionLabel="Voltar à lista de usuários" onAction={voltarParaLista} />
      ) : erro ? (
        <ErrorState text="Não foi possível carregar os detalhes deste usuário." onRetry={() => setTentativa((n) => n + 1)} />
      ) : usuario && (
        <>
          <PageHeader icon="👤" title={usuario.nome} subtitle={usuario.email} />

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "24px" }}>
            <span style={badge(usuario.role === "ADMIN" ? "var(--color-warning-light)" : "var(--color-info-light)", usuario.role === "ADMIN" ? "var(--color-warning)" : "var(--color-info)")}>
              {usuario.role === "ADMIN" ? "🛡️ Admin" : "Usuário"}
            </span>
            <span style={badge(usuario.ativo ? "var(--color-success-light)" : "var(--color-danger-light)", usuario.ativo ? "var(--color-success)" : "var(--color-danger)")}>
              {usuario.ativo ? "✓ Ativo" : "✕ Inativo"}
            </span>
            <span style={badge("var(--color-bg)", "var(--color-text-secondary)")}>ID #{usuario.id}</span>
          </div>

          <h2 style={secaoTitulo}>Módulos administrativos</h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(min(100%, 240px), 1fr))", gap: "12px", marginBottom: "28px" }}>
            {modulos.map((m) => (
              <button key={m.chave} type="button" onClick={() => navigate(`/admin/usuarios/${usuario.id}/${m.chave}`)}
                style={{ display: "flex", alignItems: "center", gap: "12px", textAlign: "left", background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "var(--radius-lg)", padding: "14px 16px", cursor: "pointer", boxShadow: "var(--shadow-sm)", transition: "all var(--transition-fast)" }}
                onMouseEnter={(e) => { e.currentTarget.style.boxShadow = "var(--shadow-md)"; e.currentTarget.style.transform = "translateY(-1px)"; }}
                onMouseLeave={(e) => { e.currentTarget.style.boxShadow = "var(--shadow-sm)"; e.currentTarget.style.transform = "translateY(0)"; }}>
                <span aria-hidden="true" style={{ width: "38px", height: "38px", borderRadius: "10px", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "18px", flexShrink: 0, ...moduloBadge(m.chave) }}>{m.icone}</span>
                <span style={{ flex: 1, minWidth: 0 }}>
                  <span style={{ display: "block", fontSize: "14px", fontWeight: 700, color: "var(--color-text)" }}>{m.nome}</span>
                  <span style={{ display: "block", fontSize: "12px", color: "var(--color-text-secondary)" }}>{m.descricao}</span>
                </span>
                {m.contagem !== null && (
                  <span style={{ fontSize: "12px", fontWeight: 700, padding: "3px 10px", borderRadius: "var(--radius-full)", background: "var(--color-bg)", color: "var(--color-text-secondary)" }}>{m.contagem}</span>
                )}
                <span aria-hidden="true" style={{ color: "var(--color-primary)", fontWeight: 700, fontSize: "16px" }}>›</span>
              </button>
            ))}
          </div>

          <h2 style={secaoTitulo}>Resumo de dados</h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(min(100%, 280px), 1fr))", gap: "12px" }}>
            <GrupoResumo titulo="💰 Finanças" itens={[
              { label: "Carteiras de investimento", valor: usuario.resumo.financas.carteirasInvestimento },
              { label: "Carteiras de dívidas", valor: usuario.resumo.financas.carteirasDividas },
              { label: "Ativos", valor: usuario.resumo.financas.ativos },
              { label: "Despesas", valor: usuario.resumo.financas.despesas },
              { label: "Operações compra/venda", valor: usuario.resumo.financas.operacoesCompraVenda },
              { label: "Preços-alvo", valor: usuario.resumo.financas.precosAlvo },
            ]} />
            <GrupoResumo titulo="📐 Planejamento" itens={[
              { label: "Estratégias", valor: usuario.resumo.planejamento.estrategias },
              { label: "Metas de ativos", valor: usuario.resumo.planejamento.metasDeAtivos },
              { label: "Checklists modelo", valor: usuario.resumo.planejamento.checklistsModelo },
              { label: "Checklists de ativo", valor: usuario.resumo.planejamento.checklistsAtivo },
              { label: "Aporte configurado", valor: usuario.resumo.planejamento.aporteConfigurado ? "Sim" : "Não" },
              { label: "Aportes registrados", valor: usuario.resumo.planejamento.aportesRegistrados },
              { label: "Registros de histórico", valor: usuario.resumo.planejamento.registrosHistorico },
            ]} />
            <GrupoResumo titulo="📖 Estudos" itens={[
              { label: "Matérias", valor: usuario.resumo.estudos.materias },
              { label: "Registros de estudo", valor: usuario.resumo.estudos.estudos },
              { label: "Sessões de estudo", valor: usuario.resumo.estudos.sessoesEstudo },
            ]} />
            <GrupoResumo titulo="✓ Tarefas" itens={[
              { label: "Tarefas", valor: usuario.resumo.tarefas },
            ]} />
            <GrupoResumo titulo="💪 Treino" itens={[
              { label: "Treinos", valor: usuario.resumo.treino.treinos },
              { label: "Sessões de treino", valor: usuario.resumo.treino.sessoes },
            ]} />
          </div>
        </>
      )}
    </Layout>
  );
}

/* ── Componentes locais ── */

const secaoTitulo: React.CSSProperties = { fontSize: "15px", fontWeight: 700, color: "var(--color-text)", marginBottom: "12px" };

const badge = (background: string, color: string): React.CSSProperties => ({
  display: "inline-flex", alignItems: "center", gap: "4px",
  padding: "4px 12px", borderRadius: "var(--radius-full)",
  fontSize: "12px", fontWeight: 700, background, color,
});

function GrupoResumo({ titulo, itens }: { titulo: string; itens: { label: string; valor: string | number }[] }) {
  return (
    <section style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "var(--radius-lg)", padding: "16px 18px", boxShadow: "var(--shadow-sm)" }}>
      <h3 style={{ fontSize: "13px", fontWeight: 700, color: "var(--color-text)", marginBottom: "10px" }}>{titulo}</h3>
      <ul style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "6px" }}>
        {itens.map((i) => (
          <li key={i.label} style={{ display: "flex", justifyContent: "space-between", gap: "10px", fontSize: "13px" }}>
            <span style={{ color: "var(--color-text-secondary)" }}>{i.label}</span>
            <span style={{ fontWeight: 700, color: "var(--color-text)", fontVariantNumeric: "tabular-nums" }}>{i.valor}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}
