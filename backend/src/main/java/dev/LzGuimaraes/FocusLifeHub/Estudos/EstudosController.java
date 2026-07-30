package dev.LzGuimaraes.FocusLifeHub.Estudos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/estudos")
public class EstudosController {

    private final EstudosService estudosService;

    public EstudosController(EstudosService estudosService) {
        this.estudosService = estudosService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<EstudosResponseDTO>> getAllEstudos(Pageable pageable) {
        Page<EstudosResponseDTO> estudos = estudosService.getAllEstudos(pageable);
        return ResponseEntity.ok(estudos);
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<EstudosResponseDTO> getEstudosById(@PathVariable Long id) {
        EstudosResponseDTO estudo = estudosService.getEstudosById(id);
        return ResponseEntity.ok(estudo);
    }

    @PostMapping("/create")
    public ResponseEntity<EstudosResponseDTO> createEstudos(@Valid @RequestBody EstudosRequestDTO dto) {
        EstudosResponseDTO createdEstudo = estudosService.createEstudos(dto);
        return new ResponseEntity<>(createdEstudo, HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<EstudosResponseDTO> updateEstudos(
            @PathVariable Long id, 
            @Valid @RequestBody EstudosRequestDTO dto) {
        EstudosResponseDTO updatedEstudo = estudosService.updateEstudos(id, dto);
        return ResponseEntity.ok(updatedEstudo);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteEstudos(@PathVariable Long id) {
        estudosService.deleteEstudos(id);
        return ResponseEntity.noContent().build();
    }
}