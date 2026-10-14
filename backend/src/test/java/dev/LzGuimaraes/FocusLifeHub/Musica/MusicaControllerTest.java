package dev.LzGuimaraes.FocusLifeHub.Musica;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.User.*;
import dev.LzGuimaraes.FocusLifeHub.config.*;
import jakarta.servlet.http.Cookie;

@WebMvcTest(controllers = { MusicaController.class, AdminMusicaController.class })
@Import({ SecurityConfig.class, TokenConfig.class })
@TestPropertySource(properties = { "jwt.secret=test-only-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz-0123456789", "jwt.expiration=604800" })
class MusicaControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TokenConfig tokens;
    @MockitoBean UserRepository users;
    @MockitoBean TokenBlacklistService blacklist;
    @MockitoBean MusicaService service;
    private Cookie autenticar(Role role) {
        var u = new UserModel(); u.setId(1L); u.setNome("Teste"); u.setEmail("teste@example.test"); u.setRole(role); u.setEnabled(true); u.setTokenVersion(0);
        when(users.findById(1L)).thenReturn(Optional.of(u));
        return new Cookie("jwt", tokens.generateToken(u));
    }
    @Test void usuarioListaEstilosESorteiaNoServidor() throws Exception {
        var cookie = autenticar(Role.USER);
        when(service.listarEstilos()).thenReturn(List.of(new MusicaDTO.Estilo(3L, "Rock", 2)));
        when(service.sortear(3L, 9L)).thenReturn(new MusicaDTO.Video(10L, 3L, "https://www.youtube.com/watch?v=X4VbdwhkE10", "https://www.youtube.com/embed/X4VbdwhkE10"));
        mvc.perform(get("/musicas/estilos").cookie(cookie)).andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Rock"));
        mvc.perform(get("/musicas/estilos/3/sortear").param("anteriorId", "9").cookie(cookie)).andExpect(status().isOk()).andExpect(jsonPath("$.estiloId").value(3)).andExpect(jsonPath("$.id").value(10));
    }
    @Test void usuarioNaoPodeAdministrarNenhumaRota() throws Exception {
        var cookie = autenticar(Role.USER);
        mvc.perform(get("/admin/musicas/estilos").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/musicas/estilos/3/videos").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/musicas/estilos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Rock\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/admin/musicas/estilos/3").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Rock\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/admin/musicas/estilos/3").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/musicas/estilos/3/videos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"urls\":[\"https://youtu.be/X4VbdwhkE10\"]}")).andExpect(status().isForbidden());
        mvc.perform(put("/admin/musicas/videos/9").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://youtu.be/X4VbdwhkE10\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/admin/musicas/videos/9").cookie(cookie)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void semLoginNaoAcessaCatalogoNemAdmin() throws Exception {
        mvc.perform(get("/musicas/estilos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/musicas/estilos/3/sortear")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/musicas/estilos")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void adminCriaEstilosEVideosEEditaERemove() throws Exception {
        var cookie = autenticar(Role.ADMIN);
        when(service.salvarEstilo(null, "Rock")).thenReturn(new MusicaDTO.Estilo(3L, "Rock", 0));
        when(service.adicionarVideos(eq(3L), anyList())).thenReturn(new MusicaDTO.Adicao(1, 0));
        mvc.perform(post("/admin/musicas/estilos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Rock\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(3));
        mvc.perform(post("/admin/musicas/estilos/3/videos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"urls\":[\"https://youtu.be/X4VbdwhkE10\"]}")).andExpect(status().isCreated()).andExpect(jsonPath("$.adicionados").value(1));
        mvc.perform(put("/admin/musicas/estilos/3").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Pop\"}")).andExpect(status().isOk());
        mvc.perform(put("/admin/musicas/videos/9").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://youtu.be/X4VbdwhkE10\"}")).andExpect(status().isOk());
        mvc.perform(delete("/admin/musicas/videos/9").cookie(cookie)).andExpect(status().isNoContent());
        mvc.perform(delete("/admin/musicas/estilos/3").cookie(cookie)).andExpect(status().isNoContent());
        verify(service).excluirVideo(9L); verify(service).excluirEstilo(3L);
    }
    @Test void validacaoRejeitaNomeVazioELoteVazioENulos() throws Exception {
        var cookie = autenticar(Role.ADMIN);
        mvc.perform(post("/admin/musicas/estilos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\" \"}")).andExpect(status().isBadRequest());
        for (String json : List.of("{\"urls\":[]}", "{\"urls\":[null]}", "{\"urls\":[\" \"]}")) {
            mvc.perform(post("/admin/musicas/estilos/3/videos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void estiloVazioResponde404() throws Exception {
        var cookie = autenticar(Role.USER);
        when(service.sortear(3L, null)).thenThrow(new ResourceNotFoundException("Este estilo ainda não possui vídeos."));
        mvc.perform(get("/musicas/estilos/3/sortear").cookie(cookie)).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Este estilo ainda não possui vídeos."));
    }
    @Test void papelDoTokenNaoPermiteCadastroAposRebaixamento() throws Exception {
        var cookie = autenticar(Role.ADMIN);
        var user = new UserModel(); user.setId(1L); user.setRole(Role.USER); user.setEnabled(true); user.setTokenVersion(0);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        mvc.perform(post("/admin/musicas/estilos").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Rock\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
