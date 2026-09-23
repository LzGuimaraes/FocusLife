package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistModeloDTO;
import jakarta.validation.Valid;

/**
 * Modelos de checklist (Módulo 4): templates reutilizáveis do usuário.
 * Aplicar um modelo a um ativo é feito em POST /checklists (com `modelo_id`).
 */
@RestController
@RequestMapping("/checklist-modelos")
public class ChecklistModeloController {

    private final ChecklistModeloService modeloService;

    public ChecklistModeloController(ChecklistModeloService modeloService) {
        this.modeloService = modeloService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<ChecklistModeloDTO.Resumo>> getAll(Pageable pageable) {
        return ResponseEntity.ok(modeloService.getAll(pageable));
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<ChecklistModeloDTO.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(modeloService.getById(id));
    }

    @PostMapping("/create")
    public ResponseEntity<ChecklistModeloDTO.Response> create(@Valid @RequestBody ChecklistModeloDTO.Request dto) {
        return new ResponseEntity<>(modeloService.create(dto), HttpStatus.CREATED);
    }

    /** Substitui o conteúdo do modelo (replace-all das perguntas). */
    @PutMapping("/alter/{id}")
    public ResponseEntity<ChecklistModeloDTO.Response> update(@PathVariable Long id,
                                                             @Valid @RequestBody ChecklistModeloDTO.Request dto) {
        return ResponseEntity.ok(modeloService.update(id, dto));
    }

    @PostMapping("/duplicar/{id}")
    public ResponseEntity<ChecklistModeloDTO.Response> duplicar(@PathVariable Long id) {
        return new ResponseEntity<>(modeloService.duplicar(id), HttpStatus.CREATED);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        modeloService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
