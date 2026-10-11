package dev.LzGuimaraes.FocusLifeHub.Admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoController;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoService;
import dev.LzGuimaraes.FocusLifeHub.Email.DailyNotificationJob;
import dev.LzGuimaraes.FocusLifeHub.User.Role;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;
import dev.LzGuimaraes.FocusLifeHub.config.SecurityConfig;
import dev.LzGuimaraes.FocusLifeHub.config.TokenBlacklistService;
import dev.LzGuimaraes.FocusLifeHub.config.TokenConfig;
import jakarta.servlet.http.Cookie;

/**
 * Base de autorização administrativa (backend).
 *
 * Exercita o {@code SecurityFilter} REAL (não mockado) e as regras de
 * {@link SecurityConfig} para garantir:
 * <ul>
 *   <li>ADMIN validado no banco acessa /admin/** e /ativos/admin/** (200/204);</li>
 *   <li>usuário autenticado SEM papel ADMIN é bloqueado (403);</li>
 *   <li>requisição não autenticada é bloqueada (401);</li>
 *   <li>um perfil ADMIN "enviado" pelo token (frontend) é IGNORADO — o papel
 *       efetivo é sempre o do banco (ex.: admin rebaixado perde acesso na hora);</li>
 *   <li>usuário desativado não recebe autoridade.</li>
 * </ul>
 */
@WebMvcTest(controllers = { AdminController.class, AtivoController.class })
@Import({ SecurityConfig.class, TokenConfig.class })
@TestPropertySource(properties = {
        "jwt.secret=test-only-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz-0123456789",
        "jwt.expiration=604800"
})
class AdminAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenConfig tokenConfig;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private AdminUserDetailService adminUserDetailService;

    @MockitoBean
    private DailyNotificationJob dailyNotificationJob;

    @MockitoBean
    private AtivoService ativoService;

    private UserModel user(Long id, Role role, boolean enabled, int tokenVersion) {
        UserModel u = new UserModel();
        u.setId(id);
        u.setNome("Usuário " + id);
        u.setEmail("user" + id + "@focuslife.test");
        u.setPassword("password-hash");
        u.setRole(role);
        u.setEnabled(enabled);
        u.setTokenVersion(tokenVersion);
        return u;
    }

    private Cookie jwtCookie(UserModel tokenOwner) {
        return new Cookie("jwt", tokenConfig.generateToken(tokenOwner));
    }

    @Test
    void adminAutenticadoAcessaRotasAdministrativas() throws Exception {
        UserModel admin = user(1L, Role.ADMIN, true, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        mockMvc.perform(get("/admin/users").cookie(jwtCookie(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/ativos/admin/delete-all").cookie(jwtCookie(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void usuarioAutenticadoSemPapelAdminRecebeForbidden() throws Exception {
        UserModel comum = user(2L, Role.USER, true, 0);
        when(userRepository.findById(2L)).thenReturn(Optional.of(comum));

        mockMvc.perform(get("/admin/users").cookie(jwtCookie(comum)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/ativos/admin/delete-all").cookie(jwtCookie(comum)))
                .andExpect(status().isForbidden());
    }

    @Test
    void requisicaoNaoAutenticadaERejeitada() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void papelAdminNoTokenEIgnoradoEmFavorDoPapelDoBanco() throws Exception {
        // Token emitido com role ADMIN, mas o banco diz que o usuário é USER
        // (ex.: admin rebaixado). O backend deve usar o papel do banco -> 403.
        UserModel tokenComAdmin = user(3L, Role.ADMIN, true, 0);
        UserModel doBancoComoUser = user(3L, Role.USER, true, 0);
        when(userRepository.findById(3L)).thenReturn(Optional.of(doBancoComoUser));

        mockMvc.perform(get("/admin/users").cookie(jwtCookie(tokenComAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioDesativadoNaoRecebeAutoridade() throws Exception {
        UserModel desativado = user(4L, Role.ADMIN, false, 0);
        when(userRepository.findById(4L)).thenReturn(Optional.of(desativado));

        mockMvc.perform(get("/admin/users").cookie(jwtCookie(desativado)))
                .andExpect(status().isUnauthorized());
    }
}
