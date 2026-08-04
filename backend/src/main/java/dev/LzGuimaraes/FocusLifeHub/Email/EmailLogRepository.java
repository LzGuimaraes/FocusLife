package dev.LzGuimaraes.FocusLifeHub.Email;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLogModel, Long> {

    // @Query explícito: os campos do EmailLogModel são snake_case (referencia_id,
    // data_referencia), então uma derived query (ex.: existsByReferenciaId) não
    // resolve a propriedade corretamente.
    @Query("SELECT COUNT(e) > 0 FROM EmailLogModel e "
         + "WHERE e.tipo = :tipo AND e.referencia_id = :referenciaId AND e.data_referencia = :dataReferencia")
    boolean existsByTipoAndReferenciaIdAndDataReferencia(
            @Param("tipo") TipoEmailLog tipo,
            @Param("referenciaId") Long referenciaId,
            @Param("dataReferencia") LocalDate dataReferencia);
}
