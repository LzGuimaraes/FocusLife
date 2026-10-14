package dev.LzGuimaraes.FocusLifeHub.Musica;

import java.util.List;
import jakarta.validation.constraints.*;

public final class MusicaDTO {
    private MusicaDTO() {}
    public record Estilo(Long id, String nome, long totalVideos) {}
    public record Video(Long id, Long estiloId, String url, String embedUrl) {
        public static Video from(MusicaVideoModel video) {
            return new Video(video.getId(), video.getEstilo().getId(), video.getUrl(),
                    "https://www.youtube.com/embed/" + video.getYoutubeId());
        }
    }
    public record EstiloRequest(
            @NotBlank(message = "Informe o nome do estilo.")
            @Size(max = 80, message = "O nome do estilo deve ter até 80 caracteres.") String nome) {}
    public record VideosRequest(
            @NotEmpty(message = "Informe pelo menos uma URL.")
            @Size(max = 100, message = "Adicione até 100 URLs por vez.")
            List<@NotBlank(message = "Informe uma URL válida.") @Size(max = 2048, message = "A URL é muito longa.") String> urls) {}
    public record VideoRequest(
            @NotBlank(message = "Informe a URL do vídeo.")
            @Size(max = 2048, message = "A URL é muito longa.") String url) {}
    public record Adicao(int adicionados, int duplicados) {}
}
