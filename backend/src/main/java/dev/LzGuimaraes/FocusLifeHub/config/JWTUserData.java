package dev.LzGuimaraes.FocusLifeHub.config;

import lombok.Builder;

import java.time.Instant;

@Builder
public record JWTUserData(
    String jti,
    Long userId,
    String email,
    String role,
    Integer tokenVersion,
    Instant expiresAt
) {}