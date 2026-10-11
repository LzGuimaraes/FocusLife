package dev.LzGuimaraes.FocusLifeHub.Admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminDespesaDTO;
import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminUserRecordsDTO;
import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Despesa.TipoLancamento;
import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Metas.Enum.MetaStatus;
import dev.LzGuimaraes.FocusLifeHub.Metas.dto.MetasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.Prioridade;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.dto.TarefasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.User.Role;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;
import dev.LzGuimaraes.FocusLifeHub.config.SecurityConfig;
import dev.LzGuimaraes.FocusLifeHub.config.TokenBlacklistService;
import dev.LzGuimaraes.FocusLifeHub.config.TokenConfig;
import jakarta.servlet.http.Cookie;

/**
 * Consultas administrativas por módulo ({@code /admin/users/{usuarioId}/...}).
 *
 * Prova, com o {@link SecurityConfig} e o SecurityFilter REAIS (sem banco):
 * <ul>
 *   <li>ADMIN consulta os registros do usuário SELECIONADO (o serviço recebe o
 *       id do caminho — 9 — e não o id do admin autenticado — 1);</li>
 *   <li>filtros opcionais são repassados ao serviço sem trocar o dono;</li>
 *   <li>usuário comum NÃO acessa os endpoints para consultar terceiros (403 e o
 *       serviço nunca é chamado);</li>
 *   <li>requisição anônima recebe 401;</li>
 *   <li>alvo inexistente devolve 404.</li>
 * </ul>
 */
