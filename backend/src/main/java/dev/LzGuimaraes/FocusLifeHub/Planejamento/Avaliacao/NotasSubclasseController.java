package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.NotasSubclasseDTO;
import jakarta.validation.Valid;

/**
 * NOTAS POR SUBCLASSE — uma página, um checklist, uma nota por ativo.
 *
 *   GET /notas-subclasse/{slug}?nome=Financeiro  → perguntas + notas já dadas
 *   PUT /notas-subclasse/{slug}                  → grava as notas da página
 *
 * `slug` é o nome normalizado da subclasse ("Financeiro" → "financeiro"), o mesmo
 * vínculo usado no padrão do checklist: assim o checklist sobrevive ao save da
 * Carteira Ideal, que recria as subclasses.
 */
@RestController
@RequestMapping("/notas-subclasse")
public class NotasSubclasseController {

    private final NotasSubclasseService notasSubclasseService;

    public NotasSubclasseController(NotasSubclasseService notasSubclasseService) {
        this.notasSubclasseService = notasSubclasseService;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<NotasSubclasseDTO.Painel> painel(@PathVariable String slug,
                                                           @RequestParam(name = "nome", required = false) String nome) {
        return ResponseEntity.ok(notasSubclasseService.painel(nome, slug));
    }

    @PutMapping("/{slug}")
    public ResponseEntity<NotasSubclasseDTO.SalvarResponse> salvar(@PathVariable String slug,
                                                                   @RequestParam(name = "nome", required = false) String nome,
                                                                   @Valid @RequestBody NotasSubclasseDTO.SalvarRequest req) {
        return ResponseEntity.ok(notasSubclasseService.salvar(nome, slug, req));
    }
}
