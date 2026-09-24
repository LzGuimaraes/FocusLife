package dev.LzGuimaraes.FocusLifeHub.SetorMercado;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorMercadoRepository extends JpaRepository<SetorMercadoModel, Long> {

    List<SetorMercadoModel> findAllByOrderByNomeAsc();

    List<SetorMercadoModel> findByAtivoTrueOrderByNomeAsc();

    /** Chave de REUSO do catálogo: nome normalizado ("Bancos" = "bancos"). */
    Optional<SetorMercadoModel> findBySlug(String slug);
}
