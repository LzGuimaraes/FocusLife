package dev.LzGuimaraes.FocusLifeHub.Suporte;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SuporteRepository extends JpaRepository<SuporteChamadoModel, Long> {
    Optional<SuporteChamadoModel> findByUserIdAndSolicitacaoId(Long userId, UUID solicitacaoId);
    Optional<SuporteChamadoModel> findByIdAndUserId(Long id, Long userId);
    Page<SuporteChamadoModel> findByUserIdOrderByCriadoEmDescIdDesc(Long userId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SuporteChamadoModel c where c.id = :id")
    Optional<SuporteChamadoModel> buscarParaEnviar(@Param("id") Long id);
    @Query("select c.id from SuporteChamadoModel c where c.emailEnviado = false order by c.tentativas, c.criadoEm, c.id")
    List<Long> buscarPendentes(Pageable pageable);
}
