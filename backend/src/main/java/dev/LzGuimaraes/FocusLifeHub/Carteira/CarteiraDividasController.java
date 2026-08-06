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
@RequestMapping("/carteiras-dividas")
public class CarteiraDividasController {

    private final CarteiraDividasService carteiraDividasService;

    public CarteiraDividasController(CarteiraDividasService carteiraDividasService) {
        this.carteiraDividasService = carteiraDividasService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<CarteiraResponseDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(carteiraDividasService.getAll(pageable));
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<CarteiraResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(carteiraDividasService.getById(id));
    }

    @PostMapping("/create")
    public ResponseEntity<CarteiraResponseDTO> create(@Valid @RequestBody CarteiraRequestDTO dto) {
        CarteiraResponseDTO created = carteiraDividasService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<CarteiraResponseDTO> update(@PathVariable Long id, @Valid @RequestBody CarteiraRequestDTO dto) {
        return ResponseEntity.ok(carteiraDividasService.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carteiraDividasService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
