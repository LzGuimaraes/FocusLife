package dev.LzGuimaraes.FocusLifeHub.AtivoCadastro;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.dto.AtivoCadastroResponseDTO;

@Service
public class AtivoCadastroService {

    private final AtivoCadastroRepository ativoCadastroRepository;

    public AtivoCadastroService(AtivoCadastroRepository ativoCadastroRepository) {
        this.ativoCadastroRepository = ativoCadastroRepository;
    }

    /**
     * Lista os ativos cadastrados (renda variável), opcionalmente filtrando
     * por nome/ticker (case-insensitive) para o autocomplete.
     */
    public List<AtivoCadastroResponseDTO> listar(String q) {
        List<AtivoCadastroModel> ativos = (q == null || q.isBlank())
                ? ativoCadastroRepository.findAllByOrderByNomeAsc()
                : ativoCadastroRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(q.trim());

        return ativos.stream()
                .map(a -> new AtivoCadastroResponseDTO(a.getId(), a.getNome(), a.getTipo(), a.getPrecoAtual()))
                .collect(Collectors.toList());
    }
}
