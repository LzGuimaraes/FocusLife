package dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.dto.MetaAtivoRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.dto.MetaAtivoResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/metas-ativos")
public class MetaAtivoController {

    private final MetaAtivoService metaAtivoService;

    public MetaAtivoController(MetaAtivoService metaAtivoService) {
        this.metaAtivoService = metaAtivoService;
    }

    @GetMapping("/by-carteira/{carteiraId}")
    public ResponseEntity<List<MetaAtivoResponseDTO>> getByCarteira(@PathVariable Long carteiraId) {
        return ResponseEntity.ok(metaAtivoService.getByCarteira(carteiraId));
    }

    @PostMapping("/create")
    public ResponseEntity<MetaAtivoResponseDTO> create(@Valid @RequestBody MetaAtivoRequestDTO dto) {
        return new ResponseEntity<>(metaAtivoService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<MetaAtivoResponseDTO> update(@PathVariable Long id,
                                                      @Valid @RequestBody MetaAtivoRequestDTO dto) {
        return ResponseEntity.ok(metaAtivoService.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        metaAtivoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
