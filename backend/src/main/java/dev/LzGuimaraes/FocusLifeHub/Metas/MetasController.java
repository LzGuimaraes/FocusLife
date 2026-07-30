package dev.LzGuimaraes.FocusLifeHub.Metas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Metas.dto.MetasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Metas.dto.MetasResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/metas")
public class MetasController {

    private final MetasService metasService;

    public MetasController(MetasService metasService) {
        this.metasService = metasService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<MetasResponseDTO>> getAllMetas(Pageable pageable) {
        Page<MetasResponseDTO> metas = metasService.getAllMetas(pageable);
        return ResponseEntity.ok(metas);
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<MetasResponseDTO> getMetaById(@PathVariable Long id) {
        MetasResponseDTO meta = metasService.getMetaById(id);
        return ResponseEntity.ok(meta);
    }

    @PostMapping("/create")
    public ResponseEntity<MetasResponseDTO> createMeta(@Valid @RequestBody MetasRequestDTO dto) {
        MetasResponseDTO createdMeta = metasService.createMeta(dto);
        return new ResponseEntity<>(createdMeta, HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<MetasResponseDTO> updateMeta(
            @PathVariable Long id,
            @Valid @RequestBody MetasRequestDTO dto) {
        MetasResponseDTO updatedMeta = metasService.updateMeta(id, dto);
        return ResponseEntity.ok(updatedMeta);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteMeta(@PathVariable Long id) {
        metasService.deleteMeta(id);
        return ResponseEntity.noContent().build();
    }
}