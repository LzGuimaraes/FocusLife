package dev.LzGuimaraes.FocusLifeHub.User.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record ForgotPasswordRequest(
        @NotEmpty(message = "Email cannot be empty")
        @Email(message = "Formato de e-mail inválido")
        String email
) {}
