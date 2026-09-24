package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecklistModeloRepository extends JpaRepository<ChecklistModeloModel, Long> {

    Optional<ChecklistModeloModel> findByIdAndUserId(Long id, Long userId);

    /**
     * Checklist padrão de uma SUBCLASSE (um por usuário): a página de notas usa
     * este modelo para desenhar as colunas e para criar o checklist de cada ativo.
     */
    @Query("""
            select distinct m from ChecklistModeloModel m
            left join fetch m.perguntas
            where m.user.id = :userId and m.subclasseSlug = :slug
            """)
    Optional<ChecklistModeloModel> findBySubclasseComPerguntas(@Param("userId") Long userId,
                                                              @Param("slug") String slug);

    /**
     * Modelos do usuário com as perguntas já carregadas (uma query só).
     * A listagem monta a página em memória a partir daqui: evita N+1 e evita
     * combinar paginação com fetch join de coleção.
     */
    @Query("""
            select distinct m from ChecklistModeloModel m
            left join fetch m.perguntas
            where m.user.id = :userId
            """)
    List<ChecklistModeloModel> findAllComPerguntas(@Param("userId") Long userId);

    /** Detalhe com perguntas carregadas (uma coleção só — sem MultipleBagFetch). */
    @Query("""
            select distinct m from ChecklistModeloModel m
            left join fetch m.perguntas
            where m.id = :id and m.user.id = :userId
            """)
    Optional<ChecklistModeloModel> findDetalheByIdAndUserId(@Param("id") Long id,
                                                            @Param("userId") Long userId);
}
