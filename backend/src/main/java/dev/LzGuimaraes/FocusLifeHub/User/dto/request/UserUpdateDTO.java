package dev.LzGuimaraes.FocusLifeHub.User.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateDTO(
    String nome,
    @Email(message = "Formato de email inválido")
    String email,
    @Size(min = 8, message = "A nova senha deve ter pelo menos 8 caracteres")
    String password
) {}