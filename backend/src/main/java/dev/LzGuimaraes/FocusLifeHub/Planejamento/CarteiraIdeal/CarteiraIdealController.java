package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.CarteiraIdealRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.CarteiraIdealResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.MeusAtivosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ResumoIdealDTO;
import jakarta.validation.Valid;

/**
 * Carteira Ideal de uma carteira de investimento (Módulo 1) e o comparativo
 * com a carteira real (Módulo 10).
 *
 * Compartilha o prefixo /carteiras-investimento com o CRUD de carteiras: as
 * rotas são disjuntas (/all/**, /create, /alter/**, /delete/** versus
 * /{id}/ideal/**).
 */
@RestController
@RequestMapping("/carteiras-investimento")
public class CarteiraIdealController {

    private final CarteiraIdealService carteiraIdealService;

    public CarteiraIdealController(CarteiraIdealService carteiraIdealService) {
        this.carteiraIdealService = carteiraIdealService;
    }

    /** Configuração salva (classes, subclasses, metas, avisos). */
    @GetMapping("/{carteiraId}/ideal")
    public ResponseEntity<CarteiraIdealResponseDTO> get(@PathVariable Long carteiraId) {
        return ResponseEntity.ok(carteiraIdealService.get(carteiraId));
    }

    /**
     * Salva a configuração completa (REPLACE-ALL transacional).
     * 400 quando a soma das classes ≠ 100% ou há referência inválida.
     */
    @PutMapping("/{carteiraId}/ideal")
    public ResponseEntity<CarteiraIdealResponseDTO> save(@PathVariable Long carteiraId,
                                                        @Valid @RequestBody CarteiraIdealRequestDTO dto) {
        return ResponseEntity.ok(carteiraIdealService.save(carteiraId, dto));
    }

    /** Comparativo Carteira Atual × Carteira Ideal, por classe/subclasse/ativo. */
    @GetMapping("/{carteiraId}/ideal/comparativo")
    public ResponseEntity<ComparativoResponseDTO> comparativo(@PathVariable Long carteiraId) {
        return ResponseEntity.ok(carteiraIdealService.comparativo(carteiraId));
    }

    /**
     * Ativos que o usuário JÁ TEM nesta carteira (com o percentual atual e a
     * meta, se houver). É a base da tela de metas: o planejamento parte da
     * carteira real, sem o usuário recadastrar os ativos.
     */
    @GetMapping("/{carteiraId}/ideal/meus-ativos")
    public ResponseEntity<MeusAtivosResponseDTO> meusAtivos(@PathVariable Long carteiraId) {
        return ResponseEntity.ok(carteiraIdealService.meusAtivos(carteiraId));
    }

    /** Resumo por classe para o widget do Dashboard. */
    @GetMapping("/{carteiraId}/ideal/resumo")
    public ResponseEntity<ResumoIdealDTO> resumo(@PathVariable Long carteiraId) {
        return ResponseEntity.ok(carteiraIdealService.resumo(carteiraId));
    }
}
