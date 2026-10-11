import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { isAxiosError } from "axios";
import api from "../api/api";
import Layout from "../components/Layout";
import { Select, DateInput } from "../components/Form";
import { Button } from "../components/Shared";
import { PageHeader, Spinner, EmptyState, ErrorState, Pagination, StatusBadge, PrioridadeBadge, ProgressBar } from "../components/UI";
import { parseLocalDate } from "../utils/date";
import { fmtUnidades } from "../utils/numeros";
import { categoriaDespesaInfo, type CategoriaDespesa } from "../types/despesas";

/* Consultas ADMIN de leitura dos registros de UM usuário-alvo
   (GET /admin/users/{usuarioId}/{modulo}). O backend é quem garante o escopo:
   o id vem do caminho da URL e é aplicado como filtro obrigatório. */

type Modulo = "tarefas" | "metas" | "estudos" | "investimentos" | "contas";

interface TarefaAdmin { id: number; titulo: string; status: string; prioridade: string; prazo: string | null; horario: string | null; }
interface MetaAdmin { id: number; titulo: string; descricao: string | null; prograsso: number; prazo: string | null; status: string; }
interface EstudoAdmin { id: number; nome: string; duracao_min: number; data: string | null; notas: string | null; materia_id: number | null; }
interface InvestimentoAdmin { id: number; nome: string; categoriaInvestimento: string | null; quantidade: number | null; valorUnitario: number | null; precoAtual: number | null; saldo: number; instituicao: string | null; dataAplicacao: string | null; vencimento: string | null; dataVencimento: string | null; rentabilidade: number | null; carteiraNome: string | null; carteiraMoeda: string | null; }
interface ContaAdmin { id: number; nome: string; saldo: number; dataVencimento: string | null; pago: boolean | null; tipo: string; categoria: CategoriaDespesa | null; carteiraNome: string | null; carteiraMoeda: string | null; }
type Registro = TarefaAdmin | MetaAdmin | EstudoAdmin | InvestimentoAdmin | ContaAdmin;

interface UsuarioResumo { id: number; nome: string; email: string; }

const TAMANHO_PAGINA = 10;

/* Rótulos/cores das classes de investimento — mesmos do formulário de posições
   (CarteiraDetalhe/Contas), aqui só para exibir o badge e o filtro. */
const catInvestInfo: Record<string, { icon: string; label: string; color: string }> = {
  RENDA_FIXA: { icon: "📊", label: "Renda Fixa", color: "#3b82f6" },
  TESOURO_DIRETO: { icon: "🏛️", label: "Tesouro Direto", color: "#10b981" },
  ACOES: { icon: "📈", label: "Ações", color: "#6366f1" },
  FIIS: { icon: "🏢", label: "FIIs", color: "#8b5cf6" },
  ETFS: { icon: "📦", label: "ETFs", color: "#06b6d4" },
  CRIPTOMOEDAS: { icon: "₿", label: "Criptomoedas", color: "#f59e0b" },
  OUTROS: { icon: "📦", label: "Outros", color: "#64748b" },
};

/** Classes oferecidas no filtro: as mesmas do cadastro de posições (sem "Outros"). */
const classesInvestimento = ["RENDA_FIXA", "TESOURO_DIRETO", "ACOES", "FIIS", "ETFS", "CRIPTOMOEDAS"] as const;

const moedaSimbolo: Record<string, string> = { BRL: "R$", USD: "$", EUR: "€", GBP: "£", JPY: "¥" };
const fmtMoeda = (v: number | null | undefined, m: string | null) => {
  if (v == null) return "—";
  const simbolo = m ? (moedaSimbolo[m] || m) : "R$";
  return `${simbolo} ${v.toFixed(2)}`;
};

const MODULO_INFO: Record<Modulo, { icone: string; titulo: string; tituloVazio: string; vazio: string; plural: string }> = {
  tarefas: { icone: "✓", titulo: "Tarefas", tituloVazio: "Nenhuma tarefa", vazio: "Este usuário ainda não possui tarefas cadastradas.", plural: "tarefas" },
  metas: { icone: "🎯", titulo: "Metas", tituloVazio: "Nenhuma meta", vazio: "Este usuário ainda não possui metas cadastradas.", plural: "metas" },
  estudos: { icone: "📖", titulo: "Estudos", tituloVazio: "Nenhum registro de estudo", vazio: "Este usuário ainda não possui registros de estudo.", plural: "registros de estudo" },
  investimentos: { icone: "📈", titulo: "Investimentos", tituloVazio: "Nenhum investimento", vazio: "Este usuário ainda não possui investimentos nas carteiras.", plural: "investimentos" },
  contas: { icone: "🧾", titulo: "Contas", tituloVazio: "Nenhum lançamento", vazio: "Este usuário ainda não possui contas ou saldos nas carteiras de dívidas.", plural: "contas" },
};

