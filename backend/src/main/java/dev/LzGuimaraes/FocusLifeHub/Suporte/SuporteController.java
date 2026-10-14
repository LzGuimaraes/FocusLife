package dev.LzGuimaraes.FocusLifeHub.Suporte;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ErrorResponse;

@RestController
@RequestMapping("/suporte/chamados")
public class SuporteController {
    private final SuporteService service;
    private final SuporteEmailService emails;
    public SuporteController(SuporteService service, SuporteEmailService emails) { this.service = service; this.emails = emails; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public SuporteDTO.Response abrir(@AuthenticationPrincipal JWTUserData usuario, @Valid @RequestBody SuporteDTO.Request request) {
        // A criação confirma a transação antes do envio: SMTP indisponível não apaga o chamado.
        var chamado = service.abrir(usuario.userId(), request);
        emails.enviar(chamado.id());
        return service.buscarMeu(usuario.userId(), chamado.id());
    }
    @GetMapping
    public Page<SuporteDTO.Response> listar(@AuthenticationPrincipal JWTUserData usuario, @RequestParam(defaultValue = "0") int page) {
        return service.listarMeus(usuario.userId(), page);
    }
    @GetMapping("/{id}")
    public SuporteDTO.Response buscar(@AuthenticationPrincipal JWTUserData usuario, @PathVariable Long id) {
        return service.buscarMeu(usuario.userId(), id);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> dadosInvalidos() {
        return ResponseEntity.badRequest().body(new ErrorResponse(java.time.LocalDateTime.now(), 400,
                "Dados do chamado inválidos. Confira o tipo de solicitação e tente novamente."));
    }
}
