package dev.LzGuimaraes.FocusLifeHub.Ativo;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtivoRepository extends JpaRepository<AtivoModel, Long> {
    Page<AtivoModel> findByCarteiraInvestimento_UserId(Long userId, Pageable pageable);
    List<AtivoModel> findByCarteiraInvestimentoId(Long carteiraId);
    List<AtivoModel> findByAtivoCadastroId(UUID ativoCadastroId);

    /** Posições do usuário, restritas a um conjunto de IDs (usado ao vincular ao catálogo). */
    List<AtivoModel> findByIdInAndCarteiraInvestimento_UserId(Collection<Long> ids, Long userId);

    /**
     * Posições SEM carteira (dados legados anteriores à divisão das carteiras).
     * Como todo acesso a `ativo` passa pela carteira, uma linha assim fica
     * invisível em todas as telas — é o que o diagnóstico financeiro detecta.
     */
    List<AtivoModel> findByCarteiraInvestimentoIsNull();

    long countByCarteiraInvestimentoIsNull();
}
