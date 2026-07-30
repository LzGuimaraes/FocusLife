import Layout from "../components/Layout";

const features = [
  {
    icon: "💰", title: "Carteiras Financeiras",
    color: "#10b981", bg: "#ecfdf5",
    desc: "Crie carteiras de Despesas e Investimentos com navegação contextual. Cada carteira tem sua própria página com indicadores exclusivos.",
    steps: [
      "Carteira de Despesas: contas mensais com checklist Pago/Pendente, totalizadores automáticos",
      "Carteira de Investimentos: patrimônio total, distribuição por categorias com gráfico de barras %",
      "Navegação por carteira: seletor dropdown para trocar rapidamente sem voltar à tela inicial",
      "Suporte a múltiplas moedas: BRL, USD, EUR, GBP, JPY com formatação automática",
    ],
  },
  {
    icon: "📈", title: "6 Categorias de Investimento",
    color: "#8b5cf6", bg: "#f5f3ff",
    desc: "Sistema escalável com formulários condicionais — cada categoria exibe apenas os campos relevantes.",
    steps: [
      "📊 Renda Fixa: instituição, data da aplicação, rentabilidade %, vencimento",
      "🏛️ Tesouro Direto: título, data da compra, vencimento, quantidade de títulos",
      "📈 Ações: ticker, empresa, preço médio × quantidade, preço atual (opcional)",
      "🏢 FIIs: ticker, quantidade de cotas, preço médio, preço atual",
      "📦 ETFs: ticker, quantidade, preço médio, preço atual",
      "₿ Criptomoedas: ativo (BTC, ETH, etc.), preço médio, quantidade com até 8 casas decimais, preço atual",
    ],
  },
  {
    icon: "📊", title: "Distribuição de Patrimônio",
    color: "#6366f1", bg: "#eef2ff",
    desc: "Visualize a composição da sua carteira de investimentos com gráficos de barra e percentuais.",
    steps: [
      "Gráfico de barras horizontal mostrando cada categoria de investimento",
      "Percentual de participação calculado automaticamente",
      "Valor total por categoria em tempo real",
      "Cálculo automático da posição: Preço Médio × Quantidade",
      "Preço atual opcional com indicador visual (🟢 acima do PM / 🔴 abaixo)",
    ],
  },
  {
    icon: "✓", title: "Tarefas",
    color: "#06b6d4", bg: "#ecfeff",
    desc: "Organize seu dia a dia com um sistema completo de gerenciamento de tarefas com validações.",
    steps: [
      "Crie tarefas com título, prioridade (🔵 Baixa, 🟡 Média, 🔴 Alta) e prazo",
      "Acompanhe o status: Pendente → Ativa → Concluída (ou Cancelada)",
      "Filtre por status para focar no que importa agora",
      "Validação de campos obrigatórios com mensagens inline",
      "Confirmação de exclusão via toast (Sonner) — sem alert() bloqueante",
    ],
  },
  {
    icon: "🎯", title: "Metas",
    color: "#f59e0b", bg: "#fffbeb",
    desc: "Defina objetivos e acompanhe o progresso com barras coloridas e indicadores visuais.",
    steps: [
      "Crie metas com título, descrição, prazo e progresso (0-100%)",
      "Barra de progresso animada: 🔴 <50%, 🟡 50-80%, 🟢 >80%",
      "Filtro por status: Pendente, Ativa, Concluída, Cancelada",
      "Validação de progresso entre 0 e 100",
    ],
  },
  {
    icon: "📝", title: "Matérias + Estudos",
    color: "#ec4899", bg: "#fdf2f8",
    desc: "Gerencie disciplinas e registre cada sessão de estudo com controle de tempo.",
    steps: [
      "Cadastre matérias com nome e descrição",
      "Registre estudos vinculados com data, duração em minutos e anotações",
      "Duração com cor variável: 🟢 2h+, 🔵 1h+, 🟡 30min+, 🔴 <30min",
      "Acompanhe total de horas por matéria automaticamente",
      "Filtre estudos por matéria para ver seu histórico",
    ],
  },
  {
    icon: "🎨", title: "Design System & UX",
    color: "#ec4899", bg: "#fdf2f8",
    desc: "Interface moderna com componentes padronizados, responsividade e notificações elegantes.",
    steps: [
      "Componentes de formulário padronizados: Input, Select, DateInput, NumberInput, TextArea",
      "Estados visuais consistentes: foco (anel indigo), erro (borda + fundo vermelho), disabled",
      "Notificações via Sonner: toast.promise para loading/sucesso/erro em operações assíncronas",
      "Sidebar responsiva: fixa no desktop, drawer com overlay no mobile",
      "Modal com backdrop blur, formulários condicionais e validação inline",
    ],
  },
  {
    icon: "🔐", title: "Autenticação Segura",
    color: "#6366f1", bg: "#eef2ff",
    desc: "Sistema de login com JWT, cookies HttpOnly e proteção de dados por usuário.",
    steps: [
      "Registre-se com nome, e-mail e senha (com confirmação)",
      "Login com feedback visual de erro e loading state",
      "Token JWT armazenado em cookie seguro",
      "Todas as requisições autenticadas automaticamente",
      "Logout com limpeza de sessão e redirecionamento",
    ],
  },
];

