package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.AporteConfigDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * MARGEM OPERACIONAL do motor de aporte — o único parâmetro configurável que
 * sobrou, e o único que faltava.
 *
 * A margem é RELATIVA à meta: limite = meta × (1 + margem). Com meta de 5% e
 * margem de 5% o ativo pode ir até 5,25% do patrimônio projetado. Ela existe
 * para o motor não tratar uma diferença de centavos como "está acima do alvo" e
 * para dar um teto operacional explícito (que o limite cadastrado, quando
 * existir, pode reduzir — nunca aumentar).
 *
 * Faixa aceita: 3% a 5%. Padrão: 5% (sem linha gravada, nada é criado).
 */
@Service
public class AporteConfigService {

    private final AporteConfigRepository repository;
    private final UserRepository userRepository;
    private final ContextoUsuario contexto;

    public AporteConfigService(AporteConfigRepository repository, UserRepository userRepository,
                               ContextoUsuario contexto) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.contexto = contexto;
    }

    /** Margem do usuário logado, em % (nunca null). */
    @Transactional(readOnly = true)
    public double margemPercentual() {
        return repository.findByUserId(contexto.id())
                .map(AporteConfigModel::getMargemPercentual)
                .filter(m -> m != null && m.signum() > 0)
                .orElseGet(() -> BigDecimal.valueOf(ReferenciaAporte.MARGEM_PADRAO))
                .doubleValue();
    }

    @Transactional(readOnly = true)
    public AporteConfigDTO obter() {
        Optional<BigDecimal> gravada = repository.findByUserId(contexto.id())
                .map(AporteConfigModel::getMargemPercentual)
                .filter(m -> m != null && m.signum() > 0);
        return new AporteConfigDTO(
                gravada.orElseGet(() -> BigDecimal.valueOf(ReferenciaAporte.MARGEM_PADRAO)),
                BigDecimal.valueOf(ReferenciaAporte.MARGEM_MINIMA),
                BigDecimal.valueOf(ReferenciaAporte.MARGEM_MAXIMA),
                BigDecimal.valueOf(ReferenciaAporte.MARGEM_PADRAO),
                gravada.isPresent());
    }

    /**
     * Grava a margem do usuário. O PUT CRIA a linha na primeira vez — antes
     * disso o GET responde o padrão sem persistir nada.
     */
    @Transactional
    public AporteConfigDTO salvar(BigDecimal margemPercentual) {
        if (margemPercentual == null) {
            throw new BusinessRuleException("Informe a margem operacional (entre "
                    + ReferenciaAporte.MARGEM_MINIMA + "% e " + ReferenciaAporte.MARGEM_MAXIMA + "%).");
        }
        double valor = margemPercentual.doubleValue();
        if (valor < ReferenciaAporte.MARGEM_MINIMA || valor > ReferenciaAporte.MARGEM_MAXIMA) {
            throw new BusinessRuleException("A margem operacional deve ficar entre "
                    + ReferenciaAporte.MARGEM_MINIMA + "% e " + ReferenciaAporte.MARGEM_MAXIMA
                    + "% (relativa à meta: meta de 5% com margem de 5% dá limite de 5,25%).");
        }
        AporteConfigModel config = repository.findByUserId(contexto.id())
                .orElseGet(AporteConfigModel::new);
        if (config.getId() == null) {
            UserModel usuario = userRepository.findById(contexto.id())
                    .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado."));
            config.setUser(usuario);
        }
        config.setMargemPercentual(margemPercentual.setScale(2, java.math.RoundingMode.HALF_UP));
        config.setUpdatedAt(LocalDateTime.now());
        repository.save(config);
        return obter();
    }
}
