import { useEffect, useState } from "react";
import api from "../api/api";
import Modal from "./Modal";
import { Spinner, EmptyState } from "./UI";

interface ContaLog { id: number; acao: string; criadoEm: string; }

interface Props {
  open: boolean;
  contaId: number;
  contaNome: string;
  onClose: () => void;
}

const PAGE_SIZE = 10;

export default function ContaLogsModal({ open, contaId, contaNome, onClose }: Props) {
  const [logs, setLogs] = useState<ContaLog[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open || !contaId) return;
    setLoading(true);
    api.get(`/despesas/${contaId}/logs?page=${page}&size=${PAGE_SIZE}`)
      .then(r => { setLogs(r.data.content || []); setTotalPages(r.data.totalPages || 0); })
      .catch(() => setLogs([]))
      .finally(() => setLoading(false));
  }, [open, contaId, page]);

  // criadoEm vem em ISO (ex.: "2026-08-03T08:00:00") → exibe dd/MM/yyyy HH:mm
  const fmtDateTime = (iso: string) => {
    const d = new Date(iso);
    const p = (n: number) => String(n).padStart(2, "0");
    return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
  };

  return (
    <Modal open={open} onClose={onClose} title={`Logs — ${contaNome}`} width="620px">
      {loading ? <Spinner text="Carregando logs..." /> :
        logs.length === 0 ? <EmptyState icon="📜" title="Sem logs" text="Nenhum registro para esta conta." /> :
        <>
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px" }}>
              <thead>
                <tr style={{ background: "#f8fafc" }}>
                  <th style={thStyle}>Ação</th>
                  <th style={{ ...thStyle, textAlign: "right" }}>Data/Hora</th>
                </tr>
              </thead>
              <tbody>
                {logs.map(l => (
                  <tr key={l.id} style={{ borderTop: "1px solid #f1f5f9" }}>
                    <td style={tdStyle}>{l.acao}</td>
                    <td style={{ ...tdStyle, textAlign: "right", whiteSpace: "nowrap", color: "#475569" }}>{fmtDateTime(l.criadoEm)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "12px" }}>
            <button type="button" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} style={navBtnStyle}>← Anterior</button>
            <span style={{ fontSize: "13px", fontWeight: 600, color: "#64748b" }}>Página {page + 1} de {Math.max(totalPages, 1)}</span>
            <button type="button" onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1} style={navBtnStyle}>Próximo →</button>
          </div>
        </>
      }
    </Modal>
  );
}

const thStyle: React.CSSProperties = { padding: "10px 14px", fontSize: "11px", fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.04em", whiteSpace: "nowrap" };
const tdStyle: React.CSSProperties = { padding: "10px 14px", borderTop: "1px solid #f1f5f9", verticalAlign: "middle" };
const navBtnStyle: React.CSSProperties = { padding: "7px 14px", borderRadius: "8px", border: "none", cursor: "pointer", fontSize: "13px", fontWeight: 600, background: "#f1f5f9", color: "#64748b" };
