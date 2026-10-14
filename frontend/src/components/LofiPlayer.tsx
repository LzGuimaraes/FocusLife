import { useEffect, useId, useRef, useState } from "react";
import { isAxiosError } from "axios";
import { Link } from "react-router-dom";
import api from "../api/api";
import { useAuth } from "../auth/AuthProvider";
import { Button } from "./Shared";
import type { EstiloMusical, MusicaVideo } from "../types/musica";
import "./LofiPlayer.css";

export default function LofiPlayer() {
  const id = useId();
  const { isAdmin } = useAuth();
  const [aberto, setAberto] = useState(false);
  const [estilos, setEstilos] = useState<EstiloMusical[]>([]);
  const [estiloId, setEstiloId] = useState<number | null>(null);
  const [video, setVideo] = useState<MusicaVideo | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [sorteando, setSorteando] = useState(false);
  const [erro, setErro] = useState("");
  const [falhaCatalogo, setFalhaCatalogo] = useState(false);
  const [tentativa, setTentativa] = useState(0);
  const sorteioAtual = useRef(0);

  useEffect(() => {
    if (!aberto) return;
    const controller = new AbortController();
    api.get<EstiloMusical[]>("/musicas/estilos", { signal: controller.signal })
      .then(({ data }) => {
        if (controller.signal.aborted) return;
        setEstilos(data.filter(e => e.totalVideos > 0));
        setEstiloId(atual => data.some(e => e.id === atual && e.totalVideos > 0) ? atual : null);
        setFalhaCatalogo(false);
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setFalhaCatalogo(true);
          setErro("Não foi possível carregar os estilos. Tente novamente.");
        }
      })
      .finally(() => { if (!controller.signal.aborted) setCarregando(false); });
    return () => controller.abort();
  }, [aberto, tentativa]);

  const alternar = () => {
    sorteioAtual.current++;
    setSorteando(false);
    if (!aberto) { setCarregando(true); setErro(""); }
    setAberto(!aberto);
  };

  const escolherEstilo = (novoId: number) => {
    sorteioAtual.current++;
    setEstiloId(novoId);
    setVideo(null);
    setErro("");
    setSorteando(false);
  };

  const sortear = async () => {
    if (estiloId === null || sorteando) return;
    const requisicao = ++sorteioAtual.current;
    setSorteando(true);
    setErro("");
    try {
      const { data } = await api.get<MusicaVideo>(`/musicas/estilos/${estiloId}/sortear`, {
        params: video?.estiloId === estiloId ? { anteriorId: video.id } : {},
      });
      if (requisicao === sorteioAtual.current) setVideo(data);
    } catch (e) {
      if (requisicao === sorteioAtual.current) {
        setErro(isAxiosError(e) ? e.response?.data?.message || "Não foi possível sortear. Tente novamente." : "Não foi possível sortear. Tente novamente.");
      }
    } finally {
      if (requisicao === sorteioAtual.current) setSorteando(false);
    }
  };

  const estilo = estilos.find(e => e.id === estiloId);

  return (
    <div className="lofi-player">
      <button type="button" onClick={alternar} className="lofi-player__toggle"
        aria-expanded={aberto} aria-controls={`${id}-panel`}>
        🎵 {aberto ? "Fechar música" : "Música para estudar"}
      </button>
      {aberto && (
        <div id={`${id}-panel`} className="lofi-player__panel">
          <div className="lofi-player__settings">
            <h2>Escolha seu estilo</h2>
            <p>Selecione um estilo e sorteie uma música para acompanhar seus estudos.</p>
            {carregando ? <p role="status">Carregando estilos...</p> : falhaCatalogo ? (
              <Button size="sm" variant="outline" onClick={() => { setCarregando(true); setErro(""); setTentativa(t => t + 1); }}>Tentar novamente</Button>
            ) : estilos.length === 0 ? (
              <p>A seleção de músicas ainda está sendo preparada. Volte em breve.</p>
            ) : (
              <div className="lofi-player__styles" role="group" aria-label="Estilos musicais">
                {estilos.map(e => (
                  <button key={e.id} type="button" className="lofi-player__style" aria-pressed={estiloId === e.id}
                    onClick={() => escolherEstilo(e.id)}>{e.nome}</button>
                ))}
              </div>
            )}
            {isAdmin && <Link className="lofi-player__admin" to="/admin/musicas">Gerenciar estilos e vídeos</Link>}
          </div>
          {!carregando && !falhaCatalogo && estilos.length > 0 && (
            <div className="lofi-player__now-playing">
              <p aria-live="polite">{estilo ? `${estilo.nome} · ${estilo.totalVideos} ${estilo.totalVideos === 1 ? "vídeo" : "vídeos"}` : "Escolha um estilo para começar."}</p>
              <Button onClick={sortear} disabled={!estilo} loading={sorteando}>🔀 {video ? "Sortear outro" : "Sortear"}</Button>
            </div>
          )}
          {erro && <p className="lofi-player__error" role="alert">{erro}</p>}
          {video && estilo && !falhaCatalogo && (
            <>
              <p className="lofi-player__hint">Vídeo sorteado. Dê play para começar.</p>
              <div className="lofi-player__video">
                <iframe key={video.embedUrl} src={video.embedUrl} title={`Música para estudar — ${estilo.nome}`}
                  width="100%" height="100%" allow="autoplay; encrypted-media" loading="lazy" allowFullScreen />
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
