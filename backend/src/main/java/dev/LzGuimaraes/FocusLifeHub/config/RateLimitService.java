package dev.LzGuimaraes.FocusLifeHub.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de taxa simples em memória (janela fixa de 60s por IP + ação).
 *
 * Protege as rotas públicas de autenticação (/auth/register, /auth/login,
 * /auth/forgot-password, /auth/reset-password) contra força bruta, criação
 * em massa de contas e "email bombing". É uma proteção de camada de aplicação;
 * em produção vale combinar com WAF/limites do proxy reverso (nginx).
 */
@Service
public class RateLimitService {

    private static final long WINDOW_SECONDS = 60;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @return true se a requisição está dentro do limite (maxRequests por janela de 60s)
     */
    public boolean allow(HttpServletRequest request, String action, int maxRequests) {
        String key = action + ":" + clientIp(request);
        long now = System.currentTimeMillis() / 1000;

        Window current = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startAt >= WINDOW_SECONDS) {
                return new Window(now, 1);
            }
            return new Window(existing.startAt, existing.count + 1);
        });

        return current.count <= maxRequests;
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanup() {
        long now = System.currentTimeMillis() / 1000;
        windows.entrySet().removeIf(entry -> now - entry.getValue().startAt >= WINDOW_SECONDS);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // O primeiro endereço é o cliente original (padrão de proxies)
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Window(long startAt, long count) {}
}
