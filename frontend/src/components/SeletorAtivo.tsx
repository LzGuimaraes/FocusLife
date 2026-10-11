import { useEffect, useState } from "react";
import api from "../api/api";
import AtivoAutocomplete, { type AtivoCadastro } from "./AtivoAutocomplete";
import { Select } from "./Form";
import { Badge } from "./Shared";
import { catInfo, classeSugerida } from "../utils/percentual";
import type { CategoriaInvestimento } from "../types/planejamento";

/* ══════════════════════════════════════════════════════════════════════
   Seletor de ativo REUTILIZÁVEL (Compras/Vendas e Preço teto/mínimo).

   Duas formas de escolher o ativo:
     1. CATÁLOGO — autocomplete de tickers (`/ativos-cadastrados`), que já
        traz preco_atual; a classe é sugerida pelo tipo do ticker.
     2. MINHA POSIÇÃO — lista as posições que o usuário JÁ tem na carteira
        escolhida (`/ativos/by-carteira/{id}`), útil para renda fixa/caixinha
        que não têm ticker no catálogo.

   Ao escolher, emite a seleção (com ticker/nome em snapshot e preco_atual) e
   mostra um CHIP visual com ticker, classe, preço atual e, na posição, a
   quantidade — para o usuário conferir de onde o número veio.
   ══════════════════════════════════════════════════════════════════════ */

export interface CarteiraOption {
  id: number;
  nome: string;
}

interface PosicaoOption {
  id: number;
  nome: string;
  categoriaInvestimento: CategoriaInvestimento | null;
  quantidade: number | null;
  precoAtual: number | null;
}

export interface SelecaoAtivo {
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string;
  nome: string;
  classe: CategoriaInvestimento | null;
  preco_atual: number | null;
  quantidade: number | null;
  carteira_investimento_id: number | null;
}

interface Props {
  carteiras: CarteiraOption[];
  onSelecionar: (sel: SelecaoAtivo | null) => void;
}

