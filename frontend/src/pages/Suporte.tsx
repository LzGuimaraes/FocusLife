import { useEffect, useRef, useState } from "react";
import { isAxiosError } from "axios";
import { useSearchParams } from "react-router-dom";
import api from "../api/api";
import { useAuth } from "../auth/AuthProvider";
import Layout from "../components/Layout";
import { Input, TextArea } from "../components/Form";
import { Button } from "../components/Shared";
import { PageHeader, Spinner, Pagination } from "../components/UI";
import type { ChamadoSuporte, PaginaChamados, TipoChamado } from "../types/suporte";
import "./Suporte.css";

const categorias: { tipo: TipoChamado; icon: string; nome: string; resumo: string; dica: string; exemplo: string }[] = [
  { tipo: "NOVA_MUSICA", icon: "🎵", nome: "Nova música", resumo: "Sugira um vídeo para o catálogo.", dica: "Informe o artista, o estilo e o link do vídeo no YouTube.", exemplo: "Adicionar uma música ao estilo Rock" },
  { tipo: "NOVO_ATIVO", icon: "📈", nome: "Novo ativo", resumo: "Peça um ativo que ainda não está disponível.", dica: "Informe o ticker, o nome e a categoria do ativo que deseja cadastrar.", exemplo: "Adicionar um ativo ao catálogo" },
  { tipo: "MELHORIA", icon: "💡", nome: "Melhoria do sistema", resumo: "Conte o que facilitaria sua rotina.", dica: "Descreva a melhoria, em qual tela ela se aplica e como ajudaria você.", exemplo: "Sugestão para a tela de finanças" },
  { tipo: "DUVIDA_PROBLEMA", icon: "💬", nome: "Dúvida ou problema", resumo: "Solicite ajuda com o sistema.", dica: "Conte o que aconteceu, em qual tela e quais passos fez antes de encontrar o problema.", exemplo: "Preciso de ajuda com uma funcionalidade" },
];

