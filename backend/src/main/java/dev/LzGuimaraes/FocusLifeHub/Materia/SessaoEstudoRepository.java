package dev.LzGuimaraes.FocusLifeHub.Materia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SessaoEstudoRepository extends JpaRepository<SessaoEstudo, Long> {
    Optional<SessaoEstudo> findFirstByMateriaIdAndFimIsNull(Long materiaId);
    List<SessaoEstudo> findByMateriaId(Long materiaId);
    Optional<SessaoEstudo> findByIdAndMateriaId(Long id, Long materiaId);

    /**
     * Sessões de estudo do usuário. `sessao_estudo` guarda `materia_id` como coluna
     * ESCALAR (sem FK), então a posse é resolvida pela matéria dona — em uma única
     * query (sem N+1).
     */
    @Query("select count(s) from SessaoEstudo s "
            + "where s.materiaId in (select m.id from MateriaModel m where m.user.id = :userId)")
    long countDoUsuario(@Param("userId") Long userId);
}
