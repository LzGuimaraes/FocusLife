package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;
import jakarta.validation.Valid;
import static dev.LzGuimaraes.FocusLifeHub.Jornadas.JornadasDTO.*;
@RestController @RequestMapping("/jornadas")
public class JornadasController {
    private final JornadasService service;
    public JornadasController(JornadasService service) {this.service=service;}
    @GetMapping public List<Resposta> listar(@AuthenticationPrincipal JWTUserData u) {return service.listar(u.userId());}
    @GetMapping("/{id}") public Resposta buscar(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id) {return service.buscar(u.userId(),id);}
    @PostMapping public Resposta criar(@AuthenticationPrincipal JWTUserData u,@Valid @RequestBody Pedido p) {return service.salvar(u.userId(),null,p);}
    @PutMapping("/{id}") public Resposta editar(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@Valid @RequestBody Pedido p) {return service.salvar(u.userId(),id,p);}
    @DeleteMapping("/{id}") public void excluir(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@RequestParam long version) {service.excluir(u.userId(),id,version);}
    @PostMapping("/{id}/progresso") public Resposta lancar(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@Valid @RequestBody Lancamento p) {return service.lancar(u.userId(),id,null,p);}
    @PutMapping("/{id}/progresso/{progressoId}") public Resposta corrigir(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@PathVariable Long progressoId,@Valid @RequestBody Lancamento p) {return service.lancar(u.userId(),id,progressoId,p);}
    @DeleteMapping("/{id}/progresso/{progressoId}") public Resposta excluirProgresso(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@PathVariable Long progressoId,@RequestParam long version) {return service.excluirProgresso(u.userId(),id,progressoId,version);}
    @GetMapping("/materiais") public List<Material> links(@AuthenticationPrincipal JWTUserData u) {return service.links(u.userId());}
    @PostMapping("/materiais") public Material link(@AuthenticationPrincipal JWTUserData u,@Valid @RequestBody LinkPedido p) {return service.salvarLink(u.userId(),null,p);}
    @PutMapping("/materiais/{id}") public Material editarLink(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@Valid @RequestBody LinkPedido p) {return service.salvarLink(u.userId(),id,p);}
    @DeleteMapping("/materiais/{id}") public void excluirLink(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@RequestParam long version) {service.excluirLink(u.userId(),id,version);}
    @PostMapping("/{id}/sessoes") public Resposta iniciar(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id) {return service.iniciar(u.userId(),id);}
    @PutMapping("/{id}/sessoes/{sessaoId}/pausar") public Resposta pausar(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@PathVariable Long sessaoId) {return service.pausar(u.userId(),id,sessaoId);}
    @PostMapping("/{id}/estudos") public Resposta anotar(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@Valid @RequestBody EstudoPedido p) {return service.anotar(u.userId(),id,null,p);}
    @PutMapping("/{id}/estudos/{notaId}") public Resposta editarEstudo(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@PathVariable Long notaId,@Valid @RequestBody EstudoPedido p) {return service.anotar(u.userId(),id,notaId,p);}
    @DeleteMapping("/{id}/estudos/{notaId}") public Resposta excluirEstudo(@AuthenticationPrincipal JWTUserData u,@PathVariable Long id,@PathVariable Long notaId) {return service.excluirEstudo(u.userId(),id,notaId);}
}
