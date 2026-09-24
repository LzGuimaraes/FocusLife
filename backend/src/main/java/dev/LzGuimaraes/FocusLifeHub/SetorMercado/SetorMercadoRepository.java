package dev.LzGuimaraes.FocusLifeHub.SetorMercado;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorMercadoRepository extends JpaRepository<SetorMercadoModel, Long> {

    List<SetorMercadoModel> findAllByOrderByNomeAsc();

    List<SetorMercadoModel> findByAtivoTrueOrderByNomeAsc();

    Optional<SetorMercadoModel> findBySlug(String slug);

    Optional<SetorMercadoModel> findByNomeIgnoreCase(String nome);
}
