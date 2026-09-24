package dev.LzGuimaraes.FocusLifeHub.Ativo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Ativo.dto.AtivoRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Ativo.dto.AtivoResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.dto.AtivoCadastroSyncDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ativos")
public class AtivoController {

    private final AtivoService ativoService;

    public AtivoController(AtivoService ativoService) {
        this.ativoService = ativoService;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<AtivoResponseDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ativoService.getAll(pageable));
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<AtivoResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ativoService.getById(id));
    }

    @GetMapping("/by-carteira/{carteiraId}")
    public ResponseEntity<List<AtivoResponseDTO>> getByCarteira(@PathVariable Long carteiraId) {
        return ResponseEntity.ok(ativoService.getByCarteira(carteiraId));
    }

    @PostMapping("/create")
    public ResponseEntity<AtivoResponseDTO> create(@Valid @RequestBody AtivoRequestDTO dto) {
        return new ResponseEntity<>(ativoService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<AtivoResponseDTO> update(@PathVariable Long id, @Valid @RequestBody AtivoRequestDTO dto) {
        return ResponseEntity.ok(ativoService.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ativoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Admin-only: delete all ativos (used by third-party sync)
    @DeleteMapping("/admin/delete-all")
    public ResponseEntity<Void> deleteAll() {
        ativoService.deleteAllAtivos();
        return ResponseEntity.noContent().build();
    }

    /**
     * Vincula posições existentes a um ativo do catálogo. Usado pela tela de
     * metas da Carteira Ideal para resolver posições antigas que ficaram sem
     * `ativo_cadastro_id` (e por isso não podiam ter meta individual).
     */
    @PostMapping("/vincular-catalogo")
    public ResponseEntity<Map<String, Integer>> vincularCatalogo(@RequestBody VincularCatalogoRequest body) {
        int vinculadas = ativoService.vincularCatalogo(
                (body != null) ? body.ativo_ids : null,
                (body != null) ? body.ativo_cadastro_id : null);
        return ResponseEntity.ok(Map.of("vinculadas", vinculadas));
    }

    /**
     * Classifica posições SEM ticker (renda fixa, Tesouro, caixinhas) em uma
     * subclasse da Carteira Ideal. A meta é o percentual da subclasse: com a
     * posição atribuída, ela passa a contar para o alvo e para a prioridade de
     * aporte — coisa que não era possível quando a meta exigia um ticker.
     */
    @PostMapping("/atribuir-subclasse")
    public ResponseEntity<Map<String, Integer>> atribuirSubclasse(@RequestBody AtribuirSubclasseRequest body) {
        int classificadas = ativoService.atribuirSubclasse(
                (body != null) ? body.ativo_ids : null,
                (body != null) ? body.subclasse_id : null);
        return ResponseEntity.ok(Map.of("classificadas", classificadas));
    }

    /**
     * Classifica posições em um SETOR (nível opcional dentro da subclasse).
     * Corpo com `setor_id: null` remove a classificação de setor.
     */
    @PostMapping("/atribuir-setor")
    public ResponseEntity<Map<String, Integer>> atribuirSetor(@RequestBody AtribuirSetorRequest body) {
        int classificadas = ativoService.atribuirSetor(
                (body != null) ? body.ativo_ids : null,
                (body != null) ? body.setor_id : null);
        return ResponseEntity.ok(Map.of("classificadas", classificadas));
    }


    // Admin-only: bulk update prices for ativos
    @PostMapping("/admin/update-prices")
    public ResponseEntity<Void> bulkUpdatePrices(@RequestBody List<AtivoPriceUpdate> updates) {
        ativoService.bulkUpdatePrices(updates);
        return ResponseEntity.ok().build();
    }

    // Admin-only: sync catálogo de ativos (upsert into ativo_cadastro) e
    // propaga o novo preço para as posições (cards) que usam cada ativo.
    @PostMapping("/admin/sync")
    public ResponseEntity<Map<String, Integer>> syncAtivos(@RequestBody List<AtivoCadastroSyncDTO> payload) {
        return ResponseEntity.ok(ativoService.syncCatalogo(payload));
    }
}

class AtivoPriceUpdate {
    public Long ativoId;
    public Float precoAtual;
}

/** Corpo do vínculo de posições a um ativo do catálogo. */
class VincularCatalogoRequest {
    public List<Long> ativo_ids;
    public UUID ativo_cadastro_id;
}

/** Corpo da classificação de posições em uma subclasse da Carteira Ideal. */
class AtribuirSubclasseRequest {
    public List<Long> ativo_ids;
    public Long subclasse_id;
}

/** Corpo da classificação de posições em um setor da subclasse. */
class AtribuirSetorRequest {
    public List<Long> ativo_ids;
    public Long setor_id;
}
