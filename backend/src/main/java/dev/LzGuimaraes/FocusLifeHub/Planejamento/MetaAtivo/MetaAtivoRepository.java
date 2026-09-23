package dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetaAtivoRepository extends JpaRepository<MetaAtivoModel, Long> {

    List<MetaAtivoModel> findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(Long carteiraId);

    Optional<MetaAtivoModel> findByIdAndCarteiraInvestimento_UserId(Long id, Long userId);

    Optional<MetaAtivoModel> findByCarteiraInvestimentoIdAndAtivoCadastroId(Long carteiraId, UUID ativoCadastroId);

    void deleteByCarteiraInvestimentoId(Long carteiraId);
}
