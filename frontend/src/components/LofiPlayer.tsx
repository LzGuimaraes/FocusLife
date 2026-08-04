import { useState } from "react";

// URL fixa da playlist lofi (troque <ID_DA_PLAYLIST> pelo ID real)
const PLAYLIST_URL = "https://www.youtube.com/embed/videoseries?list=<ID_DA_PLAYLIST>";

export default function LofiPlayer() {
  const [aberto, setAberto] = useState(false);

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
        <div style={{ marginTop: "12px", width: "100%", maxWidth: "720px", aspectRatio: "16 / 9", borderRadius: "12px", overflow: "hidden", boxShadow: "var(--shadow-sm)" }}>
          <iframe
            src={PLAYLIST_URL}
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
    </div>
  );
}
