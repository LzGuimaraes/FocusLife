package dev.LzGuimaraes.FocusLifeHub.Contas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContaLogRepository extends JpaRepository<ContaLogModel, Long> {
    Page<ContaLogModel> findByContaId(Long contaId, Pageable pageable);
}
