package dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.dto;

import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.TipoAtivoCadastro;

public record AtivoCadastroResponseDTO(
    UUID id,
    String nome,
    TipoAtivoCadastro tipo,
    Float precoAtual
) {}
