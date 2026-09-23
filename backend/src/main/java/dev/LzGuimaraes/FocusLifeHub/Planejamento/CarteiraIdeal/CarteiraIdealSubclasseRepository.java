package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarteiraIdealSubclasseRepository extends JpaRepository<CarteiraIdealSubclasseModel, Long> {

    List<CarteiraIdealSubclasseModel> findByClasseIdInOrderByOrdemAscIdAsc(Collection<Long> classeIds);

    Optional<CarteiraIdealSubclasseModel> findByClasseIdAndNomeIgnoreCase(Long classeId, String nome);
}
