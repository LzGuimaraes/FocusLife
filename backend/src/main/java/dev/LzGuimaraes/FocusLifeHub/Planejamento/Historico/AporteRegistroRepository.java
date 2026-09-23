package dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AporteRegistroRepository extends JpaRepository<AporteRegistroModel, Long> {

    List<AporteRegistroModel> findByCarteiraInvestimentoIdOrderByDataDescIdDesc(
            Long carteiraInvestimentoId);

    /** Aportes recentes — usado pelo motor para avisar sobre concentração. */
    List<AporteRegistroModel> findByCarteiraInvestimentoIdAndDataGreaterThanEqual(
            Long carteiraInvestimentoId, LocalDate desde);

    Optional<AporteRegistroModel> findByIdAndUserId(Long id, Long userId);
}
