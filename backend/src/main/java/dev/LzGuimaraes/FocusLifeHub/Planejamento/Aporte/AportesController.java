package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.AporteConfigDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.RankingAportesDTO;

/**
 * Ranking de prioridade de aporte (Módulos 6 e 9) e configuração do motor.
 *
 * `valor` é opcional: sem ele o endpoint devolve só a ordem de prioridade;
 * com ele, devolve também a sugestão de quanto iria para cada ativo.
 */
@RestController
@RequestMapping("/aportes")
public class AportesController {

    private final AporteService aporteService;
    private final AporteConfigService configService;

    public AportesController(AporteService aporteService, AporteConfigService configService) {
        this.aporteService = aporteService;
        this.configService = configService;
    }

    @GetMapping("/ranking")
    public ResponseEntity<RankingAportesDTO.Response> ranking(
            @RequestParam("carteira_investimento_id") Long carteiraId,
            @RequestParam(name = "valor", required = false) BigDecimal valor) {
        return ResponseEntity.ok(aporteService.ranking(carteiraId, valor));
    }

    /** Margem operacional do motor (3% a 5%, relativa à meta). */
    @GetMapping("/config")
    public ResponseEntity<AporteConfigDTO> config() {
        return ResponseEntity.ok(configService.obter());
    }

    @PutMapping("/config")
    public ResponseEntity<AporteConfigDTO> salvarConfig(@RequestBody MargemRequestDTO corpo) {
        return ResponseEntity.ok(configService.salvar((corpo != null) ? corpo.margem_percentual() : null));
    }

    /** Corpo do PUT: só a margem, em % (relativa à meta). */
    public record MargemRequestDTO(BigDecimal margem_percentual) {}
}
