let seq = 0;

/**
 * Chave local estável para itens de listas editáveis (usada como `key` do
 * React e para localizar o item em edição). Não vai para o backend.
 */
export const novaChave = (): string => `k${++seq}`;
