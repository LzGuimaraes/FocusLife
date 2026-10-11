package dev.LzGuimaraes.FocusLifeHub.Admin;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminUserDetailDTO;
import dev.LzGuimaraes.FocusLifeHub.Email.DailyNotificationJob;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.User.Role;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;
import dev.LzGuimaraes.FocusLifeHub.config.SecurityConfig;
import dev.LzGuimaraes.FocusLifeHub.config.TokenBlacklistService;
import dev.LzGuimaraes.FocusLifeHub.config.TokenConfig;
import jakarta.servlet.http.Cookie;

/**
 * Detalhe administrativo de um usuário ({@code GET /admin/users/{id}}).
 *
 * Cobre o contrato HTTP e a autorização no backend (SecurityFilter REAL, sem DB):
 * usuário existente -> 200 com o resumo estruturado; usuário inexistente -> 404;
 * autenticado sem papel ADMIN -> 403; sem autenticação -> 401. A resposta NUNCA
 * expõe credenciais/segredos (verificado no nível do JSON).
 */
@WebMvcTest(controllers = AdminController.class)
@Import({ SecurityConfig.class, TokenConfig.class })
@TestPropertySource(properties = {
        "jwt.secret=test-only-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz-0123456789",
        "jwt.expiration=604800"
})
class AdminUserDetailControllerTest {

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

    private UserModel user(Long id, Role role, boolean enabled) {
        UserModel u = new UserModel();
        u.setId(id);
        u.setNome("Usuário " + id);
        u.setEmail("user" + id + "@focuslife.test");
        u.setPassword("password-hash");
        u.setRole(role);
        u.setEnabled(enabled);
        u.setTokenVersion(0);
        return u;
    }

    private Cookie jwtCookie(UserModel tokenOwner) {
        return new Cookie("jwt", tokenConfig.generateToken(tokenOwner));
    }

    private AdminUserDetailDTO detalheExemplo() {
        return new AdminUserDetailDTO(
                9L, "Maria", "maria@focuslife.test", "USER", true,
                new AdminUserDetailDTO.ResumoDTO(
                        new AdminUserDetailDTO.FinancasDTO(2, 1, 15, 8, 6, 4),
                        new AdminUserDetailDTO.PlanejamentoDTO(3, 5, 2, 1, true, 10, 7),
                        new AdminUserDetailDTO.EstudosDTO(4, 20, 30),
                        12,
                        new AdminUserDetailDTO.TreinoDTO(5, 40)));
    }

    @Test
    void adminConsultaDetalheDeUsuarioExistente() throws Exception {
        UserModel admin = user(1L, Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserDetailService.getUserDetail(9L)).thenReturn(detalheExemplo());

        mockMvc.perform(get("/admin/users/9").cookie(jwtCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.email").value("maria@focuslife.test"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.resumo.financas.carteirasInvestimento").value(2))
                .andExpect(jsonPath("$.resumo.financas.ativos").value(15))
                .andExpect(jsonPath("$.resumo.planejamento.aporteConfigurado").value(true))
                .andExpect(jsonPath("$.resumo.estudos.sessoesEstudo").value(30))
                .andExpect(jsonPath("$.resumo.tarefas").value(12))
                .andExpect(jsonPath("$.resumo.treino.treinos").value(5));
    }

    @Test
    void respostaNaoExpoeCredenciaisNemCamposSensiveis() throws Exception {
        UserModel admin = user(1L, Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserDetailService.getUserDetail(9L)).thenReturn(detalheExemplo());

        mockMvc.perform(get("/admin/users/9").cookie(jwtCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.activationCode").doesNotExist())
                .andExpect(jsonPath("$.activationCodeExpiration").doesNotExist())
                .andExpect(jsonPath("$.resetPasswordToken").doesNotExist())
                .andExpect(jsonPath("$.resetPasswordTokenExpiration").doesNotExist())
                .andExpect(jsonPath("$.tokenVersion").doesNotExist())
                // nenhum desses nomes pode aparecer em NENHUM nível do JSON
                .andExpect(jsonPath("$..password").doesNotExist())
                .andExpect(jsonPath("$..activationCode").doesNotExist())
                .andExpect(jsonPath("$..resetPasswordToken").doesNotExist())
                .andExpect(jsonPath("$..tokenVersion").doesNotExist());
    }

    @Test
    void usuarioInexistenteRetorna404() throws Exception {
        UserModel admin = user(1L, Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserDetailService.getUserDetail(99L))
                .thenThrow(new ResourceNotFoundException("Usuário com ID 99 não encontrado"));

        mockMvc.perform(get("/admin/users/99").cookie(jwtCookie(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioSemPapelAdminRecebe403() throws Exception {
        UserModel comum = user(2L, Role.USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(comum));

        mockMvc.perform(get("/admin/users/9").cookie(jwtCookie(comum)))
                .andExpect(status().isForbidden());

        // O serviço de detalhe nem chega a ser executado.
        verify(adminUserDetailService, never()).getUserDetail(9L);
    }

    @Test
    void requisicaoNaoAutenticadaRecebe401() throws Exception {
        mockMvc.perform(get("/admin/users/9"))
                .andExpect(status().isUnauthorized());

        verify(adminUserDetailService, never()).getUserDetail(9L);
    }
}
