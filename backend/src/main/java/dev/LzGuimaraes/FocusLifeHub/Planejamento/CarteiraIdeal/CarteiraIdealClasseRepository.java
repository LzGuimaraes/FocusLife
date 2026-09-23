package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

@Repository
public interface CarteiraIdealClasseRepository extends JpaRepository<CarteiraIdealClasseModel, Long> {

    List<CarteiraIdealClasseModel> findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(Long carteiraId);

    Optional<CarteiraIdealClasseModel> findByCarteiraInvestimentoIdAndClasse(Long carteiraId,
                                                                            CategoriaInvestimento classe);

    void deleteByCarteiraInvestimentoId(Long carteiraId);
}
