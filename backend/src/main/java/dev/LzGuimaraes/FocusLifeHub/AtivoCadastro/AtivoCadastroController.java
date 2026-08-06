package dev.LzGuimaraes.FocusLifeHub.AtivoCadastro;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.dto.AtivoCadastroResponseDTO;

@RestController
@RequestMapping("/ativos-cadastrados")
public class AtivoCadastroController {

    private final AtivoCadastroService ativoCadastroService;

    public AtivoCadastroController(AtivoCadastroService ativoCadastroService) {
        this.ativoCadastroService = ativoCadastroService;
    }

    @GetMapping
    public ResponseEntity<List<AtivoCadastroResponseDTO>> listar(
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(ativoCadastroService.listar(q));
    }
}
