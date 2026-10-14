import { useEffect, useState } from "react";
import { isAxiosError } from "axios";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, TextArea } from "../components/Form";
import { Button } from "../components/Shared";
import { PageHeader, Spinner, EmptyState } from "../components/UI";
import type { AdicaoMusicas, EstiloMusical, MusicaVideo } from "../types/musica";
import "./AdminMusicas.css";

const mensagem = (e: unknown) => isAxiosError(e) ? e.response?.data?.message || "Não foi possível concluir. Tente novamente." : "Não foi possível concluir. Tente novamente.";

export default function AdminMusicas() {
  const [estilos, setEstilos] = useState<EstiloMusical[]>([]);
  const [estiloId, setEstiloId] = useState<number | null>(null);
  const [videos, setVideos] = useState<MusicaVideo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [tentativa, setTentativa] = useState(0);
  const [ocupado, setOcupado] = useState(false);
  const [urls, setUrls] = useState("");
  const [erroUrls, setErroUrls] = useState("");
  const [modalEstilo, setModalEstilo] = useState(false);
  const [editandoEstilo, setEditandoEstilo] = useState<EstiloMusical | null>(null);
  const [nome, setNome] = useState("");
  const [erroEstilo, setErroEstilo] = useState("");
  const [editandoVideo, setEditandoVideo] = useState<MusicaVideo | null>(null);
  const [urlEditada, setUrlEditada] = useState("");
  const [erroVideo, setErroVideo] = useState("");
  const [exclusao, setExclusao] = useState<{ tipo: "estilo" | "video"; id: number; descricao: string } | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      api.get<EstiloMusical[]>("/admin/musicas/estilos", { signal: controller.signal }),
      estiloId === null ? Promise.resolve({ data: [] as MusicaVideo[] })
        : api.get<MusicaVideo[]>(`/admin/musicas/estilos/${estiloId}/videos`, { signal: controller.signal }),
    ]).then(([listaEstilos, listaVideos]) => {
      if (controller.signal.aborted) return;
      setEstilos(listaEstilos.data);
      setVideos(listaVideos.data);
      setErro("");
    }).catch(e => {
      if (!controller.signal.aborted) setErro(mensagem(e));
    }).finally(() => { if (!controller.signal.aborted) setCarregando(false); });
    return () => controller.abort();
  }, [estiloId, tentativa]);

  const atualizar = () => { setCarregando(true); setTentativa(t => t + 1); };
  const selecionar = (id: number) => {
    if (id === estiloId || ocupado) return;
    setCarregando(true); setEstiloId(id); setVideos([]); setUrls(""); setErroUrls("");
  };
  const abrirEstilo = (estilo: EstiloMusical | null) => {
    setEditandoEstilo(estilo); setNome(estilo?.nome || ""); setErroEstilo(""); setModalEstilo(true);
  };
  const salvarEstilo = async () => {
    if (ocupado) return;
    setOcupado(true); setErroEstilo("");
    try {
      const { data } = editandoEstilo
        ? await api.put<EstiloMusical>(`/admin/musicas/estilos/${editandoEstilo.id}`, { nome })
        : await api.post<EstiloMusical>("/admin/musicas/estilos", { nome });
      setModalEstilo(false); setEstiloId(data.id); atualizar();
      toast.success("Estilo salvo.");
    } catch (e) { setErroEstilo(mensagem(e)); }
    finally { setOcupado(false); }
  };
  const adicionarVideos = async () => {
    if (estiloId === null || ocupado) return;
    const links = urls.split(/\r?\n/).map(s => s.trim()).filter(Boolean);
    if (links.length === 0) { setErroUrls("Cole pelo menos uma URL."); return; }
    if (links.length > 100) { setErroUrls("Adicione até 100 URLs por vez."); return; }
    setOcupado(true); setErroUrls("");
    try {
      const { data } = await api.post<AdicaoMusicas>(`/admin/musicas/estilos/${estiloId}/videos`, { urls: links });
      setUrls(""); atualizar();
      toast.success(`${data.adicionados} vídeo(s) adicionado(s).${data.duplicados ? ` ${data.duplicados} já cadastrado(s).` : ""}`);
    } catch (e) { setErroUrls(mensagem(e)); }
    finally { setOcupado(false); }
  };
  const salvarVideo = async () => {
    if (!editandoVideo || ocupado) return;
    setOcupado(true); setErroVideo("");
    try {
      await api.put(`/admin/musicas/videos/${editandoVideo.id}`, { url: urlEditada });
      setEditandoVideo(null); atualizar(); toast.success("Vídeo atualizado.");
    } catch (e) { setErroVideo(mensagem(e)); }
    finally { setOcupado(false); }
  };
  const excluir = async () => {
    if (!exclusao || ocupado) return;
    setOcupado(true);
    try {
      await api.delete(`/admin/musicas/${exclusao.tipo === "estilo" ? "estilos" : "videos"}/${exclusao.id}`);
      if (exclusao.tipo === "estilo") setEstiloId(null);
      setExclusao(null); atualizar(); toast.success("Registro removido.");
    } catch (e) { toast.error(mensagem(e)); }
    finally { setOcupado(false); }
  };
  const estilo = estilos.find(e => e.id === estiloId);

  return (
    <Layout>
      <Link to="/admin" className="music-admin__back">← Administração</Link>
      <PageHeader icon="🎵" title="Catálogo de músicas" subtitle="Cadastre estilos e vídeos disponíveis para todos os usuários." />
      <div className="music-admin__toolbar"><Button onClick={() => abrirEstilo(null)} disabled={ocupado}>Novo estilo</Button><Link to="/materias">Abrir player de estudos →</Link></div>
      {erro && <div className="music-admin__error" role="alert"><p>{erro}</p><Button size="sm" variant="outline" onClick={atualizar}>Tentar novamente</Button></div>}
      <div className="music-admin__grid">
        <section className="music-admin__card">
          <h2>Estilos musicais</h2>
          <p>Selecione um estilo para gerenciar seus vídeos.</p>
          <div className="music-admin__styles" role="group" aria-label="Estilos musicais">
            {estilos.map(e => <button type="button" key={e.id} aria-pressed={estiloId === e.id} onClick={() => selecionar(e.id)} disabled={ocupado || carregando}>
              <span>{e.nome}</span><span>{e.totalVideos} {e.totalVideos === 1 ? "vídeo" : "vídeos"}</span>
            </button>)}
          </div>
          {!carregando && !erro && estilos.length === 0 && <p>Nenhum estilo cadastrado. Crie o primeiro para começar.</p>}
        </section>
        <section className="music-admin__card">
          {carregando ? <Spinner text="Carregando catálogo..." /> : erro ? <p>Recarregue o catálogo para gerenciar os vídeos.</p> : estilo ? (
            <>
              <div className="music-admin__heading"><h2>{estilo.nome}</h2><div className="music-admin__actions">
                <Button size="sm" variant="outline" disabled={ocupado} onClick={() => abrirEstilo(estilo)}>Renomear</Button>
                <Button size="sm" variant="ghost" disabled={ocupado || estilo.totalVideos > 0} title={estilo.totalVideos > 0 ? "Remova os vídeos antes de excluir o estilo" : "Excluir estilo"}
                  onClick={() => setExclusao({ tipo: "estilo", id: estilo.id, descricao: estilo.nome })}>Excluir estilo</Button>
              </div></div>
              <form onSubmit={e => { e.preventDefault(); adicionarVideos(); }} className="music-admin__form">
                <TextArea label="URLs dos vídeos" hint="Um vídeo do YouTube por linha, até 100 por vez. Links repetidos neste estilo são ignorados."
                  placeholder="https://www.youtube.com/watch?v=..." rows={4} value={urls} disabled={ocupado}
                  onChange={e => { setUrls(e.target.value); setErroUrls(""); }} error={erroUrls} />
                <Button type="submit" loading={ocupado}>Adicionar vídeos</Button>
              </form>
              <h3>Vídeos cadastrados ({videos.length})</h3>
              {videos.length === 0 ? <p>Adicione vídeos para disponibilizar este estilo no player.</p> : (
                <ul className="music-admin__videos">{videos.map(v => <li key={v.id}>
                  <a href={v.url} target="_blank" rel="noopener noreferrer">{v.url}</a>
                  <div className="music-admin__actions">
                    <Button size="sm" variant="outline" disabled={ocupado} onClick={() => { setEditandoVideo(v); setUrlEditada(v.url); setErroVideo(""); }}>Editar</Button>
                    <Button size="sm" variant="ghost" disabled={ocupado} onClick={() => setExclusao({ tipo: "video", id: v.id, descricao: v.url })}>Remover</Button>
                  </div>
                </li>)}</ul>
              )}
            </>
          ) : <EmptyState icon="🎶" title="Escolha um estilo" text="Cadastre sertanejo, rock, lo-fi ou outros estilos e organize os vídeos de cada um." />}
        </section>
      </div>
      <Modal open={modalEstilo} onClose={() => { if (!ocupado) setModalEstilo(false); }} title={editandoEstilo ? "Renomear estilo" : "Novo estilo"}
        onSubmit={salvarEstilo} submitLabel={ocupado ? "Salvando..." : "Salvar estilo"}>
        <Input label="Nome do estilo" placeholder="Ex.: Sertanejo" required maxLength={80} value={nome} disabled={ocupado} onChange={e => { setNome(e.target.value); setErroEstilo(""); }} error={erroEstilo} />
      </Modal>
      <Modal open={!!editandoVideo} onClose={() => { if (!ocupado) setEditandoVideo(null); }} title="Editar vídeo" onSubmit={salvarVideo} submitLabel={ocupado ? "Salvando..." : "Salvar vídeo"}>
        <Input label="URL do vídeo no YouTube" required maxLength={2048} value={urlEditada} disabled={ocupado} onChange={e => { setUrlEditada(e.target.value); setErroVideo(""); }} error={erroVideo} />
      </Modal>
      <Modal open={!!exclusao} onClose={() => { if (!ocupado) setExclusao(null); }} title={exclusao?.tipo === "estilo" ? "Excluir estilo" : "Remover vídeo"}>
        <p className="music-admin__break">{exclusao?.descricao}</p><p>Este registro sairá do catálogo disponível aos usuários.</p>
        <div className="music-admin__actions"><Button variant="secondary" disabled={ocupado} onClick={() => setExclusao(null)}>Cancelar</Button><Button variant="destructive" loading={ocupado} onClick={excluir}>Confirmar remoção</Button></div>
      </Modal>
    </Layout>
  );
}
