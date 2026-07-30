package dev.LzGuimaraes.FocusLifeHub.Materia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.LzGuimaraes.FocusLifeHub.Materia.dto.MateriaRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Materia.dto.MateriaResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/materias")
public class MateriaController {

    private MateriaService materiaService;

    public MateriaController(MateriaService materiaService){
        this.materiaService = materiaService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<MateriaResponseDTO>> getAllMaterias(Pageable pageable) {
        Page<MateriaResponseDTO> materias = materiaService.getAllMaterias(pageable);
        return ResponseEntity.ok(materias);
    }

    @GetMapping("/all/{id}")
    public MateriaResponseDTO getMateriaById(@PathVariable Long id) {
        return materiaService.getMateriaById(id);
    }

    @PostMapping("create")
    public ResponseEntity<MateriaResponseDTO> createMateria(@RequestBody @Valid MateriaRequestDTO materia) {
        MateriaResponseDTO createdMateria = materiaService.createMateria(materia);
        return new ResponseEntity<>(createdMateria,HttpStatus.CREATED);
    }

    @PutMapping("alter/{id}")
    public MateriaResponseDTO alterarMateria(@PathVariable Long id, @Valid @RequestBody MateriaRequestDTO updatedMateria) {
        return materiaService.alterarMateria(id, updatedMateria);
    }

    @DeleteMapping("delete/{id}")
    public void deleteMateria(@PathVariable Long id) {
        materiaService.deleteMateria(id);
    }
}
