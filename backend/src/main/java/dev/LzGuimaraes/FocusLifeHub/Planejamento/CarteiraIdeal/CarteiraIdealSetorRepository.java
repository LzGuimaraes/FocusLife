package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarteiraIdealSetorRepository extends JpaRepository<CarteiraIdealSetorModel, Long> {

    /** Setores das subclasses informadas, na ordem de exibição. */
    List<CarteiraIdealSetorModel> findBySubclasseIdInOrderByOrdemAscIdAsc(List<Long> subclasseIds);

    void deleteBySubclasseIdIn(List<Long> subclasseIds);
}
