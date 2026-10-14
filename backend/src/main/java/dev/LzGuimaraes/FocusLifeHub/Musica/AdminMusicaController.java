package dev.LzGuimaraes.FocusLifeHub.Musica;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Todas as alterações passam por /admin/**, protegido pelo SecurityConfig. */
@RestController
@RequestMapping("/admin/musicas")
public class AdminMusicaController {
    private final MusicaService service;
    public AdminMusicaController(MusicaService service) { this.service = service; }
    @GetMapping("/estilos")
    public List<MusicaDTO.Estilo> estilos() { return service.listarEstilos(); }
    @PostMapping("/estilos") @ResponseStatus(HttpStatus.CREATED)
    public MusicaDTO.Estilo criar(@Valid @RequestBody MusicaDTO.EstiloRequest request) { return service.salvarEstilo(null, request.nome()); }
    @PutMapping("/estilos/{id}")
    public MusicaDTO.Estilo editar(@PathVariable Long id, @Valid @RequestBody MusicaDTO.EstiloRequest request) { return service.salvarEstilo(id, request.nome()); }
    @DeleteMapping("/estilos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirEstilo(@PathVariable Long id) { service.excluirEstilo(id); }
    @GetMapping("/estilos/{id}/videos")
    public List<MusicaDTO.Video> videos(@PathVariable Long id) { return service.listarVideos(id); }
    @PostMapping("/estilos/{id}/videos") @ResponseStatus(HttpStatus.CREATED)
    public MusicaDTO.Adicao adicionar(@PathVariable Long id, @Valid @RequestBody MusicaDTO.VideosRequest request) { return service.adicionarVideos(id, request.urls()); }
    @PutMapping("/videos/{id}")
    public MusicaDTO.Video editarVideo(@PathVariable Long id, @Valid @RequestBody MusicaDTO.VideoRequest request) { return service.editarVideo(id, request.url()); }
    @DeleteMapping("/videos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirVideo(@PathVariable Long id) { service.excluirVideo(id); }
}
