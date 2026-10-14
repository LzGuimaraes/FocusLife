package dev.LzGuimaraes.FocusLifeHub.Musica;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/musicas")
public class MusicaController {
    private final MusicaService service;
    public MusicaController(MusicaService service) { this.service = service; }
    @GetMapping("/estilos")
    public List<MusicaDTO.Estilo> estilos() { return service.listarEstilos(); }
    @GetMapping("/estilos/{id}/sortear")
    public MusicaDTO.Video sortear(@PathVariable Long id, @RequestParam(required = false) Long anteriorId) {
        return service.sortear(id, anteriorId);
    }
}
