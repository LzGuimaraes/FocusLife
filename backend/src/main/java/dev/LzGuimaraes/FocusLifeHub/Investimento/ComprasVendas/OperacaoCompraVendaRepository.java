package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperacaoCompraVendaRepository extends JpaRepository<OperacaoCompraVendaModel, Long> {

    List<OperacaoCompraVendaModel> findByUserIdOrderByDataCompraDescIdDesc(Long userId);

    List<OperacaoCompraVendaModel> findByUserIdAndCarteiraInvestimentoIdOrderByDataCompraDescIdDesc(
            Long userId, Long carteiraInvestimentoId);

    Optional<OperacaoCompraVendaModel> findByIdAndUserId(Long id, Long userId);

    /** Operações de compra/venda do usuário (operacao_compra_venda.user_id, escalar). */
    long countByUserId(Long userId);
}
