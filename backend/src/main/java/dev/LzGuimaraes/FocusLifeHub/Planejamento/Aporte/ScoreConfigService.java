package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.ScoreConfigDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * Configuração de pontuação do usuário (Módulo 6).
 *
 * Não existe configuração "do sistema": os pesos padrão de {@link TermoScore}
 * são apenas o ponto de partida. Enquanto o usuário não salvar, nada é gravado
 * no banco e o GET devolve os padrões com `personalizada = false`.
 */
@Service
public class ScoreConfigService {

    private final ScoreConfigRepository configRepository;
    private final UserRepository userRepository;
    private final ContextoUsuario contextoUsuario;

    public ScoreConfigService(ScoreConfigRepository configRepository,
                              UserRepository userRepository,
                              ContextoUsuario contextoUsuario) {
        this.configRepository = configRepository;
        this.userRepository = userRepository;
        this.contextoUsuario = contextoUsuario;
    }

    /** Configuração vigente ou os padrões (sem gravar nada). */
    @Transactional(readOnly = true)
    public ScoreConfigModel obterOuPadrao() {
        return obterOuPadrao(contextoUsuario.id());
    }

    @Transactional(readOnly = true)
    public ScoreConfigModel obterOuPadrao(Long userId) {
        return configRepository.findByUserId(userId).orElseGet(ScoreConfigModel::comPadroes);
    }

    @Transactional(readOnly = true)
    public ScoreConfigDTO.Response get() {
        Long userId = contextoUsuario.id();
        var existente = configRepository.findByUserId(userId);
        return toResponse(existente.orElseGet(ScoreConfigModel::comPadroes), existente.isPresent());
    }

    /** Catálogo dos termos da fórmula (transparência para a tela de configuração). */
    @Transactional(readOnly = true)
    public List<ScoreConfigDTO.Termo> termos() {
        return catalogo(obterOuPadrao());
    }

    @Transactional
    public ScoreConfigDTO.Response salvar(ScoreConfigDTO.Request dto) {
        Long userId = contextoUsuario.id();

        ScoreConfigModel config = configRepository.findByUserId(userId).orElseGet(() -> {
            UserModel user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + userId + " não encontrado"));
            ScoreConfigModel novo = ScoreConfigModel.comPadroes();
            novo.setUser(user);
            return novo;
        });

        if (dto.peso_quality() != null) {
            config.setPesoQuality(dto.peso_quality());
        }
        if (dto.peso_deficit() != null) {
            config.setPesoDeficit(dto.peso_deficit());
        }
        if (dto.peso_excesso() != null) {
            config.setPesoExcesso(dto.peso_excesso());
        }
        if (dto.peso_prioridade() != null) {
            config.setPesoPrioridade(dto.peso_prioridade());
        }
        if (dto.estrategia_aporte() != null) {
            config.setEstrategiaAporte(dto.estrategia_aporte());
        }

        // Sem nenhum peso não existe fórmula: o denominador seria zero.
        if (config.somaPesos().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(
                    "Informe ao menos um peso maior que zero — sem isso não há como calcular a prioridade de aporte.");
        }

        config.setUpdatedAt(LocalDateTime.now());
        return toResponse(configRepository.save(config), true);
    }

    /** Volta aos pesos padrão do sistema. */
    @Transactional
    public ScoreConfigDTO.Response restaurarPadroes() {
        configRepository.findByUserId(contextoUsuario.id()).ifPresent(configRepository::delete);
        return toResponse(ScoreConfigModel.comPadroes(), false);
    }

    /* ── Helpers ── */

    private ScoreConfigDTO.Response toResponse(ScoreConfigModel config, boolean personalizada) {
        return new ScoreConfigDTO.Response(
                config.getId(),
                config.getPesoQuality(),
                config.getPesoDeficit(),
                config.getPesoExcesso(),
                config.getPesoPrioridade(),
                // α efetivo: é 0 quando o ativo não tem Quality Score (termo sai da conta).
                config.getPesoQuality(),
                config.somaPesos(),
                config.getEstrategiaAporte(),
                personalizada,
                catalogo(config));
    }

    private List<ScoreConfigDTO.Termo> catalogo(ScoreConfigModel config) {
        List<ScoreConfigDTO.Termo> termos = new ArrayList<>();
        for (TermoScore termo : TermoScore.values()) {
            termos.add(new ScoreConfigDTO.Termo(
                    termo,
                    termo.getLabel(),
                    termo.getDescricao(),
                    config.pesoDe(termo),
                    termo.getPesoPadrao()));
        }
        return termos;
    }
}
