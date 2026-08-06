package dev.LzGuimaraes.FocusLifeHub.Despesa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DespesaRepository extends JpaRepository<DespesaModel, Long> {
    Page<DespesaModel> findByCarteiraDividas_UserId(Long userId, Pageable pageable);
    List<DespesaModel> findByCarteiraDividasId(Long carteiraId);
    List<DespesaModel> findByPagoFalseAndDataVencimento(LocalDate dataVencimento);
    List<DespesaModel> findByCarteiraDividas_UserIdAndPagoFalseAndDataVencimento(Long userId, LocalDate dataVencimento);
}
