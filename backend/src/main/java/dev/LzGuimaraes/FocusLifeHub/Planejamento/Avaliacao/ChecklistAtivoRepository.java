package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecklistAtivoRepository extends JpaRepository<ChecklistAtivoModel, Long> {

    Optional<ChecklistAtivoModel> findByIdAndUserId(Long id, Long userId);

    /**
     * Checklists do usuário com as perguntas carregadas em UMA query
     * (evita N+1 ao calcular os scores). A ordenação é feita em memória para
     * não combinar `distinct` com `order by` no SQL.
     */
    @Query("""
            select distinct c from ChecklistAtivoModel c
            left join fetch c.perguntas
            where c.user.id = :userId
            """)
    List<ChecklistAtivoModel> findAllComPerguntas(@Param("userId") Long userId);

    @Query("""
            select distinct c from ChecklistAtivoModel c
            left join fetch c.perguntas
            where c.user.id = :userId and c.ativoCadastro.id = :ativoCadastroId
            """)
    List<ChecklistAtivoModel> findByAtivoCadastroComPerguntas(@Param("userId") Long userId,
                                                              @Param("ativoCadastroId") UUID ativoCadastroId);

    @Query("""
            select distinct c from ChecklistAtivoModel c
            left join fetch c.perguntas
            where c.user.id = :userId and c.ativo.id = :ativoId
            """)
    List<ChecklistAtivoModel> findByAtivoComPerguntas(@Param("userId") Long userId,
                                                      @Param("ativoId") Long ativoId);

    @Query("""
            select distinct c from ChecklistAtivoModel c
            left join fetch c.perguntas
            where c.user.id = :userId and c.subclasseSlug = :slug
            """)
    List<ChecklistAtivoModel> findBySubclasseComPerguntas(@Param("userId") Long userId,
                                                          @Param("slug") String slug);

    long countByUserIdAndAtivoCadastroId(Long userId, UUID ativoCadastroId);
}
