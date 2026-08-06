package dev.LzGuimaraes.FocusLifeHub.Despesa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DespesaLogRepository extends JpaRepository<DespesaLogModel, Long> {
    Page<DespesaLogModel> findByContaId(Long contaId, Pageable pageable);
}
