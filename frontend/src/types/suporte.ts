export type TipoChamado = "NOVA_MUSICA" | "NOVO_ATIVO" | "MELHORIA" | "DUVIDA_PROBLEMA";
export interface ChamadoSuporte {
  id: number;
  protocolo: string;
  tipo: TipoChamado;
  titulo: string;
  descricao: string;
  criadoEm: string;
  emailEnviado: boolean;
}
export interface PaginaChamados {
  content: ChamadoSuporte[];
  totalPages: number;
}
