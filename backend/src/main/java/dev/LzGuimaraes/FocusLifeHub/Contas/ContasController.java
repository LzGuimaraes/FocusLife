package dev.LzGuimaraes.FocusLifeHub.Contas;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContasResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/contas")
public class ContasController {

    private final ContasService contasService;

    public ContasController(ContasService contasService) {
        this.contasService = contasService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<ContasResponseDTO>> getAllContas(Pageable pageable) {
        Page<ContasResponseDTO> contas = contasService.getAllContas(pageable);
        return ResponseEntity.ok(contas);
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<ContasResponseDTO> getContaById(@PathVariable Long id) {
        ContasResponseDTO conta = contasService.getContaById(id);
        return ResponseEntity.ok(conta);
    }

    @GetMapping("/by-financa/{financasId}")
    public ResponseEntity<List<ContasResponseDTO>> getContasByFinancaId(@PathVariable Long financasId) {
        List<ContasResponseDTO> contas = contasService.getContasByFinancaId(financasId);
        return ResponseEntity.ok(contas);
    }

    @PostMapping("/create")
    public ResponseEntity<ContasResponseDTO> createConta(@Valid @RequestBody ContasRequestDTO dto) {
        ContasResponseDTO createdConta = contasService.createConta(dto);
        return new ResponseEntity<>(createdConta, HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<ContasResponseDTO> updateConta(
            @PathVariable Long id,
            @Valid @RequestBody ContasRequestDTO dto) {
        ContasResponseDTO updatedConta = contasService.updateConta(id, dto);
        return ResponseEntity.ok(updatedConta);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteConta(@PathVariable Long id) {
        contasService.deleteConta(id);
        return ResponseEntity.noContent().build();
    }
}