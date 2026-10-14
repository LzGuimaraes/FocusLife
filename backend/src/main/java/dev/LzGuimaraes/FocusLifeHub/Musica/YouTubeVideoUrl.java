package dev.LzGuimaraes.FocusLifeHub.Musica;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;

final class YouTubeVideoUrl {
    private static final Pattern ID = Pattern.compile("[a-zA-Z0-9_-]{11}");
    private static final Set<String> HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtube-nocookie.com", "www.youtube-nocookie.com");
    private YouTubeVideoUrl() {}

    static String id(String input) {
        try {
            String raw = input == null ? "" : input.trim();
            if (ID.matcher(raw).matches()) return raw;
            URI uri = URI.create(raw.contains("://") ? raw : "https://" + raw);
            String host = uri.getHost();
            if (host == null || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getUserInfo() != null || uri.getPort() != -1) throw invalida();
            host = host.toLowerCase(java.util.Locale.ROOT);
            String path = uri.getPath().replaceAll("/$", "");
            String value = null;
            if ("youtu.be".equals(host) || "www.youtu.be".equals(host)) value = path.substring(1);
            else if (HOSTS.contains(host)) {
                if ("/watch".equals(path) && uri.getRawQuery() != null) {
                    for (String item : uri.getRawQuery().split("&")) {
                        String[] pair = item.split("=", 2);
                        if (pair.length == 2 && "v".equals(pair[0])) {
                            value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                            break;
                        }
                    }
                } else if (path.matches("/(embed|shorts|live)/[a-zA-Z0-9_-]{11}")) {
                    value = path.substring(path.lastIndexOf('/') + 1);
                }
            }
            if (value != null && ID.matcher(value).matches()) return value;
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            // Uma mensagem única para formato, domínio ou identificador inválido.
        }
        throw invalida();
    }

    private static BusinessRuleException invalida() {
        return new BusinessRuleException("Use uma URL de vídeo válida do YouTube (watch, youtu.be, shorts, live ou embed).");
    }
}
