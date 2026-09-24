package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.util.List;

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
 *   GET /notas-subclasse/buckets?carteira_investimento_id=2  → onde dá para avaliar
 *   GET /notas-subclasse/{slug}?nome=Financeiro               → perguntas + notas
 *   PUT /notas-subclasse/{slug}                               → grava as notas
 *
 * `slug` é o nome normalizado do balde ("Financeiro" → "financeiro"), o mesmo
 * vínculo usado no padrão do checklist: assim o checklist sobrevive ao save da
 * Carteira Ideal, que recria as subclasses. Para as classes que NÃO usam
 * subclasse (cripto, renda fixa, Tesouro), o balde é a própria CLASSE e o slug é
 * o nome dela ("criptomoedas") — é o que faz esses ativos virem com o checklist
 * padrão do tipo deles.
 */
@RestController
@RequestMapping("/notas-subclasse")
public class NotasSubclasseController {

    private final NotasSubclasseService notasSubclasseService;

    public NotasSubclasseController(NotasSubclasseService notasSubclasseService) {
        this.notasSubclasseService = notasSubclasseService;
    }

    /** Baldes de avaliação da carteira (subclasses + classes sem subclasse). */
    @GetMapping("/buckets")
    public ResponseEntity<List<NotasSubclasseDTO.Bucket>> buckets(
            @RequestParam(name = "carteira_investimento_id", required = false) Long carteiraId) {
        return ResponseEntity.ok(notasSubclasseService.buckets(carteiraId));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<NotasSubclasseDTO.Painel> painel(
            @PathVariable String slug,
            @RequestParam(name = "nome", required = false) String nome,
            @RequestParam(name = "carteira_investimento_id", required = false) Long carteiraId) {
        return ResponseEntity.ok(notasSubclasseService.painel(nome, slug, carteiraId));
    }

    @PutMapping("/{slug}")
    public ResponseEntity<NotasSubclasseDTO.SalvarResponse> salvar(
            @PathVariable String slug,
            @RequestParam(name = "nome", required = false) String nome,
            @RequestParam(name = "carteira_investimento_id", required = false) Long carteiraId,
            @Valid @RequestBody NotasSubclasseDTO.SalvarRequest req) {
        return ResponseEntity.ok(notasSubclasseService.salvar(nome, slug, carteiraId, req));
    }
}
