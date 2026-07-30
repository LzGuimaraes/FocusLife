
package dev.LzGuimaraes.FocusLifeHub.User.dto.request;

import jakarta.validation.constraints.NotEmpty;

public record LoginRequest(
    @NotEmpty(message = "Email cannot be empty")
    String email,
    @NotEmpty(message = "Senha cannot be empty")
    String password
) {}