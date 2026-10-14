package dev.LzGuimaraes.FocusLifeHub.Tarefas;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.Prioridade;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.Email.TipoEmailLog;

@Repository
public interface TarefasRepository extends JpaRepository<TarefasModel, Long> {
    Page<TarefasModel> findByUserId(Long userId, Pageable pageable);
    List<TarefasModel> findByUserIdAndPrazo(Long userId, LocalDate prazo);
    List<TarefasModel> findByPrazoAndStatusNot(LocalDate prazo, TarefaStatus status);

    /** Destinatário já carregado: funciona no scheduler sem sessão HTTP ou Open EntityManager in View. */
    @Query("""
            select t from TarefasModel t
            join fetch t.user u
            where t.prazo = :data and t.status in :status
              and u.enabled = true
              and not exists (
                  select e.id from EmailLogModel e
                  where e.usuario_id = u.id and e.tipo = :tipoEmail
                    and e.referencia_id = t.id and e.data_referencia = :data
              )
            """)
    List<TarefasModel> buscarTarefasParaNotificar(@Param("data") LocalDate data,
                                                @Param("status") List<TarefaStatus> status,
                                                @Param("tipoEmail") TipoEmailLog tipoEmail);

    /** Tarefas do usuário (tb_tarefas.user_id). */
    long countByUserId(Long userId);

    /**
     * Consulta ADMIN das tarefas de UM usuário-alvo.
     *
     * O {@code userId} é SEMPRE obrigatório e aplicado junto com os filtros
     * ({@code AND}), então nenhum filtro opcional consegue ampliar o escopo
     * para tarefas de outros usuários. Cada filtro tem uma flag de presença
     * ({@code temStatus}/{@code temPrioridade}): usar {@code :x is null or ...}
     * quebra no Postgres, que não consegue inferir o tipo de um parâmetro nulo.
     */
    @Query("select t from TarefasModel t "
            + "where t.user.id = :userId "
            + "and (:temStatus = false or t.status = :status) "
            + "and (:temPrioridade = false or t.prioridade = :prioridade)")
    Page<TarefasModel> buscarDoUsuario(@Param("userId") Long userId,
                                       @Param("temStatus") boolean temStatus,
                                       @Param("status") TarefaStatus status,
                                       @Param("temPrioridade") boolean temPrioridade,
                                       @Param("prioridade") Prioridade prioridade,
                                       Pageable pageable);
}