@WebMvcTest(controllers = AdminUserRecordsController.class)
@Import({ SecurityConfig.class, TokenConfig.class })
@TestPropertySource(properties = {
        "jwt.secret=test-only-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz-0123456789",
        "jwt.expiration=604800"
})
class AdminUserRecordsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenConfig tokenConfig;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private AdminUserRecordsService adminUserRecordsService;

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

    private AdminUserRecordsDTO<TarefasResponseDTO> paginaDeTarefas(Long alvo) {
        TarefasResponseDTO dto = new TarefasResponseDTO(
                100L, "Estudar Spring", TarefaStatus.PENDENTE, Prioridade.alta,
                LocalDate.of(2026, 10, 20), null, alvo);
        return new AdminUserRecordsDTO<>(alvo, new PageImpl<>(List.of(dto)));
    }

    private AdminUserRecordsDTO<MetasResponseDTO> paginaDeMetas(Long alvo) {
        MetasResponseDTO dto = new MetasResponseDTO(
                200L, "Ler 12 livros", "Um por mês", 25.0f,
                LocalDate.of(2026, 12, 31), MetaStatus.Ativa, alvo);
        return new AdminUserRecordsDTO<>(alvo, new PageImpl<>(List.of(dto)));
    }

    private AdminUserRecordsDTO<EstudosResponseDTO> paginaDeEstudos(Long alvo) {
        EstudosResponseDTO dto = new EstudosResponseDTO(
                300L, "Revisão de cálculo", 45, LocalDate.of(2026, 10, 5), "cap. 3", 55L);
        return new AdminUserRecordsDTO<>(alvo, new PageImpl<>(List.of(dto)));
    }

    private AdminUserRecordsDTO<AdminAtivoDTO> paginaDeInvestimentos(Long alvo) {
        AdminAtivoDTO dto = new AdminAtivoDTO(
                400L, "PETR4", CategoriaInvestimento.ACOES, 10f, 30f, 33f, 330f,
                "Corretora X", null, null, null, null, 30L, "Carteira do alvo", "BRL");
        return new AdminUserRecordsDTO<>(alvo, new PageImpl<>(List.of(dto)));
    }

    private AdminUserRecordsDTO<AdminDespesaDTO> paginaDeContas(Long alvo) {
        AdminDespesaDTO dto = new AdminDespesaDTO(
                500L, "Aluguel", 1500f, LocalDate.of(2026, 11, 5), false,
                TipoLancamento.DESPESA, null, 40L, "Dívidas do alvo", "BRL");
        return new AdminUserRecordsDTO<>(alvo, new PageImpl<>(List.of(dto)));
    }

    // ------------------------------------------------------------------ ADMIN OK

    @Test
    void adminConsultaTarefasDoUsuarioSelecionado() throws Exception {
        UserModel admin = user(1L, Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserRecordsService.tarefasDoUsuario(eq(9L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(paginaDeTarefas(9L));

        mockMvc.perform(get("/admin/users/9/tarefas").cookie(jwtCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(9))
                .andExpect(jsonPath("$.pagina.content[0].titulo").value("Estudar Spring"))
                .andExpect(jsonPath("$.pagina.content[0].user_id").value(9))
                .andExpect(jsonPath("$.pagina.totalElements").value(1));

        // O alvo é o 9 (do caminho), nunca o admin autenticado (1).
        verify(adminUserRecordsService).tarefasDoUsuario(eq(9L), isNull(), isNull(), any(Pageable.class));
        verify(adminUserRecordsService, never()).tarefasDoUsuario(eq(1L), any(), any(), any());
    }

    @Test
    void adminConsultaMetasDoUsuarioSelecionadoComFiltroDeStatus() throws Exception {
        UserModel admin = user(1L, Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserRecordsService.metasDoUsuario(eq(9L), eq(MetaStatus.Ativa), any(Pageable.class)))
                .thenReturn(paginaDeMetas(9L));

        mockMvc.perform(get("/admin/users/9/metas").param("status", "Ativa").cookie(jwtCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(9))
                .andExpect(jsonPath("$.pagina.content[0].titulo").value("Ler 12 livros"))
                .andExpect(jsonPath("$.pagina.content[0].user_id").value(9));

        verify(adminUserRecordsService).metasDoUsuario(eq(9L), eq(MetaStatus.Ativa), any(Pageable.class));
    }

    @Test
    void adminConsultaEstudosDoUsuarioSelecionadoComFiltros() throws Exception {
        UserModel admin = user(1L, Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserRecordsService.estudosDoUsuario(eq(9L), eq(55L),
                eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31)), any(Pageable.class)))
                .thenReturn(paginaDeEstudos(9L));

        mockMvc.perform(get("/admin/users/9/estudos")
                        .param("materia_id", "55")
                        .param("de", "2026-10-01")
                        .param("ate", "2026-10-31")
                        .cookie(jwtCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(9))
                .andExpect(jsonPath("$.pagina.content[0].nome").value("Revisão de cálculo"))
                .andExpect(jsonPath("$.pagina.content[0].materia_id").value(55));

        // O alvo (9) é sempre enviado junto com os filtros: a posse é revalidada no banco.
        verify(adminUserRecordsService).estudosDoUsuario(eq(9L), eq(55L),
                eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31)), any(Pageable.class));
    }

    @Test
    void adminConsultandoAlvoInexistenteRecebe404() throws Exception {
        UserModel admin = user(1L, Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserRecordsService.tarefasDoUsuario(eq(99L), isNull(), isNull(), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Usuário com ID 99 não encontrado"));

        mockMvc.perform(get("/admin/users/99/tarefas").cookie(jwtCookie(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminConsultaInvestimentosDoUsuarioSelecionadoComFiltroDeCategoria() throws Exception {
        UserModel admin = user(1L, Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserRecordsService.investimentosDoUsuario(eq(9L), eq(CategoriaInvestimento.ACOES), any(Pageable.class)))
                .thenReturn(paginaDeInvestimentos(9L));

        mockMvc.perform(get("/admin/users/9/investimentos")
                        .param("categoria", "ACOES")
                        .cookie(jwtCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(9))
                .andExpect(jsonPath("$.pagina.content[0].nome").value("PETR4"))
                .andExpect(jsonPath("$.pagina.content[0].carteiraNome").value("Carteira do alvo"))
                .andExpect(jsonPath("$.pagina.totalElements").value(1));

        // O alvo é o 9 (do caminho) e o filtro vai JUNTO — nunca troca o dono.
        verify(adminUserRecordsService).investimentosDoUsuario(eq(9L), eq(CategoriaInvestimento.ACOES), any(Pageable.class));
    }

    @Test
    void adminConsultaContasDoUsuarioSelecionadoComFiltrosDeTipoEPago() throws Exception {
        UserModel admin = user(1L, Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminUserRecordsService.contasDoUsuario(eq(9L), eq(TipoLancamento.DESPESA), eq(false), any(Pageable.class)))
                .thenReturn(paginaDeContas(9L));

        mockMvc.perform(get("/admin/users/9/contas")
                        .param("tipo", "DESPESA")
                        .param("pago", "false")
                        .cookie(jwtCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(9))
                .andExpect(jsonPath("$.pagina.content[0].nome").value("Aluguel"))
                .andExpect(jsonPath("$.pagina.content[0].pago").value(false))
                .andExpect(jsonPath("$.pagina.content[0].carteiraNome").value("Dívidas do alvo"));

        verify(adminUserRecordsService).contasDoUsuario(eq(9L), eq(TipoLancamento.DESPESA), eq(false), any(Pageable.class));
    }

    // ------------------------------------------------------------- NÃO AUTORIZADO

    @Test
    void usuarioComumNaoConsegueConsultarTerceiros() throws Exception {
        UserModel comum = user(2L, Role.USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(comum));
        Cookie cookie = jwtCookie(comum);

        mockMvc.perform(get("/admin/users/9/tarefas").cookie(cookie)).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/9/metas").cookie(cookie)).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/9/estudos").cookie(cookie)).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/9/investimentos").cookie(cookie)).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/9/contas").cookie(cookie)).andExpect(status().isForbidden());

        // Nem chega a consultar: a barreira é a autorização, não o serviço.
        verifyNoInteractions(adminUserRecordsService);
    }

    @Test
    void requisicaoNaoAutenticadaRecebe401() throws Exception {
        mockMvc.perform(get("/admin/users/9/tarefas"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserRecordsService);
    }

    @Test
    void papelAdminNoTokenNaoAutorizaSeOBancoDizUser() throws Exception {
        // Token emitido como ADMIN, mas o banco diz USER (admin rebaixado) -> 403.
        UserModel tokenComAdmin = user(3L, Role.ADMIN, true);
        UserModel doBancoComoUser = user(3L, Role.USER, true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(doBancoComoUser));

        mockMvc.perform(get("/admin/users/9/tarefas").cookie(jwtCookie(tokenComAdmin)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserRecordsService);
    }
}
