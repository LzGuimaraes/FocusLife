import { useState, type CSSProperties } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, ChevronDown } from "lucide-react";
import Layout from "../components/Layout";
import "./ComoFunciona.css";

const sections = [
  {
    id: "rotina", icon: "✓", title: "Organize sua rotina", label: "Comece aqui",
    color: "var(--color-info)",
    description: "Tarefas e metas para saber o que fazer agora e acompanhar seu progresso.",
    steps: [
      { title: "Defina seus objetivos", text: "Crie metas com prazo e atualize o progresso conforme avançar." },
      { title: "Escolha as prioridades do dia", text: "Cadastre tarefas com prioridade e prazo. Filtre por status para focar nas pendências." },
      { title: "Acompanhe o que foi feito", text: "Atualize o status das tarefas e confira a evolução das metas. Tarefas pendentes ou ativas com prazo hoje recebem lembretes por e-mail." },
    ],
    links: [{ label: "Abrir tarefas", path: "/tarefas" }, { label: "Ver metas", path: "/metas" }],
  },
  {
    id: "contas", icon: "💳", title: "Mantenha as contas em dia", label: "Controle financeiro",
    color: "var(--color-success)",
    description: "Reúna despesas, acompanhe vencimentos e receba lembretes de pagamento.",
    steps: [
      { title: "Crie uma carteira de despesas", text: "Separe suas contas por carteira e escolha a moeda dos lançamentos." },
      { title: "Registre valores e vencimentos", text: "Consulte os totais e filtre contas pagas, pendentes ou atrasadas." },
      { title: "Marque as contas pagas", text: "Contas vencendo hoje ou atrasadas recebem um lembrete por e-mail por dia. Ao marcar como paga, a conta sai dos próximos avisos." },
    ],
    links: [{ label: "Abrir carteiras", path: "/financas/carteiras" }],
  },
  {
    id: "investimentos", icon: "📈", title: "Conheça seu patrimônio", label: "Antes de planejar aportes",
    color: "var(--color-success)",
    description: "Veja seus investimentos e acompanhe o resultado de compras e vendas.",
    steps: [
      { title: "Cadastre seus investimentos", text: "Crie uma carteira e inclua renda fixa, Tesouro Direto, ações, FIIs, ETFs ou criptomoedas." },
      { title: "Confira a distribuição", text: "Visualize o valor e a participação de cada categoria, com quantidade, preço médio e preço atual quando informado." },
      { title: "Registre as operações", text: "Em Compras/Vendas, acompanhe lucro ou prejuízo, rentabilidade e vendas parciais. Defina preços teto e mínimo para seus ativos." },
    ],
    links: [{ label: "Abrir carteiras", path: "/financas/carteiras" }, { label: "Compras/Vendas", path: "/financas/compras-vendas" }],
  },
  {
    id: "organizacao", icon: "📐", title: "Planeje os próximos aportes", label: "Depois de cadastrar a carteira",
    color: "var(--color-primary)",
    description: "Defina a carteira ideal, avalie os ativos e distribua o aporte com prioridades claras.",
    steps: [
      { title: "Defina suas metas de distribuição", text: "Na Carteira Ideal, configure classes, subclasses e metas por ativo. Compare a distribuição desejada com a carteira atual." },
      { title: "Monte os critérios de avaliação", text: "Crie checklists por subclasse e escolha as perguntas e os pesos que fazem sentido para sua estratégia." },
      { title: "Atribua as notas", text: "Responda às mesmas perguntas para comparar ativos da mesma subclasse. A nota ponderada vai de 0 a 100 e tem histórico de evolução." },
      { title: "Simule e confira o aporte", text: "Informe o valor em Próximos Aportes. O plano prioriza quem está abaixo da meta e usa as notas para dividir entre eles, respeitando os limites e bloqueios. Exporte o resultado em CSV." },
    ],
    note: "O plano busca aplicar pelo menos 95% do aporte e pode ampliar a margem operacional. Limites máximos, bloqueios e cotas inteiras ainda podem deixar uma sobra; confira o motivo nos alertas.",
    links: [{ label: "Abrir organização", path: "/planejamento" }, { label: "Simular aporte", path: "/planejamento/aportes" }],
  },
  {
    id: "estudos", icon: "📝", title: "Estude com mais foco", label: "Aprendizado",
    color: "var(--color-info)",
    description: "Organize matérias, registre sessões e acompanhe o tempo dedicado a cada disciplina.",
    steps: [
      { title: "Organize as matérias", text: "Cadastre suas disciplinas e escolha os dias da semana em que pretende estudar." },
      { title: "Registre o tempo e as anotações", text: "Use o cronômetro da matéria ou registre uma sessão com data, duração e notas. No player de música, escolha um estilo e sorteie um vídeo do catálogo." },
      { title: "Revise sua dedicação", text: "Veja o total de horas por matéria e filtre o histórico das sessões de estudo." },
    ],
    links: [{ label: "Abrir matérias", path: "/materias" }, { label: "Ver estudos", path: "/estudos" }],
  },
  {
    id: "treinos", icon: "💪", title: "Crie constância nos treinos", label: "Bem-estar",
    color: "var(--color-primary)",
    description: "Planeje exercícios e acompanhe sua execução e consistência semana a semana.",
    steps: [
      { title: "Monte a rotina semanal", text: "Cadastre treinos por dia da semana e organize exercícios com séries, repetições e outras medidas." },
      { title: "Marque a execução", text: "Use o checklist de cada sessão para acompanhar os exercícios feitos e a conclusão do treino." },
      { title: "Acompanhe sua constância", text: "Consulte o resumo semanal, as sequências de treinos e o histórico das semanas anteriores." },
    ],
    links: [{ label: "Abrir treinos", path: "/treinos" }],
  },
];

