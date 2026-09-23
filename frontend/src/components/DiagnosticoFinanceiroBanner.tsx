import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import { Button } from "../components/Shared";
import { controlStyle, miniLabel } from "../components/FormStyles";
import type { DiagnosticoFinanceiro, ReparoResultado } from "../types/diagnostico";
import { fmtMoeda } from "../utils/percentual";

/* ══════════════════════════════════════════════════════════════════════
   Diagnóstico financeiro (recuperação de dados legados).

   POR QUE ISSO EXISTE
   As carteiras antigas foram divididas em tabelas novas (migrations V8..V11).
   Na V8 o vínculo de cada conta só era preenchido quando a carteira de origem
   tinha `tipo_carteira = 'INVESTIMENTO'` (ou 'DESPESAS'); qualquer outra
   situação deixou a linha SEM carteira. Como todo acesso passa pela carteira,
   ela desapareceu das telas sem nenhum erro — o sintoma visível é uma carteira
   que "não tem investimentos cadastrados".

   Este banner consulta o backend, avisa quantos registros estão sem vínculo e
   permite religá-los a uma carteira (reparo só preenche um vínculo vazio).
   ══════════════════════════════════════════════════════════════════════ */

interface Props {
  /** Chamado depois de um reparo bem-sucedido, para a tela recarregar os dados. */
  onReparado?: () => void;
  /**
   * Mostra também o aviso "verificamos e não há nada a recuperar" quando a
   * carteira aberta está vazia. Faz sentido onde o sintoma aparece (Carteira
   * Ideal); em Finanças isso seria ruído para quem só está começando.
   */
  mostrarSemPosicoes?: boolean;
  /**
   * Carteira aberta na tela. O aviso de "carteira vazia" é calculado SÓ para
   * ela: ter posições em outra carteira não pode esconder o diagnóstico desta.
   */
  carteiraId?: number | null;
}

