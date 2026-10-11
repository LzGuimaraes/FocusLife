import { useNavigate } from "react-router-dom";
import Layout from "../components/Layout";
import { PageHeader, CardGrid } from "../components/UI";
import { Card } from "../components/Shared";
import DiagnosticoFinanceiroBanner from "../components/DiagnosticoFinanceiroBanner";

/* ══════════════════════════════════════════════════════════════════════
   Hub de FINANÇAS.

   `/financas` deixou de ser a lista de carteiras e passou a ser o ponto de
   entrada do módulo financeiro, com três destinos:
     • Compras/Vendas — lucro/prejuízo por operação;
     • Organização   — o antigo módulo de Planejamento (metas, avaliação,
                        aportes), que agora nasce DENTRO de Finanças;
     • Carteiras     — a lista de carteiras que antes vivia aqui.

   As rotas antigas (`/planejamento/*`, `/avaliacao/*`) continuam vivas: o
   card "Organização" aponta para o hub de Planejamento sem URL nova, para não
   quebrar os links espalhados (botão 🎯 das carteiras, widget do Dashboard).
   ══════════════════════════════════════════════════════════════════════ */

interface Destino {
  icon: string;
  titulo: string;
  descricao: string;
  path: string;
  cor: string;
  bg: string;
}

const destinos: Destino[] = [
  {
    icon: "🔄",
    titulo: "Compras/Vendas",
    descricao: "Registre a compra e a venda de um ativo e veja o lucro/prejuízo em R$, a rentabilidade %, o andamento e o preço teto/mínimo.",
    path: "/financas/compras-vendas",
    cor: "#0ea5e9",
    bg: "#f0f9ff",
  },
  {
    icon: "📐",
    titulo: "Organização",
    descricao: "Seu módulo de planejamento: carteira ideal, avaliação de ativos, notas por subclasse e próximos aportes.",
    path: "/planejamento",
    cor: "#6366f1",
    bg: "#eef2ff",
  },
  {
    icon: "💼",
    titulo: "Carteiras",
    descricao: "Suas carteiras de investimento e despesas, com posições, lançamentos e saldos.",
    path: "/financas/carteiras",
    cor: "#10b981",
    bg: "#ecfdf5",
  },
];

export default function Financas() {
  const navigate = useNavigate();

  return (
    <Layout>
      <PageHeader icon="💰" title="Finanças" subtitle="Investimentos, operações de compra/venda e organização da sua carteira" />

      {/* Diagnóstico de dados legados: avisa (e permite reparar) posições que
          ficaram sem carteira na migração que dividiu as carteiras. */}
      <DiagnosticoFinanceiroBanner />

      <CardGrid>
        {destinos.map(d => (
          <Card key={d.titulo} accent={d.cor} onClick={() => navigate(d.path)}>
            <div style={{
              width: "46px", height: "46px", borderRadius: "12px", background: d.bg,
              display: "flex", alignItems: "center", justifyContent: "center",
              fontSize: "23px", marginBottom: "14px",
            }}>{d.icon}</div>
            <h3 style={{ fontSize: "17px", fontWeight: 700, color: "#0f172a", marginBottom: "6px" }}>{d.titulo}</h3>
            <p style={{ fontSize: "13px", color: "#64748b", lineHeight: 1.5, margin: 0 }}>{d.descricao}</p>
          </Card>
        ))}
      </CardGrid>
    </Layout>
  );
}
