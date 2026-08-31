package dev.LzGuimaraes.FocusLifeHub.config;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import dev.LzGuimaraes.FocusLifeHub.User.UserModel;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


@Component
public class TokenConfig {

    public static final String ISSUER = "FocusLifeHub";
    public static final String AUDIENCE = "focuslife-web";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:604800}")
    private long expirationSeconds;

    public String generateToken(UserModel user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);

        String role = user.getRole() != null ? user.getRole().name() : "USER";
        Integer tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;

        return JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("userId", user.getId())
            .withClaim("role", role)
            .withClaim("ver", tokenVersion)
            .withSubject(user.getEmail())
            .withIssuedAt(Instant.now())
            .withExpiresAt(Instant.now().plusSeconds(expirationSeconds))
            .sign(algorithm);
    }

    public Optional<JWTUserData> validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            DecodedJWT decode = JWT.require(algorithm)
                        .withIssuer(ISSUER)
                        .withAudience(AUDIENCE)
                        .build().verify(token);

            return Optional.of(JWTUserData.builder()
                    .jti(decode.getId())
                    .userId(decode.getClaim("userId").asLong())
                    .email(decode.getSubject())
                    .role(decode.getClaim("role").asString())
                    .tokenVersion(decode.getClaim("ver").isNull() ? null : decode.getClaim("ver").asInt())
                    .expiresAt(decode.getExpiresAtAsInstant())
                    .build());
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }
    }
}