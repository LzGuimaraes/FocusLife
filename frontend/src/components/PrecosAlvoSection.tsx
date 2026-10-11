import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import { NumberInput } from "./Form";
import { Badge, Button } from "./Shared";
import { Spinner } from "./UI";
import SeletorAtivo, { type CarteiraOption, type SelecaoAtivo } from "./SeletorAtivo";
import { textoParaNumOuNull } from "../utils/numeros";
import type { AtivoPrecoAlvo } from "../types/comprasVendas";

/* ══════════════════════════════════════════════════════════════════════
   Preço teto / preço mínimo por ativo.

   Lista as regras salvas (com o preço atual vindo do catálogo) e sinaliza
   visualmente quando o ativo está ACIMA do teto ou ABAIXO do mínimo. O
   formulário faz UPSERT: se já existir regra para o ativo escolhido, ela é
   atualizada em vez de duplicar.
   ══════════════════════════════════════════════════════════════════════ */

interface Props {
  carteiras: CarteiraOption[];
}

const fmt = (v: number | null) =>
  v == null ? "—" : `R$ ${v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export default function PrecosAlvoSection({ carteiras }: Props) {
  const [regras, setRegras] = useState<AtivoPrecoAlvo[]>([]);
  const [loading, setLoading] = useState(true);
  const [selecao, setSelecao] = useState<SelecaoAtivo | null>(null);
  const [teto, setTeto] = useState("");
  const [minimo, setMinimo] = useState("");
  const [salvando, setSalvando] = useState(false);

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<AtivoPrecoAlvo[]>("/precos-alvo/all");
      setRegras(data ?? []);
    } catch {
      toast.error("Erro ao carregar as regras de preço");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  const handleUpsert = async () => {
    if (!selecao) {
      toast.error("Escolha o ativo (catálogo ou posição) para definir o preço.");
      return;
    }
    const precoTeto = textoParaNumOuNull(teto);
    const precoMinimo = textoParaNumOuNull(minimo);
    if (precoTeto == null && precoMinimo == null) {
      toast.error("Informe o preço teto, o preço mínimo, ou ambos.");
      return;
    }
    if (precoTeto != null && precoTeto <= 0) {
      toast.error("O preço teto deve ser maior que zero.");
      return;
    }
    if (precoMinimo != null && precoMinimo <= 0) {
      toast.error("O preço mínimo deve ser maior que zero.");
      return;
    }
    if (precoTeto != null && precoMinimo != null && precoMinimo > precoTeto) {
      toast.error("O preço mínimo não pode ser maior que o preço teto.");
      return;
    }

    setSalvando(true);
    try {
      await api.post("/precos-alvo/upsert", {
        ativo_cadastro_id: selecao.ativo_cadastro_id,
        ativo_id: selecao.ativo_id,
        ticker: selecao.ticker,
        preco_teto: precoTeto,
        preco_minimo: precoMinimo,
      });
      toast.success("Regra de preço salva!");
      setTeto("");
      setMinimo("");
      await carregar();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao salvar a regra de preço");
    } finally {
      setSalvando(false);
    }
  };

  const handleDelete = (r: AtivoPrecoAlvo) => {
    toast("Excluir a regra de preço?", {
      action: {
        label: "Sim",
        onClick: () => {
          toast.promise(api.delete(`/precos-alvo/delete/${r.id}`), {
            loading: "Excluindo...",
            success: () => { carregar(); return "Regra excluída!"; },
            error: "Erro ao excluir",
          });
        },
      },
      cancel: { label: "Cancelar", onClick: () => {} },
    });
  };

  return (
    <section style={card}>
      <h2 style={{ fontSize: "16px", fontWeight: 700, color: "#0f172a", margin: "0 0 4px" }}>
        🎯 Preço teto / preço mínimo
      </h2>
      <p style={{ fontSize: "12.5px", color: "#64748b", margin: "0 0 16px" }}>
        Defina uma faixa de preço por ativo. O sistema avisa quando a cotação atual fica
        <strong> acima do teto</strong> (cuidado para não comprar caro) ou <strong>abaixo do mínimo</strong> (possível oportunidade).
      </p>

      {loading ? (
        <Spinner text="Carregando regras..." />
      ) : regras.length === 0 ? (
        <p style={{ fontSize: "13px", color: "#94a3b8", margin: "0 0 16px" }}>
          Nenhuma regra de preço cadastrada ainda.
        </p>
      ) : (
        <div style={{ overflowX: "auto", marginBottom: "18px" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "#64748b", fontSize: "11px", textTransform: "uppercase", letterSpacing: "0.3px" }}>
                <th style={th}>Ativo</th>
                <th style={th}>Preço atual</th>
                <th style={th}>Teto</th>
                <th style={th}>Mínimo</th>
                <th style={th}>Situação</th>
                <th style={th} />
              </tr>
            </thead>
            <tbody>
              {regras.map(r => (
                <tr key={r.id} style={{ borderTop: "1px solid #f1f5f9" }}>
                  <td style={td}><strong>{r.ticker}</strong></td>
                  <td style={td}>{fmt(r.preco_atual)}</td>
                  <td style={td}>{fmt(r.preco_teto)}</td>
                  <td style={td}>{fmt(r.preco_minimo)}</td>
                  <td style={td}>
                    {r.acima_do_teto ? (
                      <Badge color="#b91c1c" bg="#fee2e2">▲ Acima do teto</Badge>
                    ) : r.abaixo_do_minimo ? (
                      <Badge color="#047857" bg="#d1fae5">▼ Abaixo do mínimo</Badge>
                    ) : (
                      <Badge color="#475569" bg="#f1f5f9">• Dentro da faixa</Badge>
                    )}
                  </td>
                  <td style={{ ...td, textAlign: "right" }}>
                    <Button variant="ghost" size="sm" onClick={() => handleDelete(r)}
                      style={{ color: "#ef4444" }} title="Excluir regra">🗑</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div style={{ borderTop: "1px solid #f1f5f9", paddingTop: "16px" }}>
        <h3 style={{ fontSize: "13px", fontWeight: 700, color: "#334155", margin: "0 0 10px" }}>
          Definir / atualizar regra
        </h3>
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          <SeletorAtivo carteiras={carteiras} onSelecionar={setSelecao} />
          <div style={{ display: "flex", gap: "12px", flexWrap: "wrap" }}>
            <div style={{ flex: "1 1 160px" }}>
              <NumberInput label="Preço teto (R$)" value={teto} onChange={setTeto} decimal
                placeholder="Ex: 45,00" />
            </div>
            <div style={{ flex: "1 1 160px" }}>
              <NumberInput label="Preço mínimo (R$)" value={minimo} onChange={setMinimo} decimal
                placeholder="Ex: 28,00" />
            </div>
          </div>
          <div style={{ display: "flex", justifyContent: "flex-end" }}>
            <Button onClick={handleUpsert} loading={salvando}>Salvar regra</Button>
          </div>
        </div>
      </div>
    </section>
  );
}

const card: React.CSSProperties = {
  background: "white", borderRadius: "16px", padding: "22px 24px",
  boxShadow: "0 1px 3px rgba(15,23,42,0.05)", border: "1px solid #eef2f7",
};
const th: React.CSSProperties = { padding: "6px 10px", fontWeight: 700 };
const td: React.CSSProperties = { padding: "9px 10px", color: "#0f172a", verticalAlign: "middle" };