export default function ComoFunciona() {
  return (
    <Layout>
      <div className="animate-fade-in" style={{ maxWidth: "900px", margin: "0 auto" }}>
        <div style={{ textAlign: "center", padding: "clamp(24px, 5vw, 40px) clamp(16px, 4vw, 24px)", background: "linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4338ca 100%)", borderRadius: "14px", color: "white", marginBottom: "clamp(16px, 3vw, 32px)" }}>
          <span style={{ fontSize: "clamp(32px, 6vw, 48px)", display: "block", marginBottom: "8px" }}>⚡</span>
          <h1 style={{ fontSize: "clamp(22px, 4vw, 32px)", fontWeight: 800, marginBottom: "8px", letterSpacing: "-1px" }}>Como o FocusLife Funciona</h1>
          <p style={{ fontSize: "clamp(13px, 1.8vw, 16px)", opacity: 0.85, maxWidth: "650px", margin: "0 auto", lineHeight: 1.7 }}>
            Hub completo de produtividade pessoal: tarefas, estudos, metas e finanças com carteiras de investimento e despesas.
          </p>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "clamp(14px, 2.5vw, 24px)" }}>
          {features.map((f, i) => (
            <div key={i} style={{ background: "white", borderRadius: "12px", padding: "clamp(16px, 3vw, 28px)", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", borderLeft: `5px solid ${f.color}`, transition: "all 0.25s ease" }} className="animate-fade-in"
              onMouseEnter={e => { e.currentTarget.style.boxShadow = "0 10px 25px rgba(0,0,0,0.1)"; }}
              onMouseLeave={e => { e.currentTarget.style.boxShadow = "0 1px 2px rgba(0,0,0,0.05)"; }}>
              <div style={{ display: "flex", alignItems: "flex-start", gap: "clamp(8px, 2vw, 14px)", marginBottom: "clamp(10px, 2vw, 16px)" }}>
                <div style={{ width: "clamp(40px, 5vw, 50px)", height: "clamp(40px, 5vw, 50px)", borderRadius: "10px", background: f.bg, display: "flex", alignItems: "center", justifyContent: "center", fontSize: "clamp(18px, 2.5vw, 24px)", flexShrink: 0 }}>{f.icon}</div>
                <div>
                  <h2 style={{ fontSize: "clamp(16px, 2vw, 20px)", fontWeight: 700, color: "#0f172a", margin: "0 0 2px" }}>{f.title}</h2>
                  <p style={{ fontSize: "clamp(12px, 1.4vw, 14px)", color: "#64748b", margin: 0 }}>{f.desc}</p>
                </div>
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 220px), 1fr))", gap: "clamp(8px, 1.5vw, 10px)" }}>
                {f.steps.map((step, j) => (
                  <div key={j} style={{ display: "flex", alignItems: "flex-start", gap: "8px", padding: "clamp(8px, 1.5vw, 12px) clamp(10px, 1.5vw, 14px)", background: f.bg, borderRadius: "8px" }}>
                    <span style={{ width: "clamp(20px, 2.5vw, 24px)", height: "clamp(20px, 2.5vw, 24px)", borderRadius: "50%", background: f.color, color: "white", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "11px", fontWeight: 700, flexShrink: 0 }}>{j + 1}</span>
                    <span style={{ fontSize: "clamp(12px, 1.3vw, 13px)", color: "#0f172a", lineHeight: 1.5 }}>{step}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div style={{ textAlign: "center", padding: "clamp(24px, 5vw, 40px) 16px", marginTop: "8px" }}>
          <p style={{ fontSize: "clamp(15px, 2vw, 18px)", fontWeight: 600, color: "#0f172a", marginBottom: "4px" }}>Pronto para começar?</p>
          <p style={{ fontSize: "clamp(12px, 1.5vw, 14px)", color: "#64748b", marginBottom: "clamp(14px, 2vw, 20px)" }}>Navegue pelo menu lateral e explore cada funcionalidade.</p>
          <div style={{ display: "flex", justifyContent: "center", gap: "clamp(6px, 1.5vw, 12px)", flexWrap: "wrap" }}>
            {[{ label: "Tarefas", path: "/tarefas", bg: "#06b6d4" },{ label: "Metas", path: "/metas", bg: "#f59e0b" },{ label: "Carteiras", path: "/financas", bg: "#10b981" }].map((btn, i) => (
              <a key={i} href={btn.path} style={{ padding: "clamp(8px, 1.5vw, 11px) clamp(16px, 3vw, 24px)", background: btn.bg, color: "white", borderRadius: "8px", textDecoration: "none", fontSize: "clamp(13px, 1.5vw, 14px)", fontWeight: 600, transition: "all 0.15s ease", display: "inline-block" }}
                onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-2px)"; e.currentTarget.style.boxShadow = "0 4px 12px rgba(0,0,0,0.15)"; }}
                onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "none"; }}>{btn.label}</a>
            ))}
          </div>
        </div>
      </div>
    </Layout>
  );
}
