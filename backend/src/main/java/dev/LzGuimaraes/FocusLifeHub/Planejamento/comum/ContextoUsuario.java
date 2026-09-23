package dev.LzGuimaraes.FocusLifeHub.Planejamento.comum;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;

/**
 * Ponto único de leitura do usuário autenticado para o módulo de Planejamento.
 * Evita duplicar o mesmo bloco de SecurityContextHolder em cada serviço novo.
 */
@Component
public class ContextoUsuario {

    /** ID do usuário autenticado (JWT). Nunca retorna null em rota autenticada. */
    public Long id() {
        return dados().userId();
    }

    /** Papel do usuário conforme o token (ex.: "ADMIN"). */
    public String role() {
        return dados().role();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role());
    }

    private JWTUserData dados() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JWTUserData dados)) {
            throw new ResourceNotFoundException("Usuário autenticado não encontrado");
        }
        return dados;
    }
}
