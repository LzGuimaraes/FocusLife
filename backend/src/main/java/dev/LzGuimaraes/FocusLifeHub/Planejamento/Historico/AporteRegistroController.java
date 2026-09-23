package dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.dto.AporteRegistroDTO;
import jakarta.validation.Valid;

/**
 * Histórico de aportes executados (§24).
 *
 *   POST   /aportes/registrar                 → lança o aporte (em lote)
 *   GET    /aportes/historico?carteira_investimento_id=…  → lista
 *   DELETE /aportes/historico/{id}            → desfaz um lançamento
 */
@RestController
@RequestMapping("/aportes")
public class AporteRegistroController {

    private final AporteRegistroService service;

    public AporteRegistroController(AporteRegistroService service) {
        this.service = service;
    }

    @PostMapping("/registrar")
    public ResponseEntity<AporteRegistroDTO.Resultado> registrar(
            @Valid @RequestBody AporteRegistroDTO.Request dto) {
        return ResponseEntity.ok(service.registrar(dto));
    }

    @GetMapping("/historico")
    public ResponseEntity<List<AporteRegistroDTO.Response>> historico(
            @RequestParam(name = "carteira_investimento_id") Long carteiraId) {
        return ResponseEntity.ok(service.listar(carteiraId));
    }

    @DeleteMapping("/historico/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