const ABAS: { id: Modulo; label: string; icon: string }[] = [
  { id: "tarefas", label: "Tarefas", icon: "✓" },
  { id: "metas", label: "Metas", icon: "🎯" },
  { id: "estudos", label: "Estudos", icon: "📖" },
  { id: "investimentos", label: "Investimentos", icon: "📈" },
  { id: "contas", label: "Contas", icon: "🧾" },
];

const fmtData = (d: string | null) => (d ? parseLocalDate(d).toLocaleDateString("pt-BR") : "—");
const fmtDuracao = (m: number) => (m < 60 ? `${m}min` : `${Math.floor(m / 60)}h ${m % 60}min`);

export default function AdminUsuarioRegistros({ modulo }: { modulo: Modulo }) {
  const { id } = useParams();
  const usuarioId = Number(id);
  const navigate = useNavigate();
  const info = MODULO_INFO[modulo];

  const [usuario, setUsuario] = useState<UsuarioResumo | null>(null);
  const [registros, setRegistros] = useState<Registro[]>([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalItens, setTotalItens] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<"geral" | "nao_encontrado" | null>(null);
  const [tentativa, setTentativa] = useState(0);

  /* Filtros (só os suportados pelo endpoint de cada módulo). */
  const [filtroStatus, setFiltroStatus] = useState("");
  const [filtroPrioridade, setFiltroPrioridade] = useState("");
  const [de, setDe] = useState("");
  const [ate, setAte] = useState("");
  const [filtroClasse, setFiltroClasse] = useState("");
  const [filtroTipo, setFiltroTipo] = useState("");
  const [filtroPago, setFiltroPago] = useState("");

  const temFiltro =
    modulo === "estudos"
      ? de !== "" || ate !== ""
      : modulo === "investimentos"
        ? filtroClasse !== ""
        : modulo === "contas"
          ? filtroTipo !== "" || filtroPago !== ""
          : filtroStatus !== "" || (modulo === "tarefas" && filtroPrioridade !== "");

  /* Nome/e-mail do usuário para o cabeçalho (best effort: se falhar, mostramos
     só o id — os dados de verdade vêm da consulta de registros). */
  useEffect(() => {
    let ativo = true;
    api.get(`/admin/users/${usuarioId}`)
      .then((res) => { if (ativo) setUsuario(res.data); })
      .catch(() => { /* cabeçalho cai no fallback com o id */ });
    return () => { ativo = false; };
  }, [usuarioId]);

  useEffect(() => {
    let ativo = true;
    (async () => {
      setCarregando(true);
      setErro(null);
      try {
        const params: Record<string, string | number> = { page: pagina, size: TAMANHO_PAGINA };
        if (modulo === "tarefas") {
          if (filtroStatus) params.status = filtroStatus;
          if (filtroPrioridade) params.prioridade = filtroPrioridade;
        } else if (modulo === "metas") {
          if (filtroStatus) params.status = filtroStatus;
        } else if (modulo === "estudos") {
          if (de) params.de = de;
          if (ate) params.ate = ate;
        } else if (modulo === "investimentos") {
          if (filtroClasse) params.categoria = filtroClasse;
        } else {
          if (filtroTipo) params.tipo = filtroTipo;
          if (filtroPago) params.pago = filtroPago;
        }
        const res = await api.get(`/admin/users/${usuarioId}/${modulo}`, { params });
        if (!ativo) return;
        setRegistros(res.data?.pagina?.content ?? []);
        setTotalPaginas(res.data?.pagina?.totalPages ?? 0);
        setTotalItens(res.data?.pagina?.totalElements ?? 0);
      } catch (e) {
        if (!ativo) return;
        setErro(isAxiosError(e) && e.response?.status === 404 ? "nao_encontrado" : "geral");
      } finally {
        if (ativo) setCarregando(false);
      }
    })();
    return () => { ativo = false; };
  }, [usuarioId, modulo, pagina, filtroStatus, filtroPrioridade, de, ate, filtroClasse, filtroTipo, filtroPago, tentativa]);

  const alterarStatus = (v: string) => { setFiltroStatus(v); setPagina(0); };
  const alterarPrioridade = (v: string) => { setFiltroPrioridade(v); setPagina(0); };
  const alterarDe = (v: string) => { setDe(v); setPagina(0); };
  const alterarAte = (v: string) => { setAte(v); setPagina(0); };
  const alterarClasse = (v: string) => { setFiltroClasse(v); setPagina(0); };
  const alterarTipo = (v: string) => { setFiltroTipo(v); setPagina(0); };
  const alterarPago = (v: string) => { setFiltroPago(v); setPagina(0); };
  const limparFiltros = () => { setFiltroStatus(""); setFiltroPrioridade(""); setDe(""); setAte(""); setFiltroClasse(""); setFiltroTipo(""); setFiltroPago(""); setPagina(0); };

  return (
    <Layout>
      <button type="button" onClick={() => navigate(`/admin/usuarios/${usuarioId}`)}
        style={{ background: "none", border: "none", color: "var(--color-primary)", fontSize: "13px", fontWeight: 600, cursor: "pointer", marginBottom: "10px", padding: 0 }}>
        ← Voltar ao usuário
      </button>

      <PageHeader
        icon={info.icone}
        title={`${info.titulo} do usuário`}
        subtitle={usuario ? `${usuario.nome} · ${usuario.email}` : `Usuário #${usuarioId}`}
      />

      <nav aria-label="Módulos administrativos do usuário" style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginBottom: "16px", paddingBottom: "12px", borderBottom: "1px solid var(--border)" }}>
        {ABAS.map((aba) => {
          const selecionada = aba.id === modulo;
          return (
            <button key={aba.id} type="button" onClick={() => navigate(`/admin/usuarios/${usuarioId}/${aba.id}`)}
              aria-current={selecionada ? "page" : undefined}
              style={{
                display: "inline-flex", alignItems: "center", gap: "6px",
                padding: "7px 14px", borderRadius: "var(--radius-full)", cursor: "pointer",
                fontSize: "12px", fontWeight: 700, whiteSpace: "nowrap",
                border: selecionada ? "1.5px solid var(--color-primary)" : "1.5px solid var(--border)",
                background: selecionada ? "var(--color-primary-light)" : "var(--surface)",
                color: selecionada ? "var(--color-primary)" : "var(--color-text-secondary)",
                transition: "all var(--transition-fast)",
              }}>
              <span aria-hidden="true">{aba.icon}</span>
              {aba.label}
            </button>
          );
        })}
      </nav>

      <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", alignItems: "flex-end", marginBottom: "20px" }}>
        {modulo === "tarefas" && (
          <>
            <div style={{ width: "180px" }}>
              <Select label="Status" value={filtroStatus} onChange={(e) => alterarStatus(e.target.value)}>
                <option value="">Todos</option>
                <option value="PENDENTE">Pendente</option>
                <option value="Ativa">Ativa</option>
                <option value="Concluida">Concluída</option>
                <option value="Cancelada">Cancelada</option>
              </Select>
            </div>
            <div style={{ width: "180px" }}>
              <Select label="Prioridade" value={filtroPrioridade} onChange={(e) => alterarPrioridade(e.target.value)}>
                <option value="">Todas</option>
                <option value="baixa">🔵 Baixa</option>
                <option value="media">🟡 Média</option>
                <option value="alta">🔴 Alta</option>
              </Select>
            </div>
          </>
        )}
        {modulo === "metas" && (
          <div style={{ width: "180px" }}>
            <Select label="Status" value={filtroStatus} onChange={(e) => alterarStatus(e.target.value)}>
              <option value="">Todos</option>
              <option value="PENDENTE">Pendente</option>
              <option value="Ativa">Ativa</option>
              <option value="Concluida">Concluída</option>
              <option value="Cancelada">Cancelada</option>
            </Select>
          </div>
        )}
        {modulo === "estudos" && (
          <>
            <div style={{ width: "180px" }}>
              <DateInput label="De" value={de} onChange={(e) => alterarDe(e.target.value)} />
            </div>
            <div style={{ width: "180px" }}>
              <DateInput label="Até" value={ate} onChange={(e) => alterarAte(e.target.value)} />
            </div>
          </>
        )}
        {modulo === "investimentos" && (
          <div style={{ width: "200px" }}>
            <Select label="Classe" value={filtroClasse} onChange={(e) => alterarClasse(e.target.value)}>
              <option value="">Todas</option>
              {classesInvestimento.map((c) => (
                <option key={c} value={c}>{catInvestInfo[c].icon} {catInvestInfo[c].label}</option>
              ))}
            </Select>
          </div>
        )}
        {modulo === "contas" && (
          <>
            <div style={{ width: "180px" }}>
              <Select label="Tipo" value={filtroTipo} onChange={(e) => alterarTipo(e.target.value)}>
                <option value="">Todos</option>
                <option value="DESPESA">💳 Contas</option>
                <option value="SALDO">💰 Saldos</option>
              </Select>
            </div>
            <div style={{ width: "180px" }}>
              <Select label="Pagamento" value={filtroPago} onChange={(e) => alterarPago(e.target.value)}>
                <option value="">Todos</option>
                <option value="true">✅ Pagas</option>
                <option value="false">⏳ Não pagas</option>
              </Select>
            </div>
          </>
        )}
        {temFiltro && <Button variant="ghost" size="sm" onClick={limparFiltros}>Limpar filtros</Button>}
      </div>

      {carregando ? (
        <Spinner text={`Carregando ${info.plural}...`} />
      ) : erro === "nao_encontrado" ? (
        <EmptyState icon="🔍" title="Usuário não encontrado" text="O usuário consultado não existe ou foi removido." actionLabel="Voltar à lista de usuários" onAction={() => navigate("/admin")} />
      ) : erro ? (
        <ErrorState text={`Não foi possível carregar as ${info.plural} deste usuário.`} onRetry={() => setTentativa((n) => n + 1)} />
      ) : registros.length === 0 ? (
        temFiltro ? (
          <EmptyState icon="🔍" title="Nenhum resultado" text="Nenhum registro corresponde aos filtros aplicados." actionLabel="Limpar filtros" onAction={limparFiltros} />
        ) : (
          <EmptyState icon={info.icone} title={info.tituloVazio} text={info.vazio} />
        )
      ) : (
        <>
          <p style={{ fontSize: "13px", color: "var(--color-text-secondary)", marginBottom: "12px" }}>
            {totalItens === 1 ? "1 registro" : `${totalItens} registros`}
            {temFiltro ? " (com filtros aplicados)" : ""}
          </p>
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {modulo === "tarefas" && (registros as TarefaAdmin[]).map((t) => (
              <div key={t.id} style={linhaStyle}>
                <div style={{ flex: 1, minWidth: "200px" }}>
                  <p style={linhaTitulo}>{t.titulo}</p>
                  <p style={linhaInfo}>📅 {fmtData(t.prazo)}{t.horario ? ` · 🕐 ${t.horario}` : ""} · ID #{t.id}</p>
                </div>
                <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", alignItems: "center" }}>
                  <PrioridadeBadge prioridade={t.prioridade} />
                  <StatusBadge status={t.status} />
                </div>
              </div>
            ))}
            {modulo === "metas" && (registros as MetaAdmin[]).map((m) => (
              <div key={m.id} style={linhaStyle}>
                <div style={{ flex: 1, minWidth: "200px" }}>
                  <p style={linhaTitulo}>{m.titulo}</p>
                  {m.descricao && <p style={{ ...linhaInfo, marginBottom: "8px" }}>{m.descricao}</p>}
                  <div style={{ maxWidth: "280px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "4px" }}>
                      <span style={{ fontSize: "12px", fontWeight: 600, color: "var(--color-text-secondary)" }}>Progresso</span>
                      <span style={{ fontSize: "12px", fontWeight: 700, color: (m.prograsso ?? 0) >= 80 ? "var(--color-success)" : (m.prograsso ?? 0) >= 50 ? "var(--color-warning)" : "var(--color-danger)" }}>{m.prograsso ?? 0}%</span>
                    </div>
                    <ProgressBar value={m.prograsso ?? 0} />
                  </div>
                </div>
                <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", alignItems: "center" }}>
                  <StatusBadge status={m.status} />
                  <span style={linhaInfo}>📅 {fmtData(m.prazo)}</span>
                  <span style={linhaInfo}>ID #{m.id}</span>
                </div>
              </div>
            ))}
            {modulo === "estudos" && (registros as EstudoAdmin[]).map((e) => (
              <div key={e.id} style={linhaStyle}>
                <div style={{ flex: 1, minWidth: "200px" }}>
                  <p style={linhaTitulo}>{e.nome}</p>
                  {e.notas && <p style={{ ...linhaInfo, fontStyle: "italic" }}>“{e.notas}”</p>}
                </div>
                <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", alignItems: "center" }}>
                  <span style={{ fontSize: "12px", fontWeight: 700, padding: "3px 10px", borderRadius: "var(--radius-full)", background: "var(--color-primary-light)", color: "var(--color-primary)" }}>⏱ {fmtDuracao(e.duracao_min)}</span>
                  <span style={linhaInfo}>📅 {fmtData(e.data)}</span>
                  <span style={linhaInfo}>ID #{e.id}</span>
                </div>
              </div>
            ))}
            {modulo === "investimentos" && (registros as InvestimentoAdmin[]).map((i) => {
              const ci = i.categoriaInvestimento ? catInvestInfo[i.categoriaInvestimento] : null;
              const temQtd = i.quantidade != null && i.valorUnitario != null;
              return (
                <div key={i.id} style={linhaStyle}>
                  <div style={{ flex: 1, minWidth: "220px" }}>
                    <p style={linhaTitulo}>{i.nome}</p>
                    <p style={linhaInfo}>
                      {temQtd ? `📦 ${fmtUnidades(i.quantidade!, Number.isInteger(i.quantidade!))} · Médio ${fmtMoeda(i.valorUnitario, i.carteiraMoeda)} · ` : ""}
                      Atual {fmtMoeda(i.precoAtual, i.carteiraMoeda)}
                      {i.instituicao ? ` · 🏦 ${i.instituicao}` : ""}
                    </p>
                    <p style={{ ...linhaInfo, marginTop: "4px" }}>
                      🗂 {i.carteiraNome ?? "—"}
                      {i.vencimento ? ` · ⏳ ${i.vencimento}` : i.dataVencimento ? ` · ⏳ vence ${fmtData(i.dataVencimento)}` : ""}
                      {` · ID #${i.id}`}
                    </p>
                  </div>
                  <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", alignItems: "center" }}>
                    {ci && <span style={{ fontSize: "12px", fontWeight: 600, padding: "3px 10px", borderRadius: "var(--radius-full)", background: `${ci.color}1f`, color: ci.color }}>{ci.icon} {ci.label}</span>}
                    <span style={{ fontSize: "14px", fontWeight: 800, color: "var(--color-success)", whiteSpace: "nowrap" }}>{fmtMoeda(i.saldo, i.carteiraMoeda)}</span>
                  </div>
                </div>
              );
            })}
            {modulo === "contas" && (registros as ContaAdmin[]).map((c) => {
              const isSaldo = c.tipo === "SALDO";
              const cd = c.categoria ? categoriaDespesaInfo[c.categoria] : null;
              return (
                <div key={c.id} style={linhaStyle}>
                  <div style={{ flex: 1, minWidth: "220px" }}>
                    <p style={linhaTitulo}>{c.nome}</p>
                    <p style={linhaInfo}>
                      {isSaldo ? "💰 Crédito disponível" : `📅 ${c.dataVencimento ? `vence ${fmtData(c.dataVencimento)}` : "sem vencimento"}`}
                      {` · 🗂 ${c.carteiraNome ?? "—"} · ID #${c.id}`}
                    </p>
                  </div>
                  <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", alignItems: "center" }}>
                    <span style={{ fontSize: "12px", fontWeight: 600, padding: "3px 10px", borderRadius: "var(--radius-full)", background: isSaldo ? "var(--color-success-light)" : "var(--color-info-light)", color: isSaldo ? "var(--color-success)" : "var(--color-info)" }}>{isSaldo ? "💰 Saldo" : "💳 Conta"}</span>
                    {!isSaldo && <span style={{ fontSize: "12px", fontWeight: 700, padding: "3px 10px", borderRadius: "var(--radius-full)", background: c.pago ? "var(--color-success-light)" : "var(--color-warning-light)", color: c.pago ? "var(--color-success)" : "var(--color-warning)" }}>{c.pago ? "✅ Pago" : "⏳ Pendente"}</span>}
                    {cd && <span style={{ fontSize: "12px", fontWeight: 600, padding: "3px 10px", borderRadius: "var(--radius-full)", background: `${cd.cor}1f`, color: cd.cor }}>{cd.icon} {cd.label}</span>}
                    <span style={{ fontSize: "14px", fontWeight: 800, whiteSpace: "nowrap", color: isSaldo || c.pago ? "var(--color-success)" : "var(--color-warning)" }}>{isSaldo ? "+" : ""}{fmtMoeda(c.saldo, c.carteiraMoeda)}</span>
                  </div>
                </div>
              );
            })}
          </div>
          <Pagination page={pagina} totalPages={totalPaginas} onChange={setPagina} ariaLabel={`Paginação de ${info.plural}`} />
        </>
      )}
    </Layout>
  );
}

/* ── Estilos locais ── */

const linhaStyle: React.CSSProperties = {
  background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "var(--radius-lg)",
  padding: "14px 18px", display: "flex", alignItems: "center", gap: "12px",
  flexWrap: "wrap", boxShadow: "var(--shadow-sm)",
};
const linhaTitulo: React.CSSProperties = { fontSize: "14px", fontWeight: 600, color: "var(--color-text)", margin: 0 };
const linhaInfo: React.CSSProperties = { fontSize: "12px", color: "var(--color-text-secondary)", margin: 0 };
