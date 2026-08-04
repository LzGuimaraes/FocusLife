import { useState } from "react";

const STORAGE_KEY = "focuslife:lofiPlaylistUrl";

/** Aceita: URL completa (watch?v=..., playlist?list=..., embed/videoseries?list=...), ou só o ID da playlist/vídeo. */
function toEmbedUrl(input: string): string | null {
  const raw = input.trim();
  if (!raw) return null;

  // Já é uma URL de embed
  if (raw.includes("youtube.com/embed/")) return raw;

  try {
    const url = new URL(raw.includes("://") ? raw : `https://${raw}`);
    const list = url.searchParams.get("list");
    const v = url.searchParams.get("v");
    if (list) return `https://www.youtube.com/embed/videoseries?list=${list}`;
    if (v) return `https://www.youtube.com/embed/${v}`;
    // youtu.be/<id>
    if (url.hostname.includes("youtu.be")) {
      const id = url.pathname.replace("/", "");
      if (id) return `https://www.youtube.com/embed/${id}`;
    }
  } catch {
    // Não é uma URL válida — trata como ID "cru" abaixo
  }

  // ID cru: se tiver cara de ID de playlist (geralmente começa com "PL", "UU", "LL", "RD", "OL"), assume playlist; senão, vídeo.
  const looksLikePlaylistId = /^(PL|UU|LL|RD|OL|FL)/i.test(raw);
  return looksLikePlaylistId ? `https://www.youtube.com/embed/videoseries?list=${raw}` : `https://www.youtube.com/embed/${raw}`;
}

export default function LofiPlayer() {
  const [aberto, setAberto] = useState(false);
  const [playlistUrl, setPlaylistUrl] = useState<string | null>(() => localStorage.getItem(STORAGE_KEY));
  const [editando, setEditando] = useState(!playlistUrl);
  const [inputValue, setInputValue] = useState(playlistUrl || "");

  const embedUrl = playlistUrl ? toEmbedUrl(playlistUrl) : null;

  const salvar = () => {
    const valido = toEmbedUrl(inputValue);
    if (!valido) return;
    localStorage.setItem(STORAGE_KEY, inputValue.trim());
    setPlaylistUrl(inputValue.trim());
    setEditando(false);
  };

  const limpar = () => {
    localStorage.removeItem(STORAGE_KEY);
    setPlaylistUrl(null);
    setInputValue("");
    setEditando(true);
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
        <div style={{ marginTop: "12px", width: "100%", maxWidth: "720px" }}>
          {editando ? (
            <div style={{ padding: "16px", borderRadius: "12px", background: "#fdf2f8", border: "1.5px solid #fbcfe8", display: "flex", flexDirection: "column", gap: "10px" }}>
              <label style={{ fontSize: "13px", fontWeight: 600, color: "#831843" }}>
                Cole o link (ou ID) da sua playlist/vídeo do YouTube
              </label>
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                <input
                  type="text"
                  value={inputValue}
                  onChange={e => setInputValue(e.target.value)}
                  onKeyDown={e => { if (e.key === "Enter") salvar(); }}
                  placeholder="https://www.youtube.com/playlist?list=..."
                  style={{ flex: 1, minWidth: "220px", padding: "10px 14px", fontSize: "14px", borderRadius: "10px", border: "1.5px solid #f9a8d4", background: "white", color: "#0f172a", outline: "none" }}
                />
                <button type="button" onClick={salvar} disabled={!toEmbedUrl(inputValue)}
                  style={{ padding: "10px 18px", borderRadius: "10px", border: "none", cursor: toEmbedUrl(inputValue) ? "pointer" : "not-allowed", fontWeight: 600, fontSize: "13px", background: "#ec4899", color: "white", opacity: toEmbedUrl(inputValue) ? 1 : 0.5 }}>
                  Salvar
                </button>
              </div>
              <p style={{ margin: 0, fontSize: "12px", color: "#9d174d" }}>
                Aceita link de playlist, de vídeo, link curto (youtu.be) ou só o ID. Fica salvo neste navegador.
              </p>
            </div>
          ) : (
            <>
              {embedUrl && (
                <div style={{ width: "100%", aspectRatio: "16 / 9", borderRadius: "12px", overflow: "hidden", boxShadow: "var(--shadow-sm)" }}>
                  <iframe
                    src={embedUrl}
                    title="Música para estudar (playlist no YouTube)"
                    width="100%"
                    height="100%"
                    style={{ border: "none", display: "block" }}
                    allow="autoplay; encrypted-media"
                    loading="lazy"
                    allowFullScreen
                  />
                </div>
              )}
              <div style={{ display: "flex", gap: "8px", marginTop: "8px" }}>
                <button type="button" onClick={() => setEditando(true)} style={{ padding: "6px 12px", borderRadius: "8px", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "#fdf2f8", color: "#ec4899" }}>
                  ✏️ Trocar playlist
                </button>
                <button type="button" onClick={limpar} style={{ padding: "6px 12px", borderRadius: "8px", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "#fef2f2", color: "#ef4444" }}>
                  🗑 Remover
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}