const fmtPreco = (v: number | null) =>
  v == null ? "—" : `R$ ${v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export default function SeletorAtivo({ carteiras, onSelecionar }: Props) {
  const [carteiraId, setCarteiraId] = useState("");
  const [modo, setModo] = useState<"catalogo" | "posicao">("catalogo");
  const [catalogo, setCatalogo] = useState<AtivoCadastro | null>(null);
  const [posicoes, setPosicoes] = useState<PosicaoOption[]>([]);
  const [carregandoPosicoes, setCarregandoPosicoes] = useState(false);
  const [posicaoId, setPosicaoId] = useState("");
  const [selecao, setSelecao] = useState<SelecaoAtivo | null>(null);

  const temCarteira = carteiras.length > 0;

  // Ao trocar a carteira, zera as escolhas: uma posição de outra carteira não vale.
  useEffect(() => {
    setCatalogo(null);
    setPosicaoId("");
    setPosicoes([]);
    setSelecao(null);
    onSelecionar(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [carteiraId]);

  // Carrega as posições da carteira só quando o modo "Minha posição" está ativo.
  useEffect(() => {
    if (modo !== "posicao" || !carteiraId) {
      return;
    }
    setCarregandoPosicoes(true);
    api.get(`/ativos/by-carteira/${carteiraId}`)
      .then(r => {
        const lista: PosicaoOption[] = (r.data ?? []).map((x: any) => ({
          id: x.id,
          nome: x.nome,
          categoriaInvestimento: x.categoriaInvestimento ?? null,
          quantidade: x.quantidade ?? null,
          precoAtual: x.precoAtual ?? null,
        }));
        setPosicoes(lista);
      })
      .catch(() => setPosicoes([]))
      .finally(() => setCarregandoPosicoes(false));
  }, [modo, carteiraId]);

  const emitir = (sel: SelecaoAtivo | null) => {
    setSelecao(sel);
    onSelecionar(sel);
  };

  const escolherCatalogo = (a: AtivoCadastro) => {
    setCatalogo(a);
    setPosicaoId("");
    emitir({
      ativo_cadastro_id: a.id,
      ativo_id: null,
      ticker: a.nome,
      nome: a.nome,
      classe: classeSugerida(a.tipo),
      preco_atual: a.precoAtual,
      quantidade: null,
      carteira_investimento_id: carteiraId ? Number(carteiraId) : null,
    });
  };

  const escolherPosicao = (id: string) => {
    setPosicaoId(id);
    setCatalogo(null);
    const p = posicoes.find(x => String(x.id) === id);
    if (!p) { emitir(null); return; }
    emitir({
      ativo_cadastro_id: null,
      ativo_id: p.id,
      ticker: p.nome,
      nome: p.nome,
      classe: p.categoriaInvestimento,
      preco_atual: p.precoAtual,
      quantidade: p.quantidade,
      carteira_investimento_id: carteiraId ? Number(carteiraId) : null,
    });
  };

  const trocarModo = (novo: "catalogo" | "posicao") => {
    setModo(novo);
    setCatalogo(null);
    setPosicaoId("");
    emitir(null);
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
      {temCarteira && (
        <Select label="Carteira (opcional)" value={carteiraId} onChange={e => setCarteiraId(e.target.value)}>
          <option value="">— Sem carteira específica —</option>
          {carteiras.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}
        </Select>
      )}

      {temCarteira && (
        <div role="tablist" style={{ display: "flex", gap: "6px" }}>
          {([["catalogo", "🔎 Do catálogo"], ["posicao", "📁 Minha posição"]] as const).map(([valor, label]) => {
            const ativoAba = modo === valor;
            return (
              <button key={valor} type="button" role="tab" aria-selected={ativoAba}
                onClick={() => trocarModo(valor)}
                style={{
                  padding: "7px 14px", borderRadius: "9999px", cursor: "pointer",
                  fontSize: "12px", fontWeight: 700,
                  border: ativoAba ? "1.5px solid #6366f1" : "1.5px solid #e2e8f0",
                  background: ativoAba ? "#eef2ff" : "white",
                  color: ativoAba ? "#4338ca" : "#64748b",
                }}>
                {label}
              </button>
            );
          })}
        </div>
      )}

      {modo === "catalogo" || !temCarteira ? (
        <div>
          <label style={{ display: "block", fontSize: "13px", fontWeight: 600, color: "#374151", marginBottom: "5px" }}>
            Ativo (ticker)
          </label>
          <AtivoAutocomplete value={catalogo?.nome ?? ""} onSelect={escolherCatalogo} />
        </div>
      ) : (
        <Select label="Posição" value={posicaoId} onChange={e => escolherPosicao(e.target.value)}
          hint="Posições que já existem na sua carteira.">
          <option value="">{carregandoPosicoes ? "Carregando..." : "— Escolha uma posição —"}</option>
          {posicoes.map(p => (
            <option key={p.id} value={p.id}>
              {p.nome}{p.quantidade != null ? ` · ${p.quantidade} un.` : ""}
            </option>
          ))}
        </Select>
      )}

      {modo === "posicao" && carteiraId && !carregandoPosicoes && posicoes.length === 0 && (
        <p style={{ fontSize: "12px", color: "#94a3b8", margin: 0 }}>
          Esta carteira não tem posições. Cadastre em <strong>Carteiras</strong> ou use “Do catálogo”.
        </p>
      )}

      {selecao && <ChipAtivo selecao={selecao} />}
    </div>
  );
}

function ChipAtivo({ selecao }: { selecao: SelecaoAtivo }) {
  const info = selecao.classe ? catInfo(selecao.classe) : null;
  return (
    <div style={{
      display: "flex", flexWrap: "wrap", alignItems: "center", gap: "10px",
      padding: "10px 14px", borderRadius: "10px",
      background: info?.bg ?? "#f8fafc", border: `1px solid ${info ? info.color + "55" : "#e2e8f0"}`,
    }}>
      <strong style={{ fontSize: "15px", color: "#0f172a" }}>{selecao.ticker}</strong>
      {info && <Badge color={info.color} bg="white">{info.icon} {info.label}</Badge>}
      <span style={{ fontSize: "12px", color: "#475569" }}>
        Preço atual: <strong>{fmtPreco(selecao.preco_atual)}</strong>
      </span>
      {selecao.quantidade != null && (
        <span style={{ fontSize: "12px", color: "#475569" }}>
          Quantidade: <strong>{selecao.quantidade}</strong>
        </span>
      )}
    </div>
  );
}
