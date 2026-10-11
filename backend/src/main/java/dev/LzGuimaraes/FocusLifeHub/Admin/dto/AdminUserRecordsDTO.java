package dev.LzGuimaraes.FocusLifeHub.Admin.dto;

import org.springframework.data.domain.Page;

/**
 * Página de registros de UM usuário-alvo, retornada pelas consultas
 * administrativas por módulo.
 *
 * O {@code usuarioId} aparece EXPLÍCITO na resposta para deixar inequívoco de
 * quem são os registros devolvidos: as consultas administrativas nunca são
 * "todos os usuários" — sempre há um alvo identificado. Os elementos da página
 * reutilizam os mesmos DTOs de resposta dos endpoints comuns (nenhum campo
 * sensível novo é exposto).
 *
 * @param <T> DTO de cada registro do módulo (ex.: {@code TarefasResponseDTO}).
 */
public record AdminUserRecordsDTO<T>(Long usuarioId, Page<T> pagina) {
}
