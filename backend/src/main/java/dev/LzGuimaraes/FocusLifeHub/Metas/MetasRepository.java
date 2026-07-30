package dev.LzGuimaraes.FocusLifeHub.Metas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

@Repository
public interface MetasRepository extends JpaRepository<MetasModel, Long> {
    Page<MetasModel> findByUserId(Long userId, Pageable pageable);
    
}