export default function ComoFunciona() {
  const [expandedId, setExpandedId] = useState<string | null>(null);

  return (
    <Layout>
      <div className="how-it-works animate-fade-in">
        <header className="how-it-works__hero">
          <span className="how-it-works__eyebrow">⚡ FocusLife</span>
          <h1>Menos pendências. Mais direção.</h1>
          <p>Organize sua rotina, cuide das finanças e acompanhe sua evolução em um só lugar.</p>
          <Link to="/tarefas" className="how-it-works__start">Começar pelas tarefas <ArrowRight size={16} aria-hidden="true" /></Link>
        </header>

        <div className="how-it-works__intro">
          <h2>Como funciona</h2>
          <p>Siga a ordem sugerida ou abra só o que precisa agora.</p>
        </div>

        <div className="how-it-works__cards">
          {sections.map((section, index) => {
            const expanded = expandedId === section.id;
            return (
              <section key={section.id} className="how-it-works__card" style={{ "--guide-accent": section.color } as CSSProperties}>
                <h3>
                  <button
                    type="button"
                    id={`guide-toggle-${section.id}`}
                    className="how-it-works__toggle"
                    aria-expanded={expanded}
                    aria-controls={`guide-panel-${section.id}`}
                    onClick={() => setExpandedId(expanded ? null : section.id)}
                  >
                    <span className="how-it-works__icon" aria-hidden="true">{section.icon}</span>
                    <span className="how-it-works__summary">
                      <span className="how-it-works__label">{String(index + 1).padStart(2, "0")} · {section.label}</span>
                      <span className="how-it-works__title">{section.title}</span>
                      <span className="how-it-works__description">{section.description}</span>
                    </span>
                    <ChevronDown size={20} className="how-it-works__chevron" aria-hidden="true" />
                  </button>
                </h3>
                <div
                  id={`guide-panel-${section.id}`}
                  className="how-it-works__panel"
                  role="region"
                  aria-labelledby={`guide-toggle-${section.id}`}
                  hidden={!expanded}
                >
                  <ol className="how-it-works__steps">
                    {section.steps.map(step => (
                      <li key={step.title}>
                        <h4>{step.title}</h4>
                        <p>{step.text}</p>
                      </li>
                    ))}
                  </ol>
                  {"note" in section && <p className="how-it-works__note">{section.note}</p>}
                  <div className="how-it-works__links">
                    {section.links.map(link => (
                      <Link key={link.path} to={link.path}>{link.label} <ArrowRight size={15} aria-hidden="true" /></Link>
                    ))}
                  </div>
                </div>
              </section>
            );
          })}
        </div>
        <p className="how-it-works__footer">Comece por uma área e avance no seu ritmo. Você pode voltar a este guia quando precisar.</p>
      </div>
    </Layout>
  );
}
