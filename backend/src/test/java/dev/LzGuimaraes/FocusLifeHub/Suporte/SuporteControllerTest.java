package dev.LzGuimaraes.FocusLifeHub.Suporte;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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

@WebMvcTest(SuporteController.class)
@Import({SecurityConfig.class,TokenConfig.class})
@TestPropertySource(properties={"jwt.secret=test-only-secret-key-0123456789-abcdefghijklmnopqrstuvwxyz-0123456789","jwt.expiration=604800"})
class SuporteControllerTest {
    @Autowired MockMvc mvc; @Autowired TokenConfig tokens;
    @MockitoBean UserRepository users; @MockitoBean TokenBlacklistService blacklist;
    @MockitoBean SuporteService service; @MockitoBean SuporteEmailService emails;
    private Cookie cookie() {
        var u=new UserModel(); u.setId(9L); u.setNome("Maria"); u.setEmail("maria@example.test"); u.setEnabled(true); u.setRole(Role.USER); u.setTokenVersion(0);
        when(users.findById(9L)).thenReturn(Optional.of(u)); return new Cookie("jwt",tokens.generateToken(u));
    }
    private String body(String tipo) { return "{\"solicitacaoId\":\""+UUID.randomUUID()+"\",\"tipo\":\""+tipo+"\",\"titulo\":\"Pedido\",\"descricao\":\"Minha sugestão\",\"userId\":999}"; }
    @Test void usuarioAbreChamadoComSuaIdentidadeERecebeProtocolo() throws Exception {
        var result = new SuporteDTO.Response(7L,"SUP-000007",TipoChamado.NOVA_MUSICA,"Pedido","Minha sugestão",Instant.now(),true);
        when(service.abrir(eq(9L),any())).thenReturn(result); when(service.buscarMeu(9L,7L)).thenReturn(result);
        mvc.perform(post("/suporte/chamados").cookie(cookie()).contentType(MediaType.APPLICATION_JSON).content(body("NOVA_MUSICA")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.protocolo").value("SUP-000007")).andExpect(jsonPath("$.emailEnviado").value(true));
        verify(service).abrir(eq(9L),any()); verify(emails).enviar(7L); verify(service,never()).abrir(eq(999L),any());
    }
    @Test void todasAsCategoriasSaoAceitas() throws Exception {
        var result=new SuporteDTO.Response(7L,"SUP-000007",TipoChamado.MELHORIA,"Pedido","Minha sugestão",Instant.now(),false);
        when(service.abrir(eq(9L),any())).thenReturn(result); when(service.buscarMeu(9L,7L)).thenReturn(result);
        var auth=cookie(); for (TipoChamado tipo:TipoChamado.values()) mvc.perform(post("/suporte/chamados").cookie(auth).contentType(MediaType.APPLICATION_JSON).content(body(tipo.name()))).andExpect(status().isCreated());
    }
    @Test void camposInvalidosNaoCriamChamadoNemEnviamEmail() throws Exception {
        String valido=body("MELHORIA");
        for(String invalido: java.util.List.of(valido.replace("\"Pedido\"","\" \""),valido.replace("\"Minha sugestão\"","\" \""),valido.replace("\"Pedido\"","\""+"a".repeat(121)+"\""),valido.replace("\"Minha sugestão\"","\""+"a".repeat(4001)+"\""),valido.replace("MELHORIA","TIPO_INVALIDO"),"{\"titulo\":\"Pedido\",\"descricao\":\"Texto\"}")) {
            mvc.perform(post("/suporte/chamados").cookie(cookie()).contentType(MediaType.APPLICATION_JSON).content(invalido)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service,emails);
    }
    @Test void semAutenticacaoNaoAbreNemConsultaChamados() throws Exception {
        mvc.perform(post("/suporte/chamados").contentType(MediaType.APPLICATION_JSON).content(body("MELHORIA"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/suporte/chamados")).andExpect(status().isUnauthorized()); mvc.perform(get("/suporte/chamados/7")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service,emails);
    }
    @Test void detalheDeOutroUsuarioRetorna404() throws Exception {
        when(service.buscarMeu(9L,7L)).thenThrow(new ResourceNotFoundException("Chamado não encontrado."));
        mvc.perform(get("/suporte/chamados/7").cookie(cookie())).andExpect(status().isNotFound());
        verify(service).buscarMeu(9L,7L);
    }
    @Test void listagemUsaUsuarioAutenticadoEPaginaSolicitada() throws Exception {
        when(service.listarMeus(9L,2)).thenReturn(org.springframework.data.domain.Page.empty());
        mvc.perform(get("/suporte/chamados").param("page","2").param("userId","999").cookie(cookie())).andExpect(status().isOk()); verify(service).listarMeus(9L,2);
    }
}
