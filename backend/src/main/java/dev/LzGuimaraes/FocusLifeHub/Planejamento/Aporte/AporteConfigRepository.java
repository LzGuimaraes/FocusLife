package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AporteConfigRepository extends JpaRepository<AporteConfigModel, Long> {

    /** Configuração do motor de aporte do usuário (uma só, por construção). */
    Optional<AporteConfigModel> findByUserId(Long userId);
}
