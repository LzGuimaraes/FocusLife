import { useEffect, useRef, useState } from "react";
import api from "../api/api";

export interface AtivoCadastro {
  id: string;
  nome: string;
  tipo: string;
  precoAtual: number | null;
}

const tipoLabel: Record<string, string> = {
  ACAO: "📈 Ação",
  FII: "🏢 FII",
  ETF: "📦 ETF",
  BDR: "🌎 BDR",
  CRIPTOMOEDA: "₿ Cripto",
};

interface Props {
  value: string;
  onSelect: (ativo: AtivoCadastro) => void;
  error?: string;
}

/**
 * Select pesquisável (autocomplete) que carrega os ativos cadastrados no
 * backend (/ativos-cadastrados). Ao selecionar, devolve o ativo para que o
 * formulário use o ticker (nome) e o preço atual (preco_atual).
 */
export default function AtivoAutocomplete({ value, onSelect, error }: Props) {
  const [query, setQuery] = useState(value);
  const [options, setOptions] = useState<AtivoCadastro[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => { setQuery(value); }, [value]);

  useEffect(() => {
    const t = setTimeout(() => {
      setLoading(true);
      api.get(`/ativos-cadastrados${query ? `?q=${encodeURIComponent(query)}` : ""}`)
        .then(r => setOptions(r.data ?? []))
        .catch(() => setOptions([]))
        .finally(() => setLoading(false));
    }, 200);
    return () => clearTimeout(t);
  }, [query]);

  // Fecha a lista ao clicar fora
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <div ref={ref} style={{ position: "relative" }}>
      <input
        value={query}
        onChange={e => { setQuery(e.target.value); setOpen(true); }}
        onFocus={() => setOpen(true)}
        placeholder="Buscar ticker (ex: PETR4, WEGE3, BTC...)"
        style={{ width: "100%", padding: "9px 12px", borderRadius: "8px", border: error ? "1.5px solid #ef4444" : "1.5px solid #e2e8f0", fontSize: "14px", outline: "none", background: "white", color: "#0f172a" }}
      />
      {error && <span style={{ fontSize: "12px", color: "#ef4444", marginTop: "4px", display: "block" }}>{error}</span>}

      {open && (
        <div style={{ position: "absolute", top: "calc(100% + 6px)", left: 0, right: 0, background: "white", border: "1px solid #e2e8f0", borderRadius: "10px", boxShadow: "0 10px 30px rgba(0,0,0,0.12)", zIndex: 30, maxHeight: "260px", overflowY: "auto" }}>
          {loading && <div style={{ padding: "12px 14px", fontSize: "13px", color: "#64748b" }}>Carregando...</div>}
          {!loading && options.length === 0 && <div style={{ padding: "12px 14px", fontSize: "13px", color: "#94a3b8" }}>Nenhum ativo cadastrado encontrado</div>}
          {options.map(a => (
            <button
              key={a.id}
              type="button"
              onClick={() => { onSelect(a); setQuery(a.nome); setOpen(false); }}
              style={{ display: "flex", justifyContent: "space-between", alignItems: "center", width: "100%", padding: "10px 14px", border: "none", background: "white", cursor: "pointer", fontSize: "14px", textAlign: "left", borderBottom: "1px solid #f1f5f9" }}
              onMouseEnter={e => { e.currentTarget.style.background = "#f8fafc"; }}
              onMouseLeave={e => { e.currentTarget.style.background = "white"; }}
            >
              <span style={{ fontWeight: 700, color: "#0f172a" }}>{a.nome}</span>
              <span style={{ fontSize: "12px", color: "#64748b" }}>{tipoLabel[a.tipo] || a.tipo}{a.precoAtual != null ? ` · R$ ${a.precoAtual.toFixed(2)}` : ""}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
