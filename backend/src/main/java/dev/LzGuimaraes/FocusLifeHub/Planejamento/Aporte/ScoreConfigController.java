package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.ScoreConfigDTO;
import jakarta.validation.Valid;

/**
 * Configuração da fórmula de prioridade de aporte (Módulo 6).
 *
 * A fórmula é fixa e transparente (α·Qualidade + β·Déficit − γ·Excesso +
 * δ·Prioridade); o usuário configura os pesos e a estratégia de rateio.
 * `GET /termos` devolve o que cada termo significa.
 */
@RestController
@RequestMapping("/score-config")
public class ScoreConfigController {

    private final ScoreConfigService scoreConfigService;

    public ScoreConfigController(ScoreConfigService scoreConfigService) {
        this.scoreConfigService = scoreConfigService;
    }

    @GetMapping
    public ResponseEntity<ScoreConfigDTO.Response> get() {
        return ResponseEntity.ok(scoreConfigService.get());
    }

    @GetMapping("/termos")
    public ResponseEntity<List<ScoreConfigDTO.Termo>> termos() {
        return ResponseEntity.ok(scoreConfigService.termos());
    }

    @PutMapping
    public ResponseEntity<ScoreConfigDTO.Response> salvar(@Valid @RequestBody ScoreConfigDTO.Request dto) {
        return ResponseEntity.ok(scoreConfigService.salvar(dto));
    }

    /** Volta aos pesos padrão (não deixa "configuração pela metade"). */
    @DeleteMapping
    public ResponseEntity<ScoreConfigDTO.Response> restaurarPadroes() {
        return ResponseEntity.ok(scoreConfigService.restaurarPadroes());
    }
}
