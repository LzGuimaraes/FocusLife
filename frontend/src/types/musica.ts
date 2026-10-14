export interface EstiloMusical {
  id: number;
  nome: string;
  totalVideos: number;
}

export interface MusicaVideo {
  id: number;
  estiloId: number;
  url: string;
  embedUrl: string;
}

export interface AdicaoMusicas {
  adicionados: number;
  duplicados: number;
}
