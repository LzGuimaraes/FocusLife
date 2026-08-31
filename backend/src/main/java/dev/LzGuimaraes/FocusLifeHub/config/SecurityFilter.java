package dev.LzGuimaraes.FocusLifeHub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenConfig tokenConfig;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;

    public SecurityFilter(TokenConfig tokenConfig,
                          TokenBlacklistService tokenBlacklistService,
                          UserRepository userRepository) {
        this.tokenConfig = tokenConfig;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
) throws ServletException, IOException {

        String token = null;

    
    String authHeader = request.getHeader("Authorization");
    if (Strings.isNotEmpty(authHeader) && authHeader.startsWith("Bearer ")) {
        token = authHeader.substring(7);
    }

    
    if (token == null && request.getCookies() != null) {
        for (Cookie cookie : request.getCookies()) {
            if ("jwt".equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }
    }

if (Strings.isNotEmpty(token)) {
    Optional<JWTUserData> optUser = tokenConfig.validateToken(token);
    if (optUser.isPresent()) {
        JWTUserData userData = optUser.get();

        // 1) Token revogado no logout é tratado como inválido
        if (tokenBlacklistService.isRevoked(userData.jti())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2) Valida contra o banco: o usuário ainda existe, está ativo e
        //    usa o papel ATUAL. Isso revoga tokens de usuários excluídos ou
        //    desativados e impede que um papel revogado (ex.: ADMIN) continue
        //    valendo até a expiração do JWT.
        userRepository.findById(userData.userId()).ifPresent(user -> {
            if (user.isEnabled() && isTokenVersionValid(userData, user)) {
                String role = user.getRole() != null ? user.getRole().name() : "USER";
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                JWTUserData.builder()
                                        .jti(userData.jti())
                                        .userId(user.getId())
                                        .email(user.getEmail())
                                        .role(role)
                                        .tokenVersion(user.getTokenVersion())
                                        .expiresAt(userData.expiresAt())
                                        .build(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        });
    }
}

filterChain.doFilter(request, response);

    }

    /**
     * O token carrega uma "versão" (ver) que é incrementada sempre que a senha
     * é trocada/redefinida. Se a conta nunca teve bump (dbVer == 0), aceita
     * qualquer token — inclusive os antigos que não carregam o claim. A partir
     * do primeiro bump, apenas tokens com a versão atual passam, invalidando
     * imediatamente todos os JWTs antigos.
     */
    private boolean isTokenVersionValid(JWTUserData userData, UserModel user) {
        int dbVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        if (dbVersion == 0) {
            return true;
        }
        return userData.tokenVersion() != null && userData.tokenVersion() == dbVersion;
    }
}
