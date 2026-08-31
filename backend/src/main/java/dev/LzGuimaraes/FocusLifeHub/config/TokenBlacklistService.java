package dev.LzGuimaraes.FocusLifeHub.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Revogação de tokens JWT em memória (denylist por jti).
 *
 * O logout adiciona o jti aqui; o SecurityFilter rejeita tokens revogados.
 * As entradas expiram junto com o próprio token (expiresAt), então o map
 * nunca cresce indefinidamente. Após um restart do backend a lista é limpa,
 * mas a expiração curta dos JWTs limita a janela de risco.
 */
@Service
public class TokenBlacklistService {

    /** jti -> expiração do token (após isso a entrada pode ser removida) */
    private final Map<String, Instant> revokedJtis = new ConcurrentHashMap<>();

    public void revoke(String jti, Instant tokenExpiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        revokedJtis.put(jti, tokenExpiresAt != null ? tokenExpiresAt : Instant.now().plusSeconds(3600));
    }

    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Instant expiresAt = revokedJtis.get(jti);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanup() {
        Instant now = Instant.now();
        revokedJtis.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
