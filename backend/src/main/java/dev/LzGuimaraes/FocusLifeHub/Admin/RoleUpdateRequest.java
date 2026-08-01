package dev.LzGuimaraes.FocusLifeHub.Admin;

import jakarta.validation.constraints.NotNull;

import dev.LzGuimaraes.FocusLifeHub.User.Role;

public record RoleUpdateRequest(
    @NotNull(message = "O role é obrigatório (USER ou ADMIN)")
    Role role
) {}
