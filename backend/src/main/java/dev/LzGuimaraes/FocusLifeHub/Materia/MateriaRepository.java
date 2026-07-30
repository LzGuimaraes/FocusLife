package dev.LzGuimaraes.FocusLifeHub.Materia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

@Repository
public interface MateriaRepository extends JpaRepository <MateriaModel, Long> {
    Page<MateriaModel> findByUserId(Long userId, Pageable pageable);
}
