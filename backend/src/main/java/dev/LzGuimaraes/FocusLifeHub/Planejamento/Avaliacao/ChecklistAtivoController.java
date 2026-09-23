package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.PerguntaDTO;
import jakarta.validation.Valid;

/**
 * Checklists de ativos (Módulos 2, 3 e 5).
 *
 * A âncora do checklist é o ativo do catálogo (`?ativo_cadastro_id=`) ou a
 * posição (`?ativo_id=`).
 */
@RestController
@RequestMapping("/checklists")
public class ChecklistAtivoController {

    private final ChecklistAtivoService checklistService;

    public ChecklistAtivoController(ChecklistAtivoService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping("/by-ativo")
    public ResponseEntity<List<ChecklistAtivoDTO.Response>> getByAtivo(
            @RequestParam(name = "ativo_cadastro_id", required = false) UUID ativoCadastroId,
            @RequestParam(name = "ativo_id", required = false) Long ativoId) {
        return ResponseEntity.ok(checklistService.getByAtivo(ativoCadastroId, ativoId));
    }

    /** Resumo dos ativos avaliados com o Quality Score consolidado (Módulo 5). */
    @GetMapping("/resumo")
    public ResponseEntity<List<ChecklistAtivoDTO.AtivoAvaliado>> resumo() {
        return ResponseEntity.ok(checklistService.resumoPorAtivo());
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<ChecklistAtivoDTO.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(checklistService.getById(id));
    }

    /** Cria em branco (com `perguntas`) ou a partir de um modelo (`modelo_id`). */
    @PostMapping("/create")
    public ResponseEntity<ChecklistAtivoDTO.Response> create(@Valid @RequestBody ChecklistAtivoDTO.Request dto) {
        return new ResponseEntity<>(checklistService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<ChecklistAtivoDTO.Response> update(@PathVariable Long id,
                                                            @Valid @RequestBody ChecklistAtivoDTO.UpdateRequest dto) {
        return ResponseEntity.ok(checklistService.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        checklistService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Duplica um checklist (estrutura sim, respostas não).
     * Corpo opcional: sem destino, a cópia fica no mesmo ativo.
     */
    @PostMapping("/duplicar/{id}")
    public ResponseEntity<ChecklistAtivoDTO.Response> duplicar(
            @PathVariable Long id,
            @RequestBody(required = false) ChecklistAtivoDTO.Request destino) {
        return new ResponseEntity<>(checklistService.duplicar(id, destino), HttpStatus.CREATED);
    }

    /* ── Perguntas ── */

    @PostMapping("/{id}/perguntas")
    public ResponseEntity<ChecklistAtivoDTO.Response> adicionarPergunta(
            @PathVariable Long id,
            @Valid @RequestBody PerguntaDTO.PerguntaRequest dto) {
        return new ResponseEntity<>(checklistService.adicionarPergunta(id, dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/perguntas/alter/{perguntaId}")
    public ResponseEntity<ChecklistAtivoDTO.Response> alterarPergunta(
            @PathVariable Long id,
            @PathVariable Long perguntaId,
            @Valid @RequestBody PerguntaDTO.PerguntaRequest dto) {
        return ResponseEntity.ok(checklistService.alterarPergunta(id, perguntaId, dto));
    }

    @DeleteMapping("/{id}/perguntas/{perguntaId}")
    public ResponseEntity<ChecklistAtivoDTO.Response> removerPergunta(@PathVariable Long id,
                                                                     @PathVariable Long perguntaId) {
        return ResponseEntity.ok(checklistService.removerPergunta(id, perguntaId));
    }

    /** Reordena as perguntas conforme a lista de IDs (↑↓ na interface). */
    @PutMapping("/{id}/perguntas/ordem")
    public ResponseEntity<ChecklistAtivoDTO.Response> reordenarPerguntas(@PathVariable Long id,
                                                                        @RequestBody List<Long> perguntaIds) {
        return ResponseEntity.ok(checklistService.reordenarPerguntas(id, perguntaIds));
    }

    /** Grava as respostas do usuário (nota/valor/opção/texto + observação). */
    @PutMapping("/{id}/responder")
    public ResponseEntity<ChecklistAtivoDTO.Response> responder(@PathVariable Long id,
                                                               @Valid @RequestBody ChecklistAtivoDTO.RespostasRequest dto) {
        return ResponseEntity.ok(checklistService.responder(id, dto));
    }
}
