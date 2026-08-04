import { useState } from "react";

const STORAGE_KEY = "focuslife:lofiPlaylistUrl";
const DEFAULT_URL = "https://www.youtube.com/watch?v=X4VbdwhkE10";

/**
 * Converte praticamente qualquer link do YouTube (vídeo, playlist, youtu.be, já-embed, ou até
 * um ID cru de 11 caracteres) na URL de embed correta. Retorna null se não conseguir reconhecer nada.
 */
function toEmbedUrl(input: string): string | null {
  const raw = input.trim();
  if (!raw) return null;

  // Já é um link de embed — usa direto.
  if (/youtube(-nocookie)?\.com\/embed\//.test(raw)) return raw;

  // ID "cru" (11 caracteres típicos de vídeo do YouTube, sem espaço nem barra).
  if (/^[a-zA-Z0-9_-]{11}$/.test(raw)) return `https://www.youtube.com/embed/${raw}`;

  let url: URL;
  try {
    url = new URL(raw.includes("://") ? raw : `https://${raw}`);
  } catch {
    return null;
  }

  const host = url.hostname.replace(/^www\./, "");
  const videoId = url.searchParams.get("v");
  const listId = url.searchParams.get("list");

  if (host === "youtu.be") {
    const id = url.pathname.replace("/", "");
    if (id) return listId ? `https://www.youtube.com/embed/${id}?list=${listId}` : `https://www.youtube.com/embed/${id}`;
  }

  if (host === "youtube.com" || host === "m.youtube.com") {
    if (videoId) return listId ? `https://www.youtube.com/embed/${videoId}?list=${listId}` : `https://www.youtube.com/embed/${videoId}`;
    if (listId) return `https://www.youtube.com/embed/videoseries?list=${listId}`;
    // .../shorts/ID
    const shortsMatch = url.pathname.match(/\/shorts\/([a-zA-Z0-9_-]{11})/);
    if (shortsMatch) return `https://www.youtube.com/embed/${shortsMatch[1]}`;
  }

  return null;
}

export default function LofiPlayer() {
  const [aberto, setAberto] = useState(false);
  const [salvo, setSalvo] = useState<string | null>(() => localStorage.getItem(STORAGE_KEY));
  const [inputValue, setInputValue] = useState(salvo || "");
  const [erro, setErro] = useState(false);

  const efetivo = salvo || DEFAULT_URL;
  const embedUrl = toEmbedUrl(efetivo);

  const salvar = () => {
    const texto = inputValue.trim();
    if (texto && !toEmbedUrl(texto)) { setErro(true); return; }
    setErro(false);
    if (texto) localStorage.setItem(STORAGE_KEY, texto);
    else localStorage.removeItem(STORAGE_KEY);
    setSalvo(texto || null);
  };

  const usarPadrao = () => {
    localStorage.removeItem(STORAGE_KEY);
    setSalvo(null);
    setInputValue("");
    setErro(false);
  };

  return (
    <div style={{ marginBottom: "20px" }}>
      <button
        type="button"
        onClick={() => setAberto(v => !v)}
        style={{ padding: "10px 18px", borderRadius: "10px", border: "none", cursor: "pointer", fontWeight: 600, fontSize: "14px", background: "#fdf2f8", color: "#ec4899", transition: "all 0.15s ease" }}
      >
        🎵 {aberto ? "Fechar música" : "Música para estudar"}
      </button>

      {aberto && (
        <div style={{ marginTop: "12px", width: "100%", maxWidth: "720px", display: "flex", flexDirection: "column", gap: "10px" }}>
          {/* Caixa para colar a URL do YouTube (vídeo ou playlist) */}
          <div style={{ padding: "12px 14px", borderRadius: "12px", background: "#fdf2f8", border: "1.5px solid #fbcfe8", display: "flex", flexDirection: "column", gap: "8px" }}>
            <label style={{ fontSize: "12px", fontWeight: 600, color: "#831843" }}>
              Cole a URL completa do YouTube (vídeo ou playlist) — se deixar em branco, toca o vídeo padrão
            </label>
            <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
              <input
                type="text"
                value={inputValue}
                onChange={e => { setInputValue(e.target.value); setErro(false); }}
                onKeyDown={e => { if (e.key === "Enter") salvar(); }}
                placeholder="https://www.youtube.com/watch?v=..."
                style={{ flex: 1, minWidth: "220px", padding: "9px 12px", fontSize: "13px", borderRadius: "8px", border: `1.5px solid ${erro ? "#fca5a5" : "#f9a8d4"}`, background: "white", color: "#0f172a", outline: "none" }}
              />
              <button type="button" onClick={salvar}
                style={{ padding: "9px 16px", borderRadius: "8px", border: "none", cursor: "pointer", fontWeight: 600, fontSize: "12px", background: "#ec4899", color: "white" }}>
                Usar este vídeo
              </button>
              {salvo && (
                <button type="button" onClick={usarPadrao}
                  style={{ padding: "9px 16px", borderRadius: "8px", border: "1.5px solid #f9a8d4", cursor: "pointer", fontWeight: 600, fontSize: "12px", background: "white", color: "#ec4899" }}>
                  Voltar ao padrão
                </button>
              )}
            </div>
            {erro && <p style={{ margin: 0, fontSize: "12px", color: "#dc2626" }}>⚠ Não consegui reconhecer esse link. Cole a URL completa do YouTube (ex: https://www.youtube.com/watch?v=ID).</p>}
          </div>

          {/* Player */}
          {embedUrl ? (
            <div style={{ width: "100%", aspectRatio: "16 / 9", borderRadius: "12px", overflow: "hidden", boxShadow: "var(--shadow-sm)" }}>
              <iframe
                key={embedUrl}
                src={embedUrl}
                title="Música para estudar (YouTube)"
                width="100%"
                height="100%"
                style={{ border: "none", display: "block" }}
                allow="autoplay; encrypted-media"
                loading="lazy"
                allowFullScreen
              />
            </div>
          ) : (
            <div style={{ padding: "16px", borderRadius: "12px", background: "#fef2f2", color: "#b91c1c", fontSize: "13px", fontWeight: 500 }}>
              ⚠ O link salvo não pôde ser reconhecido. Cole outro link acima.
            </div>
          )}
        </div>
      )}
    </div>
  );
}