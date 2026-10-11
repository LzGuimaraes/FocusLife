package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto.OperacaoCompraVendaRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto.OperacaoCompraVendaResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto.ResumoComprasVendasDTO;
import jakarta.validation.Valid;

/**
 * Operações de COMPRA/VENDA (lucro/prejuízo por operação) + resumo do gráfico.
 *
 *   GET    /compras-vendas/all?carteira_investimento_id=&de=&ate=&page=&size=
 *   GET    /compras-vendas/all/{id}
 *   GET    /compras-vendas/resumo?carteira_investimento_id=&de=&ate=
 *   POST   /compras-vendas/create
 *   PUT    /compras-vendas/alter/{id}
 *   DELETE /compras-vendas/delete/{id}
 */
@RestController
@RequestMapping("/compras-vendas")
public class OperacaoCompraVendaController {

    private final OperacaoCompraVendaService service;

    public OperacaoCompraVendaController(OperacaoCompraVendaService service) {
        this.service = service;
    }

    @GetMapping("/all")
    public ResponseEntity<Page<OperacaoCompraVendaResponseDTO>> getAll(
            @RequestParam(name = "carteira_investimento_id", required = false) Long carteiraId,
            @RequestParam(name = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            Pageable pageable) {
        return ResponseEntity.ok(service.getAll(carteiraId, de, ate, pageable));
    }

    @GetMapping("/all/{id}")
    public ResponseEntity<OperacaoCompraVendaResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/resumo")
    public ResponseEntity<ResumoComprasVendasDTO> resumo(
            @RequestParam(name = "carteira_investimento_id", required = false) Long carteiraId,
            @RequestParam(name = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(service.resumo(carteiraId, de, ate));
    }

    @PostMapping("/create")
    public ResponseEntity<OperacaoCompraVendaResponseDTO> create(
            @Valid @RequestBody OperacaoCompraVendaRequestDTO dto) {
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public ResponseEntity<OperacaoCompraVendaResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody OperacaoCompraVendaRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
