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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenConfig tokenConfig;

    public SecurityFilter(TokenConfig tokenConfig) {
        this.tokenConfig = tokenConfig;
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
        String role = userData.role() != null ? userData.role() : "USER";
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userData, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

filterChain.doFilter(request, response);

    }
}
