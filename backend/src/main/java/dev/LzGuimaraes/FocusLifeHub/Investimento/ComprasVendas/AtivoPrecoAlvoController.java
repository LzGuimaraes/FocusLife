package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import java.util.List;

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

import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto.AtivoPrecoAlvoRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto.AtivoPrecoAlvoResponseDTO;

/**
 * Regras de preço teto/mínimo (por usuário+ativo).
 *
 *   GET    /precos-alvo/all
 *   POST   /precos-alvo/create
 *   PUT    /precos-alvo/alter/{id}
 *   DELETE /precos-alvo/delete/{id}
 *   POST   /precos-alvo/upsert   ← cria ou atualiza a regra do mesmo ativo
 */
@RestController
@RequestMapping("/precos-alvo")
public class AtivoPrecoAlvoController {

    private final AtivoPrecoAlvoService service;

    public AtivoPrecoAlvoController(AtivoPrecoAlvoService service) {
        this.service = service;
    }

    @GetMapping("/all")
    public ResponseEntity<List<AtivoPrecoAlvoResponseDTO>> getAll() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping("/create")
    public ResponseEntity<AtivoPrecoAlvoResponseDTO> create(@RequestBody AtivoPrecoAlvoRequestDTO dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<AtivoPrecoAlvoResponseDTO> update(@PathVariable Long id,
                                                            @RequestBody AtivoPrecoAlvoRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PostMapping("/upsert")
    public ResponseEntity<AtivoPrecoAlvoResponseDTO> upsert(@RequestBody AtivoPrecoAlvoRequestDTO dto) {
        return ResponseEntity.ok(service.upsert(dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
