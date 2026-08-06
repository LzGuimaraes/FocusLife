package dev.LzGuimaraes.FocusLifeHub.Despesa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Despesa.dto.DespesaLogResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Despesa.dto.DespesaRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Despesa.dto.DespesaResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/despesas")
public class DespesaController {

    private final DespesaService despesaService;

    public DespesaController(DespesaService despesaService) {
        this.despesaService = despesaService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<DespesaResponseDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(despesaService.getAll(pageable));
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<DespesaResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(despesaService.getById(id));
    }

    @GetMapping("/by-carteira/{carteiraId}")
    public ResponseEntity<List<DespesaResponseDTO>> getByCarteira(@PathVariable Long carteiraId) {
        return ResponseEntity.ok(despesaService.getByCarteira(carteiraId));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<Page<DespesaLogResponseDTO>> getLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(despesaService.getLogs(id, page, size));
    }

    @PostMapping("/create")
    public ResponseEntity<DespesaResponseDTO> create(@Valid @RequestBody DespesaRequestDTO dto) {
        return new ResponseEntity<>(despesaService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<DespesaResponseDTO> update(@PathVariable Long id, @Valid @RequestBody DespesaRequestDTO dto) {
        return ResponseEntity.ok(despesaService.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        despesaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
