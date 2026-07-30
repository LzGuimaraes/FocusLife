package dev.LzGuimaraes.FocusLifeHub.Financas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Financas.dto.FinancasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Financas.dto.FinancasResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/financas")
public class FinancasController {

    private final FinancasService financasService;

    public FinancasController(FinancasService financasService) {
        this.financasService = financasService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<FinancasResponseDTO>> getAllFinancas(Pageable pageable) {
        Page<FinancasResponseDTO> financas = financasService.getAllFinancas(pageable);
        return ResponseEntity.ok(financas);
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<FinancasResponseDTO> getFinancaById(@PathVariable Long id) {
        FinancasResponseDTO financa = financasService.getFinancaById(id);
        return ResponseEntity.ok(financa);
    }

    @PostMapping("/create")
    public ResponseEntity<FinancasResponseDTO> createFinanca(@Valid @RequestBody FinancasRequestDTO dto) {
        FinancasResponseDTO createdFinanca = financasService.createFinanca(dto);
        return new ResponseEntity<>(createdFinanca, HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<FinancasResponseDTO> updateFinanca(
            @PathVariable Long id,
            @Valid @RequestBody FinancasRequestDTO dto) {
        FinancasResponseDTO updatedFinanca = financasService.updateFinanca(id, dto);
        return ResponseEntity.ok(updatedFinanca);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteFinanca(@PathVariable Long id) {
        financasService.deleteFinanca(id);
        return ResponseEntity.noContent().build();
    }
}