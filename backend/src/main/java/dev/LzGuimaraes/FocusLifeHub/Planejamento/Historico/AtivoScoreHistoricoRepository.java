package dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtivoScoreHistoricoRepository extends JpaRepository<AtivoScoreHistoricoModel, Long> {

    Optional<AtivoScoreHistoricoModel> findByIdAndUserId(Long id, Long userId);

    Optional<AtivoScoreHistoricoModel> findByUserIdAndAtivoCadastroIdAndDataReferencia(
            Long userId, UUID ativoCadastroId, LocalDate dataReferencia);

    Optional<AtivoScoreHistoricoModel> findByUserIdAndAtivoIdAndDataReferencia(
            Long userId, Long ativoId, LocalDate dataReferencia);

    List<AtivoScoreHistoricoModel> findByUserIdAndAtivoCadastroIdOrderByDataReferenciaAsc(
            Long userId, UUID ativoCadastroId);

    List<AtivoScoreHistoricoModel> findByUserIdAndAtivoIdOrderByDataReferenciaAsc(
            Long userId, Long ativoId);

    List<AtivoScoreHistoricoModel> findByUserIdAndCarteiraInvestimentoIdOrderByDataReferenciaAsc(
            Long userId, Long carteiraInvestimentoId);

    /** Histórico completo do usuário (mais recente primeiro) — base das listagens. */
    List<AtivoScoreHistoricoModel> findByUserIdOrderByDataReferenciaDescIdDesc(Long userId);
}
