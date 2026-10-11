package dev.LzGuimaraes.FocusLifeHub.Metas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import dev.LzGuimaraes.FocusLifeHub.Metas.Enum.MetaStatus;

@Repository
public interface MetasRepository extends JpaRepository<MetasModel, Long> {
    Page<MetasModel> findByUserId(Long userId, Pageable pageable);

    /** Metas do usuário (tb_metas.user_id). */
    long countByUserId(Long userId);

    /**
     * Consulta ADMIN das metas de UM usuário-alvo. O {@code userId} é sempre
     * aplicado com {@code AND}, então o filtro de status nunca amplia o escopo.
     * A flag {@code temStatus} evita o teste {@code :status is null}, que o
     * Postgres não consegue tipar.
     */
    @Query("select m from MetasModel m "
            + "where m.user.id = :userId "
            + "and (:temStatus = false or m.status = :status)")
    Page<MetasModel> buscarDoUsuario(@Param("userId") Long userId,
                                     @Param("temStatus") boolean temStatus,
                                     @Param("status") MetaStatus status,
                                     Pageable pageable);
}