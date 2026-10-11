package dev.LzGuimaraes.FocusLifeHub.Estudos;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

@Repository
public interface EstudosRepository extends JpaRepository<EstudosModel, Long> {
    Page<EstudosModel> findByMateria_UserId(Long userId, Pageable pageable);

    /**
     * Registros de estudo do usuário. `tb-estudos` NÃO tem user_id: a posse vem da
     * matéria ({@code tb-estudos.materia_id -> tb_materia.user_id}).
     */
    long countByMateria_UserId(Long userId);

    /**
     * Consulta ADMIN dos registros de estudo de UM usuário-alvo.
     *
     * O `user_id` do alvo é resolvido pela matéria dona e SEMPRE entra na query
     * com {@code AND}. Um {@code materiaId} de outro usuário que venha por
     * parâmetro apenas zera o resultado — nunca amplia o escopo (a posse é
     * revalidada no banco, não confiada ao parâmetro).
     *
     * Cada filtro tem uma flag de presença (evita {@code :x is null or ...}:
     * o Postgres não consegue inferir o tipo de um parâmetro nulo usado só no
     * teste {@code is null}).
     */
    @Query("select e from EstudosModel e "
            + "where e.materia.user.id = :userId "
            + "and (:temMateria = false or e.materia.id = :materiaId) "
            + "and (:temDe = false or e.data >= :de) "
            + "and (:temAte = false or e.data <= :ate)")
    Page<EstudosModel> buscarDoUsuario(@Param("userId") Long userId,
                                       @Param("temMateria") boolean temMateria,
                                       @Param("materiaId") Long materiaId,
                                       @Param("temDe") boolean temDe,
                                       @Param("de") LocalDate de,
                                       @Param("temAte") boolean temAte,
                                       @Param("ate") LocalDate ate,
                                       Pageable pageable);
}
