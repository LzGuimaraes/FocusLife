package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
@RestController @RequestMapping("/admin/users/{usuarioId}/jornadas") @Transactional(readOnly=true)
public class AdminJornadasController {
    private final JornadasService service;
    private final UserRepository users;
    public AdminJornadasController(JornadasService service,UserRepository users) { this.service=service;this.users=users; }
    private void validar(Long user) { if(!users.existsById(user)) throw new ResourceNotFoundException("Usuário não encontrado"); }
    @GetMapping public List<JornadasDTO.Resposta> listar(@PathVariable Long usuarioId) { validar(usuarioId);return service.listar(usuarioId); }
    @GetMapping("/{id}") public JornadasDTO.Resposta buscar(@PathVariable Long usuarioId,@PathVariable Long id) { validar(usuarioId);return service.buscar(usuarioId,id); }
}
