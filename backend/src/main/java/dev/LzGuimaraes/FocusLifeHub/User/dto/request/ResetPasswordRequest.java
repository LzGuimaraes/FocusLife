package dev.LzGuimaraes.FocusLifeHub.User.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotEmpty(message = "Token cannot be empty")
        String token,
        @NotEmpty(message = "Senha não pode estar vazia")
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String password
) {}
