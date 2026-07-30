package dev.LzGuimaraes.FocusLifeHub.Tarefas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Tarefas.dto.TarefasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.dto.TarefasResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tarefas")
public class TarefasController {

    private final TarefasService tarefasService;

    public TarefasController(TarefasService tarefasService) {
        this.tarefasService = tarefasService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<TarefasResponseDTO>> getAllTarefas(Pageable pageable) {
        Page<TarefasResponseDTO> tarefas = tarefasService.getAllTarefas(pageable);
        return ResponseEntity.ok(tarefas);
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<TarefasResponseDTO> getTarefaById(@PathVariable Long id) {
        TarefasResponseDTO tarefa = tarefasService.getTarefaById(id);
        return ResponseEntity.ok(tarefa);
    }

    @PostMapping("/create")
    public ResponseEntity<TarefasResponseDTO> createTarefa(@Valid @RequestBody TarefasRequestDTO dto) {
        TarefasResponseDTO createdTarefa = tarefasService.createTarefa(dto);
        return new ResponseEntity<>(createdTarefa, HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<TarefasResponseDTO> updateTarefa(
            @PathVariable Long id,
            @Valid @RequestBody TarefasRequestDTO dto) {
        TarefasResponseDTO updatedTarefa = tarefasService.updateTarefa(id, dto);
        return ResponseEntity.ok(updatedTarefa);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteTarefa(@PathVariable Long id) {
        tarefasService.deleteTarefa(id);
        return ResponseEntity.noContent().build();
    }
}