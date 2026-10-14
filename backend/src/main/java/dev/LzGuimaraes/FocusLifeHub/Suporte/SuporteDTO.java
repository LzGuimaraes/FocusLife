package dev.LzGuimaraes.FocusLifeHub.Suporte;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.*;

public final class SuporteDTO {
    private SuporteDTO() {}
    public record Request(
            @NotNull(message = "Identifique a solicitação.") UUID solicitacaoId,
            @NotNull(message = "Escolha o tipo de solicitação.") TipoChamado tipo,
            @NotBlank(message = "Informe o assunto.") @Size(max = 120, message = "O assunto deve ter até 120 caracteres.") String titulo,
            @NotBlank(message = "Descreva sua solicitação.") @Size(max = 4000, message = "A descrição deve ter até 4.000 caracteres.") String descricao) {}
    public record Response(Long id, String protocolo, TipoChamado tipo, String titulo, String descricao,
                           Instant criadoEm, boolean emailEnviado) {
        static Response from(SuporteChamadoModel chamado) {
            return new Response(chamado.getId(), SuporteDTO.protocolo(chamado.getId()), chamado.getTipo(), chamado.getTitulo(),
                    chamado.getDescricao(), chamado.getCriadoEm(), chamado.isEmailEnviado());
        }
    }
    static String protocolo(Long id) { return "SUP-" + String.format(java.util.Locale.ROOT, "%06d", id); }
}
