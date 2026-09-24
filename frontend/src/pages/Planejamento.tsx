import { useNavigate } from "react-router-dom";
import Layout from "../components/Layout";
import { PageHeader, CardGrid } from "../components/UI";
import { Card, Badge } from "../components/Shared";
import PlanejamentoNav from "../components/PlanejamentoNav";

/* ══════════════════════════════════════════════════════════════════════
   Hub do módulo de Planejamento.
   As fases ainda não implementadas aparecem marcadas como "Em breve" —
   o objetivo é o usuário saber para onde o módulo vai, sem prometer o que
   já não está pronto.
   ══════════════════════════════════════════════════════════════════════ */

interface Modulo {
  icon: string;
  titulo: string;
  descricao: string;
  path: string | null;
  cor: string;
  bg: string;
  pronto: boolean;
}

const modulos: Modulo[] = [
  {
    icon: "🎯",
    titulo: "Carteira Ideal",
    descricao: "Defina classes, subclasses e metas de ativos e compare com a sua carteira atual.",
    path: "/planejamento/carteira-ideal",
    cor: "#6366f1",
    bg: "#eef2ff",
    pronto: true,
  },
  {
    icon: "✅",
    titulo: "Avaliação de Ativos",
    descricao: "Monte seus próprios checklists e perguntas, atribua notas e acompanhe a evolução.",
    path: "/avaliacao",
    cor: "#8b5cf6",
    bg: "#f5f3ff",
    pronto: true,
  },
  {
    icon: "💸",
    titulo: "Próximos Aportes",
    descricao: "Priorize os aportes combinando qualidade, distância da meta e prioridade manual.",
    path: "/planejamento/aportes",
    cor: "#10b981",
    bg: "#ecfdf5",
    pronto: true,
  },
  {
    icon: "⚙️",
    titulo: "Pontuação",
    descricao: "Ajuste os pesos do Quality Score e do Priority Score da sua metodologia.",
    path: "/planejamento/pontuacao",
    cor: "#f59e0b",
    bg: "#fffbeb",
    pronto: true,
  },
];

export default function Planejamento() {
  const navigate = useNavigate();

  return (
    <Layout>
      <PlanejamentoNav ativo="visao" />
      <PageHeader
        icon="📐"
        title="Planejamento"
        subtitle="Sua metodologia de investimentos: metas, avaliações e prioridades de aporte"
      />
      <CardGrid>
        {modulos.map(m => (
          <Card key={m.titulo} accent={m.cor} onClick={m.pronto && m.path ? () => navigate(m.path as string) : undefined}
            style={{ opacity: m.pronto ? 1 : 0.65, cursor: m.pronto ? "pointer" : "default" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "12px" }}>
              <div style={{
                width: "44px", height: "44px", borderRadius: "10px", background: m.bg,
                display: "flex", alignItems: "center", justifyContent: "center", fontSize: "22px",
              }}>{m.icon}</div>
              {!m.pronto && <Badge color="#64748b" bg="#f1f5f9">Em breve</Badge>}
            </div>
            <h3 style={{ fontSize: "17px", fontWeight: 700, color: "#0f172a", marginBottom: "4px" }}>{m.titulo}</h3>
            <p style={{ fontSize: "13px", color: "#64748b", lineHeight: 1.5, margin: 0 }}>{m.descricao}</p>
          </Card>
        ))}
      </CardGrid>
    </Layout>
  );
}
