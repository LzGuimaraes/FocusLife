import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";

/* ══════════════════════════════════════════════════════════════════════
   Tema claro/escuro.

   O tema vive no atributo `data-theme` do <html>: o index.css define os
   tokens do CLARO em :root e os do ESCURO em :root[data-theme="dark"], e o
   theme.css (bridge dos estilos inline) só age no escuro.

   A escolha é guardada no localStorage ("fl-theme"). Um script inline no
   index.html aplica o tema ANTES do primeiro paint, para não haver flash
   branco quando o usuário deixa o escuro.
   ══════════════════════════════════════════════════════════════════════ */

export type Tema = "dark" | "light";

const CHAVE = "fl-theme";
const PADRAO: Tema = "dark";

function lerTemaSalvo(): Tema {
  try {
    const salvo = localStorage.getItem(CHAVE);
    if (salvo === "dark" || salvo === "light") return salvo;
  } catch {
    /* localStorage indisponível (modo privado/SSR): usa o padrão */
  }
  return PADRAO;
}

function aplicarNoDocumento(tema: Tema) {
  document.documentElement.dataset.theme = tema;
  document
    .querySelector('meta[name="theme-color"]')
    ?.setAttribute("content", tema === "dark" ? "#0b0f22" : "#f1f5f9");
}

interface TemaContexto {
  tema: Tema;
  alternar: () => void;
}

const Contexto = createContext<TemaContexto>({ tema: PADRAO, alternar: () => {} });

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [tema, setTema] = useState<Tema>(lerTemaSalvo);

  useEffect(() => {
    aplicarNoDocumento(tema);
    try {
      localStorage.setItem(CHAVE, tema);
    } catch {
      /* sem persistência: o tema vale só nesta sessão */
    }
  }, [tema]);

  const alternar = useCallback(() => {
    setTema(t => (t === "dark" ? "light" : "dark"));
  }, []);

  return <Contexto.Provider value={{ tema, alternar }}>{children}</Contexto.Provider>;
}

export const useTema = () => useContext(Contexto);
