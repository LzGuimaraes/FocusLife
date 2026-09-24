package dev.LzGuimaraes.FocusLifeHub.SetorMercado;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.LzGuimaraes.FocusLifeHub.SetorMercado.dto.SetorMercadoDTO;
import jakarta.validation.Valid;

/**
 * Catálogo global de setores.
 *
 * É uma tabela de REFERÊNCIA (não é dado do usuário): a mesma lista serve para
 * qualquer carteira. Por isso não há filtro por usuário — só autenticação. A
 * manutenção pode ser feita por aqui ou direto no banco.
 */
@RestController
@RequestMapping("/setores-mercado")
public class SetorMercadoController {

    private final SetorMercadoService setorMercadoService;

    public SetorMercadoController(SetorMercadoService setorMercadoService) {
        this.setorMercadoService = setorMercadoService;
    }

    /** `?somente_ativos=true` alimenta os seletores da tela. */
    @GetMapping
    public ResponseEntity<List<SetorMercadoDTO.Response>> listar(
            @RequestParam(name = "somente_ativos", defaultValue = "false") boolean somenteAtivos) {
        return ResponseEntity.ok(setorMercadoService.listar(somenteAtivos));
    }

    /** Cria pelo nome (idempotente: nome repetido devolve o setor existente). */
    @PostMapping
    public ResponseEntity<SetorMercadoDTO.Response> criar(@Valid @RequestBody SetorMercadoDTO.Request req) {
        SetorMercadoModel setor = setorMercadoService.criarOuObter(req.nome(), req.segmento());
        return ResponseEntity.status(HttpStatus.CREATED).body(setorMercadoService.buscar(setor.getId()));
    }

    /** Renomeia / define segmento / ATIVA-DESATIVA (desativar esconde sem apagar). */
    @PutMapping("/{id}")
    public ResponseEntity<SetorMercadoDTO.Response> alterar(@PathVariable Long id,
                                                            @Valid @RequestBody SetorMercadoDTO.AlterRequest req) {
        return ResponseEntity.ok(setorMercadoService.alterar(id, req));
    }

    /** Só exclui setor sem uso — em uso, a resposta orienta a desativar. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        setorMercadoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
