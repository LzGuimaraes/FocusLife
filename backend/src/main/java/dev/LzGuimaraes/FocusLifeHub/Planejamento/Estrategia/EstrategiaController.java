package dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia.dto.EstrategiaRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia.dto.EstrategiaResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/estrategias")
public class EstrategiaController {

    private final EstrategiaService estrategiaService;

    public EstrategiaController(EstrategiaService estrategiaService) {
        this.estrategiaService = estrategiaService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<EstrategiaResponseDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(estrategiaService.getAll(pageable));
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<EstrategiaResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(estrategiaService.getById(id));
    }

    @PostMapping("/create")
    public ResponseEntity<EstrategiaResponseDTO> create(@Valid @RequestBody EstrategiaRequestDTO dto) {
        return new ResponseEntity<>(estrategiaService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<EstrategiaResponseDTO> update(@PathVariable Long id,
                                                       @Valid @RequestBody EstrategiaRequestDTO dto) {
        return ResponseEntity.ok(estrategiaService.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        estrategiaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
