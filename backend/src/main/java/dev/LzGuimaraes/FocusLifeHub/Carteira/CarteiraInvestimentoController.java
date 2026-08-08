package dev.LzGuimaraes.FocusLifeHub.Carteira;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Carteira.dto.CarteiraRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Carteira.dto.CarteiraResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/carteiras-investimento")
public class CarteiraInvestimentoController {

    private final CarteiraInvestimentoService carteiraInvestimentoService;

    public CarteiraInvestimentoController(CarteiraInvestimentoService carteiraInvestimentoService) {
        this.carteiraInvestimentoService = carteiraInvestimentoService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<CarteiraResponseDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(carteiraInvestimentoService.getAll(pageable));
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<CarteiraResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(carteiraInvestimentoService.getById(id));
    }

    @PostMapping("/create")
    public ResponseEntity<CarteiraResponseDTO> create(@Valid @RequestBody CarteiraRequestDTO dto) {
        CarteiraResponseDTO created = carteiraInvestimentoService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/duplicar/{id}")
    public ResponseEntity<CarteiraResponseDTO> duplicate(@PathVariable Long id) {
        CarteiraResponseDTO duplicated = carteiraInvestimentoService.duplicate(id);
        return new ResponseEntity<>(duplicated, HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<CarteiraResponseDTO> update(@PathVariable Long id, @Valid @RequestBody CarteiraRequestDTO dto) {
        return ResponseEntity.ok(carteiraInvestimentoService.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carteiraInvestimentoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
