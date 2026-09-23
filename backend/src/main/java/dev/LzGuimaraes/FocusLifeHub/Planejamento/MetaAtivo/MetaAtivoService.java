package dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroRepository;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Calculo.PercentualCalculator;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSubclasseModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSubclasseRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.dto.MetaAtivoRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.dto.MetaAtivoResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.CarteiraLookup;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;

/**
 * CRUD pontual de metas de ativos e da prioridade manual (Módulo 7).
 *
 * A gravação completa da Carteira Ideal (classe + subclasse + metas em um só
 * payload) fica em {@code CarteiraIdealService.save}; aqui ficam as operações
 * de item único — em especial o ajuste rápido de prioridade, usado na tela de
 * Próximos Aportes sem reenviar a configuração inteira.
 */
@Service
public class MetaAtivoService {

    private final MetaAtivoRepository metaAtivoRepository;
    private final CarteiraIdealSubclasseRepository subclasseRepository;
    private final AtivoCadastroRepository ativoCadastroRepository;
    private final CarteiraLookup carteiraLookup;
    private final ContextoUsuario contextoUsuario;
    private final PercentualCalculator calculator;

    public MetaAtivoService(MetaAtivoRepository metaAtivoRepository,
                            CarteiraIdealSubclasseRepository subclasseRepository,
                            AtivoCadastroRepository ativoCadastroRepository,
                            CarteiraLookup carteiraLookup,
                            ContextoUsuario contextoUsuario,
                            PercentualCalculator calculator) {
        this.metaAtivoRepository = metaAtivoRepository;
        this.subclasseRepository = subclasseRepository;
        this.ativoCadastroRepository = ativoCadastroRepository;
        this.carteiraLookup = carteiraLookup;
        this.contextoUsuario = contextoUsuario;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public List<MetaAtivoResponseDTO> getByCarteira(Long carteiraId) {
        carteiraLookup.exigirCarteiraDoUsuario(carteiraId);
        return metaAtivoRepository.findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MetaAtivoResponseDTO create(MetaAtivoRequestDTO dto) {
        CarteiraInvestimentoModel carteira =
                carteiraLookup.exigirCarteiraDoUsuario(dto.carteira_investimento_id());

        metaAtivoRepository
                .findByCarteiraInvestimentoIdAndAtivoCadastroId(carteira.getId(), dto.ativo_cadastro_id())
                .ifPresent(m -> {
                    throw new BusinessRuleException("Este ativo já possui uma meta nesta carteira.");
                });

        MetaAtivoModel meta = new MetaAtivoModel();
        meta.setCarteiraInvestimento(carteira);
        meta.setAtivoCadastro(exigirAtivoDoCatalogo(dto.ativo_cadastro_id()));
        meta.setClasse(dto.classe());
        meta.setSubclasse(resolverSubclasse(dto.classe(), dto.subclasse_id()));
        meta.setPercentualIdeal(calculator.percentualNormalizado(dto.percentual_ideal()));
        meta.setPrioridadeManual(prioridadeValida(dto.prioridade_manual()));
        meta.setOrdem(dto.ordem() == null ? 0 : dto.ordem());

        return toResponse(metaAtivoRepository.save(meta));
    }

    @Transactional
    public MetaAtivoResponseDTO update(Long id, MetaAtivoRequestDTO dto) {
        MetaAtivoModel meta = exigirDoUsuario(id);

        if (dto.classe() != null) {
            meta.setClasse(dto.classe());
            // A subclasse anterior pode pertencer a outra classe: limpa e resolve de novo.
            meta.setSubclasse(null);
        }
        if (dto.subclasse_id() != null) {
            meta.setSubclasse(resolverSubclasse(meta.getClasse(), dto.subclasse_id()));
        }
        if (dto.ativo_cadastro_id() != null
                && (meta.getAtivoCadastro() == null
                    || !meta.getAtivoCadastro().getId().equals(dto.ativo_cadastro_id()))) {
            meta.setAtivoCadastro(exigirAtivoDoCatalogo(dto.ativo_cadastro_id()));
        }
        if (dto.percentual_ideal() != null) {
            meta.setPercentualIdeal(calculator.percentualNormalizado(dto.percentual_ideal()));
        }
        if (dto.prioridade_manual() != null) {
            meta.setPrioridadeManual(prioridadeValida(dto.prioridade_manual()));
        }
        if (dto.ordem() != null) {
            meta.setOrdem(dto.ordem());
        }

        return toResponse(metaAtivoRepository.save(meta));
    }

    @Transactional
    public MetaAtivoResponseDTO updatePrioridade(Long id, Integer prioridadeManual) {
        MetaAtivoModel meta = exigirDoUsuario(id);
        meta.setPrioridadeManual(prioridadeValida(prioridadeManual));
        return toResponse(metaAtivoRepository.save(meta));
    }

    @Transactional
    public void delete(Long id) {
        metaAtivoRepository.delete(exigirDoUsuario(id));
    }

    /* ── Helpers ── */

    private MetaAtivoModel exigirDoUsuario(Long id) {
        return metaAtivoRepository.findByIdAndCarteiraInvestimento_UserId(id, contextoUsuario.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meta de ativo com ID " + id + " não encontrada"));
    }

    private AtivoCadastroModel exigirAtivoDoCatalogo(java.util.UUID ativoCadastroId) {
        return ativoCadastroRepository.findById(ativoCadastroId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ativo do catálogo com ID " + ativoCadastroId + " não encontrado"));
    }

    private CarteiraIdealSubclasseModel resolverSubclasse(CategoriaInvestimento classe, Long subclasseId) {
        if (subclasseId == null) {
            return null;
        }
        CarteiraIdealSubclasseModel subclasse = subclasseRepository.findById(subclasseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subclasse com ID " + subclasseId + " não encontrada"));
        if (classe != null && subclasse.getClasse() != null && subclasse.getClasse().getClasse() != classe) {
            throw new BusinessRuleException("A subclasse " + subclasse.getNome()
                    + " pertence à classe " + subclasse.getClasse().getClasse()
                    + ", diferente da classe informada (" + classe + ").");
        }
        return subclasse;
    }

    private int prioridadeValida(Integer prioridade) {
        if (prioridade == null) {
            return 0;
        }
        if (prioridade < 0 || prioridade > 10) {
            throw new BusinessRuleException("A prioridade manual vai de 0 a 10.");
        }
        return prioridade;
    }

    private MetaAtivoResponseDTO toResponse(MetaAtivoModel meta) {
        return new MetaAtivoResponseDTO(
                meta.getId(),
                (meta.getCarteiraInvestimento() != null) ? meta.getCarteiraInvestimento().getId() : null,
                (meta.getAtivoCadastro() != null) ? meta.getAtivoCadastro().getId() : null,
                (meta.getAtivoCadastro() != null) ? meta.getAtivoCadastro().getNome() : null,
                meta.getClasse(),
                (meta.getSubclasse() != null) ? meta.getSubclasse().getId() : null,
                (meta.getSubclasse() != null) ? meta.getSubclasse().getNome() : null,
                meta.getPercentualIdeal(),
                meta.getPrioridadeManual(),
                meta.getOrdem());
    }
}
