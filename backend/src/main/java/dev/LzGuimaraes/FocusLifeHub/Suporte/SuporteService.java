package dev.LzGuimaraes.FocusLifeHub.Suporte;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

@Service
public class SuporteService {
    private final SuporteRepository chamados;
    private final UserRepository usuarios;
    public SuporteService(SuporteRepository chamados, UserRepository usuarios) { this.chamados = chamados; this.usuarios = usuarios; }

    @Transactional
    public SuporteDTO.Response abrir(Long usuarioId, SuporteDTO.Request request) {
        var existente = chamados.findByUserIdAndSolicitacaoId(usuarioId, request.solicitacaoId());
        if (existente.isPresent()) return SuporteDTO.Response.from(existente.get());
        var usuario = usuarios.findById(usuarioId).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        var chamado = new SuporteChamadoModel();
        chamado.setUser(usuario);
        chamado.setUsuarioNome(usuario.getNome());
        chamado.setUsuarioEmail(usuario.getEmail());
        chamado.setSolicitacaoId(request.solicitacaoId());
        chamado.setTipo(request.tipo());
        chamado.setTitulo(request.titulo().trim());
        chamado.setDescricao(request.descricao().trim());
        chamado.setCriadoEm(Instant.now());
        return SuporteDTO.Response.from(chamados.saveAndFlush(chamado));
    }
    @Transactional(readOnly = true)
    public SuporteDTO.Response buscarMeu(Long usuarioId, Long id) {
        return chamados.findByIdAndUserId(id, usuarioId).map(SuporteDTO.Response::from)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado."));
    }
    @Transactional(readOnly = true)
    public Page<SuporteDTO.Response> listarMeus(Long usuarioId, int pagina) {
        return chamados.findByUserIdOrderByCriadoEmDescIdDesc(usuarioId, PageRequest.of(Math.max(0, pagina), 8)).map(SuporteDTO.Response::from);
    }
}
