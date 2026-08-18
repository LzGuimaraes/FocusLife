package dev.LzGuimaraes.FocusLifeHub.Ativo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.Ativo.dto.AtivoRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Ativo.dto.AtivoResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroRepository;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.TipoAtivoCadastro;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.dto.AtivoCadastroSyncDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ativos")
public class AtivoController {
    private static final Logger log = LoggerFactory.getLogger(AtivoController.class);

    private final AtivoService ativoService;
    private final AtivoCadastroRepository ativoCadastroRepository;

    public AtivoController(AtivoService ativoService, AtivoCadastroRepository ativoCadastroRepository) {
        this.ativoService = ativoService;
        this.ativoCadastroRepository = ativoCadastroRepository;
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

    // Admin-only: bulk update prices for ativos
    @PostMapping("/admin/update-prices")
    public ResponseEntity<Void> bulkUpdatePrices(@RequestBody List<AtivoPriceUpdate> updates) {
        ativoService.bulkUpdatePrices(updates);
        return ResponseEntity.ok().build();
    }

    // Admin-only: sync catálogo de ativos (upsert into ativo_cadastro)
    @PostMapping("/admin/sync")
    public ResponseEntity<Map<String, Integer>> syncAtivos(@RequestBody List<AtivoCadastroSyncDTO> payload) {
        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("received", 0, "created", 0, "updated", 0, "invalid", 0));
        }

        int received = payload.size();
        int created = 0;
        int updated = 0;
        int invalid = 0;

        for (AtivoCadastroSyncDTO dto : payload) {
            if (dto == null || dto.getNome() == null || dto.getNome().isBlank() || dto.getTipo() == null) {
                invalid++;
                continue;
            }

            String nome = dto.getNome().trim();
            String tipoStr = dto.getTipo().trim();
            TipoAtivoCadastro tipo;
            try {
                tipo = TipoAtivoCadastro.valueOf(tipoStr);
            } catch (Exception ex) {
                try { tipo = TipoAtivoCadastro.valueOf(tipoStr.toUpperCase()); }
                catch (Exception ex2) { invalid++; log.warn("Tipo inválido para ativo '{}' : {}", nome, tipoStr); continue; }
            }

            Optional<AtivoCadastroModel> opt = ativoCadastroRepository.findByNomeIgnoreCase(nome);
            if (opt.isPresent()) {
                AtivoCadastroModel existing = opt.get();
                boolean changed = false;
                if (dto.getPrecoAtual() != null && !dto.getPrecoAtual().equals(existing.getPrecoAtual())) {
                    existing.setPrecoAtual(dto.getPrecoAtual());
                    changed = true;
                }
                if (existing.getTipo() == null || !existing.getTipo().equals(tipo)) {
                    existing.setTipo(tipo);
                    changed = true;
                }
                if (changed) {
                    ativoCadastroRepository.save(existing);
                    updated++;
                }
            } else {
                AtivoCadastroModel novo = new AtivoCadastroModel();
                novo.setNome(nome);
                novo.setTipo(tipo);
                novo.setPrecoAtual(dto.getPrecoAtual());
                ativoCadastroRepository.save(novo);
                created++;
            }
        }

        log.info("/ativos/admin/sync: received={}, created={}, updated={}, invalid={}", received, created, updated, invalid);
        return ResponseEntity.ok(Map.of("received", received, "created", created, "updated", updated, "invalid", invalid));
    }
}

class AtivoPriceUpdate {
    public Long ativoId;
    public Float precoAtual;
}