export default function Suporte() {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const [tipo, setTipo] = useState<TipoChamado>(() => categorias.find(c => c.tipo === params.get("tipo"))?.tipo || "NOVA_MUSICA");
  const [titulo, setTitulo] = useState("");
  const [descricao, setDescricao] = useState("");
  const [erros, setErros] = useState<{ titulo?: string; descricao?: string; envio?: string }>({});
  const [enviando, setEnviando] = useState(false);
  const [confirmacao, setConfirmacao] = useState<ChamadoSuporte | null>(null);
  const [chamados, setChamados] = useState<ChamadoSuporte[]>([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erroHistorico, setErroHistorico] = useState(false);
  const [atualizacao, setAtualizacao] = useState(0);
  const solicitacaoId = useRef<string | null>(null);
  const emEnvio = useRef(false);
  const categoria = categorias.find(c => c.tipo === tipo)!;

  useEffect(() => {
    const controller = new AbortController();
    api.get<PaginaChamados>("/suporte/chamados", { params: { page: pagina }, signal: controller.signal })
      .then(({ data }) => { if (!controller.signal.aborted) { setChamados(data.content); setTotalPaginas(data.totalPages); setErroHistorico(false); } })
      .catch(() => { if (!controller.signal.aborted) setErroHistorico(true); })
      .finally(() => { if (!controller.signal.aborted) setCarregando(false); });
    return () => controller.abort();
  }, [pagina, atualizacao]);

  const editar = () => { solicitacaoId.current = null; setConfirmacao(null); setErros({}); };
  const enviar = async () => {
    if (emEnvio.current) return;
    const validacao: typeof erros = {};
    if (!titulo.trim()) validacao.titulo = "Informe o assunto.";
    if (!descricao.trim()) validacao.descricao = "Descreva sua solicitação.";
    setErros(validacao);
    if (Object.keys(validacao).length) return;
    emEnvio.current = true; setEnviando(true);
    try {
      solicitacaoId.current ??= crypto.randomUUID();
      const { data } = await api.post<ChamadoSuporte>("/suporte/chamados", { solicitacaoId: solicitacaoId.current, tipo, titulo: titulo.trim(), descricao: descricao.trim() });
      setConfirmacao(data); setTitulo(""); setDescricao(""); solicitacaoId.current = null;
      setPagina(0); setCarregando(true); setAtualizacao(n => n + 1);
    } catch (e) {
      setErros({ envio: isAxiosError(e) ? e.response?.data?.message || "Não foi possível confirmar o chamado. Tente novamente; seu texto foi mantido." : "Não foi possível confirmar o chamado. Tente novamente; seu texto foi mantido." });
    } finally { emEnvio.current = false; setEnviando(false); }
  };
  const atualizarHistorico = () => { setCarregando(true); setAtualizacao(n => n + 1); };

  return (
    <Layout>
      <div className="support-page">
        <PageHeader icon="💬" title="Suporte" subtitle="Peça uma música, solicite um ativo ou ajude a melhorar o FocusLife." />
        {confirmacao && <div className="support-page__success" role="status">
          <strong>Chamado {confirmacao.protocolo} registrado.</strong>
          <p>{confirmacao.emailEnviado ? "O pedido foi enviado ao suporte por e-mail." : "O aviso ao suporte está aguardando envio. Seu pedido está salvo e o sistema tentará enviar novamente."}</p>
        </div>}
        <section className="support-page__card">
          <h2>Abrir chamado</h2>
          <div className="support-page__categories" role="group" aria-label="Tipo de solicitação">
            {categorias.map(c => <button type="button" key={c.tipo} aria-pressed={tipo === c.tipo} disabled={enviando}
              onClick={() => { editar(); setTipo(c.tipo); }}>
              <span aria-hidden="true">{c.icon}</span><strong>{c.nome}</strong><span>{c.resumo}</span>
            </button>)}
          </div>
          <form onSubmit={e => { e.preventDefault(); enviar(); }} className="support-page__form" noValidate>
            <Input label="Assunto" placeholder={categoria.exemplo} value={titulo} required maxLength={120} disabled={enviando}
              onChange={e => { editar(); setTitulo(e.target.value); }} error={erros.titulo} />
            <TextArea label="Sua solicitação" hint={categoria.dica} placeholder={categoria.dica} rows={5} required maxLength={4000} value={descricao} disabled={enviando}
              onChange={e => { editar(); setDescricao(e.target.value); }} error={erros.descricao} />
            <div className="support-page__form-footer"><p>E-mail para retorno: <strong>{user?.email}</strong></p><span>{descricao.length}/4.000</span></div>
            {erros.envio && <p className="support-page__error" role="alert">{erros.envio}</p>}
            <Button type="submit" loading={enviando}>Enviar chamado</Button>
          </form>
        </section>
        <section className="support-page__history">
          <div className="support-page__history-heading"><h2>Meus chamados</h2><Button size="sm" variant="outline" disabled={carregando} onClick={atualizarHistorico}>Atualizar</Button></div>
          {carregando ? <Spinner text="Carregando chamados..." /> : erroHistorico ? <div className="support-page__card"><p>Não foi possível carregar seus chamados.</p><Button size="sm" variant="outline" onClick={atualizarHistorico}>Tentar novamente</Button></div>
            : chamados.length === 0 ? <p>Você ainda não abriu chamados. Seu histórico aparecerá aqui.</p>
            : <div className="support-page__list">{chamados.map(c => <article key={c.id} className="support-page__card">
              <div className="support-page__ticket-heading"><span>{c.protocolo} · {categorias.find(cat => cat.tipo === c.tipo)?.nome}</span><span className={`support-page__badge${c.emailEnviado ? "" : " support-page__badge--pending"}`}>{c.emailEnviado ? "Aviso enviado" : "Aviso pendente"}</span></div>
              <h3>{c.titulo}</h3><time dateTime={c.criadoEm}>{new Date(c.criadoEm).toLocaleString("pt-BR")}</time>
              <details><summary>Ver solicitação</summary><p>{c.descricao}</p></details>
            </article>)}</div>}
          <Pagination page={pagina} totalPages={totalPaginas} onChange={p => { if (p !== pagina) { setCarregando(true); setPagina(p); } }} ariaLabel="Paginação de chamados" />
        </section>
      </div>
    </Layout>
  );
}
