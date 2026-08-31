package dev.LzGuimaraes.FocusLifeHub.Auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.mail.MessagingException;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;
import dev.LzGuimaraes.FocusLifeHub.User.dto.request.ForgotPasswordRequest;
import dev.LzGuimaraes.FocusLifeHub.User.dto.request.LoginRequest;
import dev.LzGuimaraes.FocusLifeHub.User.dto.request.RegisterUserRequest;
import dev.LzGuimaraes.FocusLifeHub.User.dto.request.ResetPasswordRequest;
import dev.LzGuimaraes.FocusLifeHub.User.dto.response.LoginResponse;
import dev.LzGuimaraes.FocusLifeHub.User.dto.response.MessageResponse;
import dev.LzGuimaraes.FocusLifeHub.config.TokenBlacklistService;
import dev.LzGuimaraes.FocusLifeHub.config.TokenConfig;
import dev.LzGuimaraes.FocusLifeHub.config.RateLimitService;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;
import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;
    private final TokenBlacklistService tokenBlacklistService;
    private final RateLimitService rateLimitService;
    private final MailService mailService;

    @Value("${app.frontend.url:https://focus.lzguimaraes.com.br}")
    private String frontendUrl;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          TokenConfig tokenConfig,
                          TokenBlacklistService tokenBlacklistService,
                          RateLimitService rateLimitService,
                          MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenConfig = tokenConfig;
        this.tokenBlacklistService = tokenBlacklistService;
        this.rateLimitService = rateLimitService;
        this.mailService = mailService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        if (!rateLimitService.allow(httpRequest, "login", 10)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Muitas tentativas de login. Tente novamente em instantes."));
        }

        UsernamePasswordAuthenticationToken userAndPass = new UsernamePasswordAuthenticationToken(
                request.email(),
                request.password()
        );

        try {
            Authentication authentication = authenticationManager.authenticate(userAndPass);
            UserModel user = (UserModel) authentication.getPrincipal();

            String token = tokenConfig.generateToken(user);
            ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60)
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            return ResponseEntity.ok(new LoginResponse("Login successful"));
        } catch (DisabledException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Conta não ativada. Verifique seu e-mail e confirme a ativação."));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("E-mail ou senha inválidos."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterUserRequest request, HttpServletRequest httpRequest) {
        if (!rateLimitService.allow(httpRequest, "register", 5)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Muitas tentativas de cadastro. Tente novamente em instantes."));
        }

        // Anti-enumeração: a resposta NÃO revela se o e-mail já existe.
        // Para quem já tem conta não é enviado e-mail, mas a resposta é idêntica
        // à de sucesso (mesmo status e mesma mensagem), impedindo que um
        // atacante valide e-mails em massa.
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Tentativa de registro com e-mail já existente (resposta ocultada do cliente)");
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MessageResponse("Usuário criado. Enviamos um e-mail de ativação. Verifique sua caixa de entrada e também a pasta de spam."));
        }

        UserModel newUser = new UserModel();
        newUser.setNome(request.name());
        newUser.setEmail(request.email());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setEnabled(false);
        newUser.setActivationCode(UUID.randomUUID().toString());
        newUser.setActivationCodeExpiration(Instant.now().plusSeconds(24 * 60 * 60));

        userRepository.save(newUser);
        log.info("Usuário salvo com ID={} e email={}", newUser.getId(), newUser.getEmail());

        String activationLink = String.format("%s/auth/activate?code=%s", frontendUrl, newUser.getActivationCode());
        String body = "<html>"
                + "<body style=\"margin:0;padding:0;font-family:Arial,sans-serif;background:#f4f7fb;color:#334155;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;margin:0 auto;padding:24px;background:#ffffff;border-radius:16px;box-shadow:0 10px 30px rgba(15,23,42,0.08);\">"
                + "<tr><td style=\"text-align:center;padding-bottom:24px;\">"
                + "<h1 style=\"margin:0;color:#0f172a;font-size:26px;\">Bem-vindo ao FocusLife Hub</h1>"
                + "</td></tr>"
                + "<tr><td style=\"padding-bottom:16px;\">"
                + "<p style=\"margin:0 0 16px;line-height:1.7;color:#475569;\">Olá " + newUser.getNome() + ",</p>"
                + "<p style=\"margin:0 0 24px;line-height:1.7;color:#475569;\">Sua conta foi criada com sucesso. Para começar a usar o FocusLife Hub, confirme seu endereço de e-mail clicando no botão abaixo.</p>"
                + "</td></tr>"
                + "<tr><td style=\"text-align:center;padding-bottom:28px;\">"
                + "<a href=\"" + activationLink + "\" style=\"display:inline-block;padding:14px 26px;background:#4f46e5;color:#fff;text-decoration:none;border-radius:12px;font-weight:700;\">Ativar conta</a>"
                + "</td></tr>"
                + "<tr><td style=\"padding-bottom:16px;\">"
                + "<p style=\"margin:0 0 8px;line-height:1.7;color:#475569;\">Se o botão acima não funcionar, copie e cole o link abaixo no seu navegador:</p>"
                + "<p style=\"word-break:break-all;color:#2563eb;\"><a href=\"" + activationLink + "\" style=\"color:#2563eb;text-decoration:none;\">" + activationLink + "</a></p>"
                + "</td></tr>"
                + "<tr><td style=\"padding-top:16px;border-top:1px solid #e2e8f0;color:#94a3b8;font-size:13px;\">"
                + "<p style=\"margin:0;\">Se você não solicitou essa conta, ignore este e-mail. Essa mensagem foi enviada automaticamente.</p>"
                + "</td></tr>"
                + "</table>"
                + "</body></html>";

        try {
            log.info("Enviando email de ativação para email={}", newUser.getEmail());
            mailService.sendHtml(newUser.getEmail(), "Ative sua conta no FocusLife Hub", body);
        } catch (Exception ex) {
            log.error("Falha ao enviar email de ativação para {}", newUser.getEmail(), ex);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MessageResponse("Usuário criado, mas não foi possível enviar o email de ativação. Verifique a configuração SMTP e tente novamente mais tarde."));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Usuário criado. Enviamos um e-mail de ativação. Verifique sua caixa de entrada e também a pasta de spam."));
    }

    @GetMapping("/activate")
    public ResponseEntity<MessageResponse> activateAccount(@RequestParam("code") String code) {
        Optional<UserModel> optionalUser = userRepository.findByActivationCode(code);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Código de ativação inválido ou expirado."));
        }

        UserModel user = optionalUser.get();

        if (user.getActivationCodeExpiration() != null && user.getActivationCodeExpiration().isBefore(Instant.now())) {
            log.warn("Código de ativação expirado para email={}", user.getEmail());
            user.setActivationCode(null);
            user.setActivationCodeExpiration(null);
            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Este link de ativação expirou. Solicite um novo cadastro ou entre em contato com o suporte."));
        }

        if (Boolean.TRUE.equals(user.getEnabled())) {
            log.info("Conta já ativada para email={}", user.getEmail());
            return ResponseEntity.ok(new MessageResponse("Conta já ativada."));
        }

        user.setEnabled(true);
        user.setActivationCode(null);
        user.setActivationCodeExpiration(null);
        userRepository.save(user);
        log.info("Conta ativada com sucesso para email={}", user.getEmail());

        return ResponseEntity.ok(new MessageResponse("Conta ativada com sucesso. Agora você pode entrar."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) throws MessagingException {
        if (!rateLimitService.allow(httpRequest, "forgot-password", 5)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Muitas solicitações de redefinição. Tente novamente em instantes."));
        }

        Optional<UserModel> optionalUser = userRepository.findByEmail(request.email());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok(new MessageResponse("Se houver uma conta associada a este e-mail, você receberá instruções para redefinir a senha."));
        }

        UserModel user = optionalUser.get();
        user.setResetPasswordToken(UUID.randomUUID().toString());
        user.setResetPasswordTokenExpiration(Instant.now().plusSeconds(3600));
        userRepository.save(user);

        String resetLink = String.format("%s/auth/reset-password?token=%s", frontendUrl, user.getResetPasswordToken());
        String body = "<html>"
                + "<body style=\"margin:0;padding:0;font-family:Arial,sans-serif;background:#f4f7fb;color:#334155;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;margin:0 auto;padding:24px;background:#ffffff;border-radius:16px;box-shadow:0 10px 30px rgba(15,23,42,0.08);\">"
                + "<tr><td style=\"text-align:center;padding-bottom:24px;\">"
                + "<h1 style=\"margin:0;color:#0f172a;font-size:26px;\">Redefinição de senha</h1>"
                + "</td></tr>"
                + "<tr><td style=\"padding-bottom:16px;\">"
                + "<p style=\"margin:0 0 16px;line-height:1.7;color:#475569;\">Olá " + user.getNome() + ",</p>"
                + "<p style=\"margin:0 0 24px;line-height:1.7;color:#475569;\">Recebemos uma solicitação para redefinir sua senha. Clique no botão abaixo para continuar:</p>"
                + "</td></tr>"
                + "<tr><td style=\"text-align:center;padding-bottom:28px;\">"
                + "<a href=\"" + resetLink + "\" style=\"display:inline-block;padding:14px 26px;background:#4f46e5;color:#fff;text-decoration:none;border-radius:12px;font-weight:700;\">Redefinir senha</a>"
                + "</td></tr>"
                + "<tr><td style=\"padding-bottom:16px;\">"
                + "<p style=\"margin:0 0 8px;line-height:1.7;color:#475569;\">Se o botão acima não funcionar, copie e cole o link abaixo no seu navegador:</p>"
                + "<p style=\"word-break:break-all;color:#2563eb;\"><a href=\"" + resetLink + "\" style=\"color:#2563eb;text-decoration:none;\">" + resetLink + "</a></p>"
                + "</td></tr>"
                + "<tr><td style=\"padding-top:16px;border-top:1px solid #e2e8f0;color:#94a3b8;font-size:13px;\">"
                + "<p style=\"margin:0;\">Se você não solicitou essa alteração, ignore este e-mail. Essa mensagem foi enviada automaticamente.</p>"
                + "</td></tr>"
                + "</table>"
                + "</body></html>";

        try {
            mailService.sendHtml(user.getEmail(), "Redefinição de senha FocusLife Hub", body);
        } catch (Exception ex) {
            log.error("Falha ao enviar email de redefinição de senha", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Houve um problema ao enviar o e-mail de redefinição de senha. Tente novamente mais tarde."));
        }

        return ResponseEntity.ok(new MessageResponse("Se houver uma conta associada a este e-mail, você receberá instruções para redefinir a senha."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        if (!rateLimitService.allow(httpRequest, "reset-password", 5)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Muitas tentativas de redefinição. Tente novamente em instantes."));
        }

        Optional<UserModel> optionalUser = userRepository.findByResetPasswordToken(request.token());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Token inválido ou expirado."));
        }

        UserModel user = optionalUser.get();
        if (user.getResetPasswordTokenExpiration() == null || user.getResetPasswordTokenExpiration().isBefore(Instant.now())) {
            log.warn("Token de reset expirado para email={}", user.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Token inválido ou expirado."));
        }

        user.setPassword(passwordEncoder.encode(request.password()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiration(null);
        // Invalida todos os JWTs emitidos antes da redefinição de senha
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userRepository.save(user);
        log.info("Senha redefinida com sucesso para email={}", user.getEmail());

        return ResponseEntity.ok(new MessageResponse("Senha redefinida com sucesso. Você já pode entrar com a nova senha."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest, HttpServletResponse response) {
        // Revoga o token atual na denylist, mesmo que o cookie não possa ser
        // limpo (ex.: token expirado não chega ao logout autenticado).
        String token = extractToken(httpRequest);
        if (token != null) {
            tokenConfig.validateToken(token).ifPresent(userData ->
                    tokenBlacklistService.revoke(userData.jti(), userData.expiresAt()));
        }

        ResponseCookie deleteCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