export default function DiagnosticoFinanceiroBanner({ onReparado, mostrarSemPosicoes = false, carteiraId = null }: Props) {
  const [diag, setDiag] = useState<DiagnosticoFinanceiro | null>(null);
  const [aberto, setAberto] = useState(false);
  const [reparando, setReparando] = useState(false);
  const [destinoInvestimento, setDestinoInvestimento] = useState("");
  const [destinoDividas, setDestinoDividas] = useState("");
  const [resultado, setResultado] = useState<ReparoResultado | null>(null);

  const carregar = useCallback(async () => {
    try {
      const { data } = await api.get<DiagnosticoFinanceiro>("/financeiro/diagnostico");
      setDiag(data);
    } catch {
      // Diagnóstico é auxiliar: se falhar (ex.: backend antigo, sem a rota),
      // simplesmente não mostra nada. O interceptor de API já loga no console.
      setDiag(null);
    }
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  if (!diag) return null;

  const orfas = diag.posicoes_sem_carteira + diag.despesas_sem_carteira;
  const aberta = (carteiraId != null) ? diag.carteiras.find(c => c.id === carteiraId) ?? null : null;
  const comItens = diag.carteiras.filter(c => c.itens > 0 && c.id !== carteiraId);
  const semPosicoes = mostrarSemPosicoes && aberta != null && aberta.itens === 0;
  if (orfas === 0 && !semPosicoes) return null;

  const handleReparar = async () => {
    setReparando(true);
    try {
      const { data } = await api.post<ReparoResultado>("/financeiro/diagnostico/reparar", {
        carteira_investimento_id: destinoInvestimento ? Number(destinoInvestimento) : null,
        carteira_dividas_id: destinoDividas ? Number(destinoDividas) : null,
      });
      setResultado(data);
      toast.success(`Recuperado! ${data.posicoes_religadas} posição(ões) e ${data.despesas_religadas} despesa(s).`);
      await carregar();
      setAberto(false);
      onReparado?.();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao recuperar os dados");
    } finally {
      setReparando(false);
    }
  };

  /* ── Caso 1: há registros sem carteira (o bug real) ── */
  if (orfas > 0) {
    return (
      <div style={banner("#fffbeb", "#fde68a")}>
        <p style={{ fontSize: "13px", color: "#92400e", margin: 0, fontWeight: 600 }}>
          ⚠ Encontramos {diag.posicoes_sem_carteira > 0 && <> {diag.posicoes_sem_carteira} posição(ões) de investimento</>}
          {diag.posicoes_sem_carteira > 0 && diag.despesas_sem_carteira > 0 ? " e " : ""}
          {diag.despesas_sem_carteira > 0 && <> {diag.despesas_sem_carteira} despesa(s)</>} sem carteira.
        </p>
        <p style={{ fontSize: "12px", color: "#a16207", margin: "6px 0 0" }}>
          Isso veio da migração que dividiu as carteiras: quando a carteira antiga não tinha um tipo reconhecido, a
          posição ficou sem vínculo — e como o sistema sempre navega pela carteira, ela parou de aparecer em todas as
          telas (inclusive na Carteira Ideal). Nenhum valor foi perdido.
        </p>

        {diag.pode_reparar ? (
          <>
            <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginTop: "10px" }}>
              <button type="button" onClick={() => setAberto(a => !a)} style={botaoSecundario}>
                {aberto ? "Cancelar" : "Recuperar agora →"}
              </button>
              {resultado && (
                <span style={{ fontSize: "12px", color: "#166534", alignSelf: "center" }}>
                  ✅ {resultado.posicoes_religadas} posição(ões) e {resultado.despesas_religadas} despesa(s) religadas.
                </span>
              )}
            </div>

            {aberto && (
              <div style={{ marginTop: "12px", background: "white", border: "1px solid #fde68a", borderRadius: "10px", padding: "14px" }}>
                <div style={{ display: "flex", gap: "14px", flexWrap: "wrap", alignItems: "flex-end" }}>
                  {diag.posicoes_sem_carteira > 0 && (
                    <div>
                      <label style={miniLabel}>Posições vão para</label>
                      <select value={destinoInvestimento} onChange={e => setDestinoInvestimento(e.target.value)}
                        aria-label="Carteira de investimento que vai receber as posições"
                        style={{ ...controlStyle, minWidth: "220px" }}>
                        <option value="">— Automático (primeira carteira) —</option>
                        {diag.carteiras.map(c => (
                          <option key={c.id} value={c.id}>{c.nome} ({c.itens} posições)</option>
                        ))}
                      </select>
                    </div>
                  )}
                  {diag.despesas_sem_carteira > 0 && (
                    <div>
                      <label style={miniLabel}>Despesas vão para</label>
                      <select value={destinoDividas} onChange={e => setDestinoDividas(e.target.value)}
                        aria-label="Carteira de dívidas que vai receber as despesas"
                        style={{ ...controlStyle, minWidth: "220px" }}>
                        <option value="">— Automático (primeira carteira de despesas) —</option>
                        {diag.carteiras_dividas.map(c => (
                          <option key={c.id} value={c.id}>{c.nome} ({c.itens} despesas)</option>
                        ))}
                      </select>
                    </div>
                  )}
                  <Button onClick={handleReparar} loading={reparando}>Recuperar</Button>
                </div>
                <p style={{ fontSize: "11px", color: "#a16207", margin: "10px 0 0" }}>
                  A recuperação só preenche o vínculo que está vazio: nenhum valor, nome ou data é alterado. Se nenhuma
                  carteira existir, uma nova ("Investimentos recuperados") é criada.
                </p>
              </div>
            )}
          </>
        ) : (
          <p style={{ fontSize: "12px", color: "#a16207", margin: "8px 0 0" }}>
            {diag.limitacao ?? "Peça a um administrador para executar a recuperação."}
          </p>
        )}
      </div>
    );
  }

  /* ── Caso 2: a carteira aberta está vazia e não há órfãos para recuperar ── */
  return (
    <div style={banner("#eef2ff", "#c7d2fe")}>
      <p style={{ fontSize: "13px", color: "#3730a3", margin: 0, fontWeight: 600 }}>
        🔍 Verificamos os seus dados antigos e não há nada a recuperar.
      </p>
      <p style={{ fontSize: "12px", color: "#4338ca", margin: "6px 0 0" }}>
        Nenhuma posição ficou sem carteira nas migrações, então <strong>{aberta?.nome ?? "esta carteira"}</strong> está
        mesmo com {aberta?.itens ?? 0} posição(ões).
        {comItens.length > 0 ? (
          <> Os seus valores estão em <strong>{comItens.map(c => c.nome).join(", ")}</strong> — troque no seletor
          <strong> Carteira</strong> acima para trabalhar nela.</>
        ) : (
          <> Cadastre as posições em <strong>Finanças</strong> (abrir a carteira → Novo Ativo) e elas aparecem aqui
          automaticamente.</>
        )}
      </p>
      {diag.carteiras.length > 1 && (
        <ul style={{ margin: "8px 0 0", paddingLeft: "18px", fontSize: "12px", color: "#4338ca" }}>
          {diag.carteiras.map(c => (
            <li key={c.id}>
              <strong>{c.nome}</strong> — {c.itens} posição(ões) · {fmtMoeda(c.valor, c.moeda ?? "BRL")}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

const banner = (bg: string, border: string): React.CSSProperties => ({
  background: bg, border: `1px solid ${border}`, borderRadius: "12px",
  padding: "14px 16px", marginBottom: "18px",
});

const botaoSecundario: React.CSSProperties = {
  background: "white", border: "1px solid #fde68a", borderRadius: "8px",
  padding: "7px 14px", fontSize: "12px", fontWeight: 700, color: "#b45309", cursor: "pointer",
};
