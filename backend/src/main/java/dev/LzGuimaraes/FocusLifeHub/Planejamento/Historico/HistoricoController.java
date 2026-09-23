package dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.dto.HistoricoDTO;
import jakarta.validation.Valid;

/**
 * Histórico de avaliações (Módulo 8).
 *
 * `POST /registrar` grava o retrato do dia para cada ativo com avaliação
 * (idempotente no dia: reavaliar e registrar de novo ATUALIZA a linha).
 */
@RestController
@RequestMapping("/historico")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @PostMapping("/registrar")
    public ResponseEntity<HistoricoDTO.RegistrarResponse> registrar(
            @Valid @RequestBody(required = false) HistoricoDTO.RegistrarRequest request) {
        return new ResponseEntity<>(historicoService.registrar(request), HttpStatus.CREATED);
    }

    /** Ativos que já possuem histórico. */
    @GetMapping("/ativos")
    public ResponseEntity<List<HistoricoDTO.AtivoComHistorico>> ativos() {
        return ResponseEntity.ok(historicoService.ativos());
    }

    /** Evolução do score de um ativo (por ticker do catálogo ou por posição). */
    @GetMapping("/serie")
    public ResponseEntity<HistoricoDTO.SerieAtivo> serie(
            @RequestParam(name = "ativo_cadastro_id", required = false) UUID ativoCadastroId,
            @RequestParam(name = "ativo_id", required = false) Long ativoId,
            @RequestParam(name = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(historicoService.serieAtivo(ativoCadastroId, ativoId, de, ate));
    }

    /** Evolução agregada da carteira (qualidade média, déficit, excesso, valor total). */
    @GetMapping("/carteira/{carteiraId}")
    public ResponseEntity<HistoricoDTO.SerieCarteira> serieCarteira(
            @PathVariable Long carteiraId,
            @RequestParam(name = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(historicoService.serieCarteira(carteiraId, de, ate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        historicoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
