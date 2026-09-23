import { useNavigate } from "react-router-dom";

/* ══════════════════════════════════════════════════════════════════════
   Sub-navegação do módulo de Planejamento.

   O módulo tem 6 telas; sem isso o usuário precisa voltar ao hub a cada troca.
   Fica no topo das telas do módulo, mostrando sempre onde ele está.
   ══════════════════════════════════════════════════════════════════════ */

interface Aba {
  id: string;
  label: string;
  icon: string;
  path: string;
}

const ABAS: Aba[] = [
  { id: "visao", label: "Visão geral", icon: "📐", path: "/planejamento" },
  { id: "carteira-ideal", label: "Carteira Ideal", icon: "🎯", path: "/planejamento/carteira-ideal" },
  { id: "avaliacao", label: "Avaliação", icon: "✅", path: "/avaliacao" },
  { id: "modelos", label: "Modelos", icon: "🗂️", path: "/avaliacao/modelos" },
  { id: "aportes", label: "Próximos Aportes", icon: "💸", path: "/planejamento/aportes" },
  { id: "pontuacao", label: "Pontuação", icon: "⚙️", path: "/planejamento/pontuacao" },
];

export default function PlanejamentoNav({ ativo }: { ativo?: string }) {
  const navigate = useNavigate();

  return (
    <nav aria-label="Navegação do Planejamento"
      style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginBottom: "16px", paddingBottom: "12px", borderBottom: "1px solid #e2e8f0" }}>
      {ABAS.map(aba => {
        const selecionada = aba.id === ativo;
        return (
          <button key={aba.id} type="button" onClick={() => navigate(aba.path)}
            aria-current={selecionada ? "page" : undefined}
            style={{
              display: "inline-flex", alignItems: "center", gap: "6px",
              padding: "7px 14px", borderRadius: "9999px", cursor: "pointer",
              fontSize: "12px", fontWeight: 700, whiteSpace: "nowrap",
              border: selecionada ? "1.5px solid #6366f1" : "1.5px solid #e2e8f0",
              background: selecionada ? "#eef2ff" : "white",
              color: selecionada ? "#4338ca" : "#64748b",
              transition: "all 0.15s ease",
            }}
            onMouseEnter={e => { if (!selecionada) { e.currentTarget.style.background = "#f8fafc"; e.currentTarget.style.color = "#334155"; } }}
            onMouseLeave={e => { if (!selecionada) { e.currentTarget.style.background = "white"; e.currentTarget.style.color = "#64748b"; } }}
          >
            <span aria-hidden="true">{aba.icon}</span>
            {aba.label}
          </button>
        );
      })}
    </nav>
  );
}
