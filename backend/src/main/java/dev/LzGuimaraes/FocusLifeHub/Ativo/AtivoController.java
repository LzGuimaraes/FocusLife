package dev.LzGuimaraes.FocusLifeHub.Ativo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Ativo.dto.AtivoRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Ativo.dto.AtivoResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ativos")
public class AtivoController {

    private final AtivoService ativoService;

    public AtivoController(AtivoService ativoService) {
        this.ativoService = ativoService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<AtivoResponseDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ativoService.getAll(pageable));
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<AtivoResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ativoService.getById(id));
    }

    @GetMapping("/by-carteira/{carteiraId}")
    public ResponseEntity<List<AtivoResponseDTO>> getByCarteira(@PathVariable Long carteiraId) {
        return ResponseEntity.ok(ativoService.getByCarteira(carteiraId));
    }

    @PostMapping("/create")
    public ResponseEntity<AtivoResponseDTO> create(@Valid @RequestBody AtivoRequestDTO dto) {
        return new ResponseEntity<>(ativoService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<AtivoResponseDTO> update(@PathVariable Long id, @Valid @RequestBody AtivoRequestDTO dto) {
        return ResponseEntity.ok(ativoService.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ativoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Admin-only: delete all ativos (used by third-party sync)
    @DeleteMapping("/admin/delete-all")
    public ResponseEntity<Void> deleteAll() {
        ativoService.deleteAllAtivos();
        return ResponseEntity.noContent().build();
    }

    // Admin-only: bulk update prices for ativos
    @PostMapping("/admin/update-prices")
    public ResponseEntity<Void> bulkUpdatePrices(@RequestBody List<AtivoPriceUpdate> updates) {
        ativoService.bulkUpdatePrices(updates);
        return ResponseEntity.ok().build();
    }
}

class AtivoPriceUpdate {
    public Long ativoId;
    public Float precoAtual;
}
