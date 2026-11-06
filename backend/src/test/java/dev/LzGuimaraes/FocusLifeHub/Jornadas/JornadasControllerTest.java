package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import dev.LzGuimaraes.FocusLifeHub.User.*;
import dev.LzGuimaraes.FocusLifeHub.config.*;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import jakarta.servlet.http.Cookie;
@WebMvcTest({JornadasController.class,AdminJornadasController.class})
@Import({SecurityConfig.class,TokenConfig.class,JornadasWebConfig.class})
@TestPropertySource(properties={"jwt.secret=test-only-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz-0123456789","jwt.expiration=604800"})
class JornadasControllerTest {
    @Autowired MockMvc mvc; @Autowired TokenConfig tokens;
    @MockitoBean UserRepository users;@MockitoBean TokenBlacklistService blacklist;@MockitoBean JornadasService service;
    Cookie cookie(Role role) {
        var u=new UserModel();u.setId(9L);u.setNome("Maria");u.setEmail("maria@example.test");u.setEnabled(true);u.setRole(role);u.setTokenVersion(0);
        when(users.findById(9L)).thenReturn(Optional.of(u));return new Cookie("jwt",tokens.generateToken(u));
    }
    String body() {return """
        {"titulo":"Livro","inicio":"2026-10-15","prazo":"2026-11-15","unidade":"páginas","fracionavel":false,"total":350,"saldoInicial":0,"status":"NAO_INICIADA","userId":999}
        """;}
    @Test void autenticacaoObrigatoriaEIdentidadeNaoVemDoPayload() throws Exception {
        mvc.perform(get("/jornadas")).andExpect(status().isUnauthorized());mvc.perform(post("/jornadas").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isUnauthorized());
        mvc.perform(post("/jornadas").cookie(cookie(Role.USER)).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isOk());
        verify(service).salvar(eq(9L),isNull(),any());verify(service,never()).salvar(eq(999L),any(),any());
    }
    @Test void camposInvalidosNaoChegamAoServico() throws Exception {
        var auth=cookie(Role.USER);
        for(String b:List.of(body().replace("\"Livro\"","\" \""),body().replace("\"total\":350","\"total\":0"),body().replace("\"saldoInicial\":0","\"saldoInicial\":-1"),body().replace("NAO_INICIADA","STATUS_INVALIDO"),body().replace("2026-10-15","2026-99-99")))
            mvc.perform(post("/jornadas").cookie(auth).contentType(MediaType.APPLICATION_JSON).content(b)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void validacaoDeMotivacaoQuantidadesEUUID() throws Exception {
        var auth=cookie(Role.USER);
        String body="{\"version\":0,\"requestId\":\"00000000-0000-0000-0000-000000000001\",\"data\":\"2026-11-05\",\"modo\":\"INCREMENTO\",\"quantidade\":5,\"motivacao\":6}";
        mvc.perform(post("/jornadas/7/progresso").cookie(auth).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(post("/jornadas/7/progresso").cookie(auth).contentType(MediaType.APPLICATION_JSON).content(body.replace("\"motivacao\":6","\"motivacao\":4").replace("\"quantidade\":5","\"quantidade\":-1"))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void registrosDeOutroUsuarioRetornam404() throws Exception {
        when(service.buscar(9L,7L)).thenThrow(new ResourceNotFoundException("Atividade não encontrada"));mvc.perform(get("/jornadas/7").cookie(cookie(Role.USER))).andExpect(status().isNotFound());
        verify(service).buscar(9L,7L);
    }
    @Test void adminSomenteLeituraEUsuarioComumNaoConsultaOutros() throws Exception {
        mvc.perform(get("/admin/users/2/jornadas").cookie(cookie(Role.USER))).andExpect(status().isForbidden());verifyNoInteractions(service);
        when(users.existsById(2L)).thenReturn(true);when(service.listar(2L)).thenReturn(List.of());
        mvc.perform(get("/admin/users/2/jornadas").cookie(cookie(Role.ADMIN))).andExpect(status().isOk());verify(service).listar(2L);
        mvc.perform(post("/admin/users/2/jornadas").cookie(cookie(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isMethodNotAllowed());
    }
    @Test void rotasAntigasNaoPermitemManterSistemasParalelos() throws Exception {
        // Interceptor também cobre caminhos sem handlers antigos carregados nesta fatia MVC.
        mvc.perform(get("/tarefas/all").cookie(cookie(Role.USER))).andExpect(status().isGone());
        mvc.perform(post("/materias/create").cookie(cookie(Role.USER)).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isGone());
        verifyNoInteractions(service);
    }
    @Test void concorrenciaRetorna409ComMensagemUtil() throws Exception {
        when(service.salvar(eq(9L),eq(7L),any())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Os dados mudaram."));
        mvc.perform(put("/jornadas/7").cookie(cookie(Role.USER)).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Os dados mudaram."));
    }
}
