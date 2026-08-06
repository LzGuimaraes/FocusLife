package dev.LzGuimaraes.FocusLifeHub.Ativo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtivoRepository extends JpaRepository<AtivoModel, Long> {
    Page<AtivoModel> findByCarteiraInvestimento_UserId(Long userId, Pageable pageable);
    List<AtivoModel> findByCarteiraInvestimentoId(Long carteiraId);
}
