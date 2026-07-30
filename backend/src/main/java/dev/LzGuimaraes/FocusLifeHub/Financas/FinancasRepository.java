package dev.LzGuimaraes.FocusLifeHub.Financas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

@Repository
public interface FinancasRepository extends JpaRepository<FinancasModel, Long> {
    Page<FinancasModel> findByUserId(Long userId, Pageable pageable);
}