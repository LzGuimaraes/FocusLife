package dev.LzGuimaraes.FocusLifeHub.Admin;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminUserRecordsDTO;
import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Metas.Enum.MetaStatus;
import dev.LzGuimaraes.FocusLifeHub.Metas.dto.MetasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.Prioridade;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.dto.TarefasResponseDTO;

/**
 * Consultas ADMIN (SOMENTE LEITURA) dos registros de UM usuário-alvo, por módulo.
 *
 * O id do usuário consultado vem SEMPRE no caminho ({@code /admin/users/{usuarioId}/...})
 * e é repassado obrigatoriamente ao serviço: não existe endpoint "todos os usuários"
 * nem parâmetro de requisição capaz de trocar o dono dos registros.
 *
 * Autorização: {@code /admin/**} exige ROLE_ADMIN validada pelo backend
 * ({@code SecurityConfig} + {@code SecurityFilter} lendo o papel do banco).
 *
 * Somente métodos {@code GET} — nenhuma escrita nesta camada.
 */
@RestController
@RequestMapping("/admin/users/{usuarioId}")
public class AdminUserRecordsController {

    private final AdminUserRecordsService adminUserRecordsService;

    public AdminUserRecordsController(AdminUserRecordsService adminUserRecordsService) {
        this.adminUserRecordsService = adminUserRecordsService;
    }

    /**
     * Tarefas do usuário-alvo.
     * {@code GET /admin/users/{usuarioId}/tarefas?status=&prioridade=&page=&size=&sort=}
     */
    @GetMapping("/tarefas")
    public ResponseEntity<AdminUserRecordsDTO<TarefasResponseDTO>> tarefas(
            @PathVariable("usuarioId") Long usuarioId,
            @RequestParam(name = "status", required = false) TarefaStatus status,
            @RequestParam(name = "prioridade", required = false) Prioridade prioridade,
            Pageable pageable) {
        return ResponseEntity.ok(adminUserRecordsService.tarefasDoUsuario(usuarioId, status, prioridade, pageable));
    }

    /**
     * Metas do usuário-alvo.
     * {@code GET /admin/users/{usuarioId}/metas?status=&page=&size=&sort=}
     */
    @GetMapping("/metas")
    public ResponseEntity<AdminUserRecordsDTO<MetasResponseDTO>> metas(
            @PathVariable("usuarioId") Long usuarioId,
            @RequestParam(name = "status", required = false) MetaStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(adminUserRecordsService.metasDoUsuario(usuarioId, status, pageable));
    }

    /**
     * Registros de estudo do usuário-alvo.
     * {@code GET /admin/users/{usuarioId}/estudos?materia_id=&de=&ate=&page=&size=&sort=}
     */
    @GetMapping("/estudos")
    public ResponseEntity<AdminUserRecordsDTO<EstudosResponseDTO>> estudos(
            @PathVariable("usuarioId") Long usuarioId,
            @RequestParam(name = "materia_id", required = false) Long materiaId,
            @RequestParam(name = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            Pageable pageable) {
        return ResponseEntity.ok(adminUserRecordsService.estudosDoUsuario(usuarioId, materiaId, de, ate, pageable));
    }
}
