package dev.LzGuimaraes.FocusLifeHub.SetorMercado;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSetorRepository;
import dev.LzGuimaraes.FocusLifeHub.SetorMercado.dto.SetorMercadoDTO;

/**
 * CATÁLOGO DE SETORES — identidade estável para "setor" no sistema inteiro.
 *
 * Duas portas de entrada, de propósito:
 *
 *   1. MANUTENÇÃO MANUAL (banco/`POST /ativos/admin/sync`): quem já tem a lista
 *      de setores classifica o catálogo direto, sem passar pela tela.
 *   2. CRIAÇÃO PELA TELA: quando o usuário digita um setor novo na Carteira
 *      Ideal, ele nasce aqui automaticamente (e passa a valer para as próximas
 *      carteiras e para os tickers classificados).
 *
 * O reuso é por NOME NORMALIZADO (`slug`): "Bancos", "bancos" e "BANCOS" caem
 * na mesma linha, então a origem do cadastro não duplica o setor.
 */
@Service
public class SetorMercadoService {

    private final SetorMercadoRepository setorRepository;
    private final AtivoCadastroRepository ativoCadastroRepository;
    private final CarteiraIdealSetorRepository carteiraIdealSetorRepository;

    public SetorMercadoService(SetorMercadoRepository setorRepository,
                               AtivoCadastroRepository ativoCadastroRepository,
                               CarteiraIdealSetorRepository carteiraIdealSetorRepository) {
        this.setorRepository = setorRepository;
        this.ativoCadastroRepository = ativoCadastroRepository;
        this.carteiraIdealSetorRepository = carteiraIdealSetorRepository;
    }

    /** Lista o catálogo, opcionalmente só os ativos (usado pelos seletores). */
    @Transactional(readOnly = true)
    public List<SetorMercadoDTO.Response> listar(boolean somenteAtivos) {
        List<SetorMercadoModel> setores = somenteAtivos
                ? setorRepository.findByAtivoTrueOrderByNomeAsc()
                : setorRepository.findAllByOrderByNomeAsc();
        return setores.stream().map(this::toResponse).toList();
    }

    /**
     * Cria (ou devolve o já existente) a partir de um NOME.
     *
     * Idempotente pelo slug: é o que permite chamar isto a cada save da Carteira
     * Ideal sem risco de duplicar setor — e é o mesmo caminho do cadastro manual.
     */
    @Transactional
    public SetorMercadoModel criarOuObter(String nome, String segmento) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRuleException("Informe o nome do setor.");
        }
        String limpo = nome.trim();
        String slug = SetorMercadoModel.slugDe(limpo);
        if (slug.isEmpty()) {
            throw new BusinessRuleException("O nome do setor precisa ter ao menos uma letra ou número.");
        }

        return setorRepository.findBySlug(slug)
                .map(existente -> {
                    // Setor reativado ao ser escolhido de novo numa carteira.
                    if (Boolean.FALSE.equals(existente.getAtivo())) {
                        existente.setAtivo(true);
                    }
                    if (segmento != null && !segmento.isBlank() && existente.getSegmento() == null) {
                        existente.setSegmento(segmento.trim());
                    }
                    return setorRepository.save(existente);
                })
                .orElseGet(() -> {
                    SetorMercadoModel novo = new SetorMercadoModel();
                    novo.setNome(limpo);
                    novo.setSlug(slug);
                    novo.setSegmento((segmento != null && !segmento.isBlank()) ? segmento.trim() : null);
                    novo.setAtivo(true);
                    novo.setCreatedAt(LocalDateTime.now());
                    return setorRepository.save(novo);
                });
    }

    /** Resolve por id (usado quando a tela manda o setor do catálogo já escolhido). */
    @Transactional(readOnly = true)
    public SetorMercadoModel exigir(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setor com ID " + id + " não encontrado"));
    }

    /** Um setor já com as contagens de uso (resposta do POST/PUT). */
    @Transactional(readOnly = true)
    public SetorMercadoDTO.Response buscar(Long id) {
        return toResponse(exigir(id));
    }

    @Transactional
    public SetorMercadoDTO.Response alterar(Long id, SetorMercadoDTO.AlterRequest req) {
        SetorMercadoModel setor = exigir(id);

        if (req.nome() != null && !req.nome().isBlank()) {
            String limpo = req.nome().trim();
            String slug = SetorMercadoModel.slugDe(limpo);
            if (!slug.equals(setor.getSlug())) {
                // Renomear para um nome que já existe juntaria dois setores no mesmo
                // balde sem o usuário perceber: melhor barrar e ele escolher o outro.
                setorRepository.findBySlug(slug).ifPresent(outro -> {
                    throw new BusinessRuleException("Já existe um setor com esse nome (\"" + outro.getNome() + "\").");
                });
                setor.setNome(limpo);
                setor.setSlug(slug);
            }
        }
        if (req.segmento() != null) {
            setor.setSegmento(req.segmento().isBlank() ? null : req.segmento().trim());
        }
        if (req.ativo() != null) {
            setor.setAtivo(req.ativo());
        }
        return toResponse(setorRepository.save(setor));
    }

    /**
     * Exclui um setor do catálogo — só quando ninguém o usa.
     *
     * Setor em uso não é erro do sistema: é decisão do usuário, então a resposta
     * diz exatamente quem está usando e sugere DESATIVAR (que esconde da tela
     * sem mexer em histórico).
     */
    @Transactional
    public void excluir(Long id) {
        SetorMercadoModel setor = exigir(id);
        long ativos = ativoCadastroRepository.countBySetorMercadoId(id);
        long alvos = carteiraIdealSetorRepository.countBySetorMercadoId(id);
        if (ativos > 0 || alvos > 0) {
            throw new BusinessRuleException("O setor \"" + setor.getNome() + "\" está em uso por "
                    + ativos + " ativo(s) do catálogo e " + alvos + " alvo(s) de carteira. "
                    + "Desative o setor (ele sai das listas e o histórico continua) em vez de excluir.");
        }
        setorRepository.delete(setor);
    }

    private SetorMercadoDTO.Response toResponse(SetorMercadoModel s) {
        return new SetorMercadoDTO.Response(
                s.getId(), s.getNome(), s.getSlug(), s.getSegmento(), s.getAtivo(),
                ativoCadastroRepository.countBySetorMercadoId(s.getId()),
                carteiraIdealSetorRepository.countBySetorMercadoId(s.getId()));
    }
}
