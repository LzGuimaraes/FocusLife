package dev.LzGuimaraes.FocusLifeHub.Carteira;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Carteira.dto.DiagnosticoFinanceiroDTO;
import dev.LzGuimaraes.FocusLifeHub.Carteira.dto.DiagnosticoFinanceiroDTO.ReparoRequest;
import dev.LzGuimaraes.FocusLifeHub.Carteira.dto.DiagnosticoFinanceiroDTO.ReparoResultadoDTO;

/**
 * Diagnóstico dos dados financeiros e reparo das linhas legadas sem carteira.
 *
 * O GET é liberado para qualquer usuário autenticado (mostra SÓ as carteiras
 * dele; as contagens de órfãos só vêm preenchidas quando a atribuição é
 * inequívoca). O reparo é permitido a ADMIN ou quando a instalação tem um único
 * usuário — ver {@link DiagnosticoFinanceiroService}.
 */
@RestController
@RequestMapping("/financeiro/diagnostico")
public class DiagnosticoFinanceiroController {

    private final DiagnosticoFinanceiroService diagnosticoService;

    public DiagnosticoFinanceiroController(DiagnosticoFinanceiroService diagnosticoService) {
        this.diagnosticoService = diagnosticoService;
    }

    @GetMapping
    public ResponseEntity<DiagnosticoFinanceiroDTO> diagnostico() {
        return ResponseEntity.ok(diagnosticoService.diagnostico());
    }

    /** Religa posições/despesas sem carteira. Corpo vazio = destino automático. */
    @PostMapping("/reparar")
    public ResponseEntity<ReparoResultadoDTO> reparar(@RequestBody(required = false) ReparoRequest body) {
        Long investimento = (body != null) ? body.carteira_investimento_id() : null;
        Long dividas = (body != null) ? body.carteira_dividas_id() : null;
        return ResponseEntity.ok(diagnosticoService.reparar(investimento, dividas));
    }
}
