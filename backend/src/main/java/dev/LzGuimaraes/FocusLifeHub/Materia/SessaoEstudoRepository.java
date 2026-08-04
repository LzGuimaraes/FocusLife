package dev.LzGuimaraes.FocusLifeHub.Materia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessaoEstudoRepository extends JpaRepository<SessaoEstudo, Long> {
    Optional<SessaoEstudo> findFirstByMateriaIdAndFimIsNull(Long materiaId);
    List<SessaoEstudo> findByMateriaId(Long materiaId);
    Optional<SessaoEstudo> findByIdAndMateriaId(Long id, Long materiaId);
}
