package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.RankingAportesDTO;

/**
 * Ranking de prioridade de aporte (Módulos 6 e 9).
 *
 * `valor` é opcional: sem ele o endpoint devolve só a ordem de prioridade;
 * com ele, devolve também a sugestão de quanto iria para cada ativo.
 */
@RestController
@RequestMapping("/aportes")
public class AportesController {

    private final AporteService aporteService;

    public AportesController(AporteService aporteService) {
        this.aporteService = aporteService;
    }

    @GetMapping("/ranking")
    public ResponseEntity<RankingAportesDTO.Response> ranking(
            @RequestParam("carteira_investimento_id") Long carteiraId,
            @RequestParam(name = "valor", required = false) BigDecimal valor) {
        return ResponseEntity.ok(aporteService.ranking(carteiraId, valor));
    }
}
