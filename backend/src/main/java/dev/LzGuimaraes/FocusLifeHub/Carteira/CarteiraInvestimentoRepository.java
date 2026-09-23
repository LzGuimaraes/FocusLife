package dev.LzGuimaraes.FocusLifeHub.Carteira;

import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public interface CarteiraInvestimentoRepository extends CarteiraRepository<CarteiraInvestimentoModel> {

    /** Carteiras vinculadas a uma estratégia (usado ao excluir a estratégia). */
    List<CarteiraInvestimentoModel> findByEstrategiaId(Long estrategiaId);
}
