package dev.LzGuimaraes.FocusLifeHub.Despesa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.LzGuimaraes.FocusLifeHub.Email.TipoEmailLog;

@Repository
public interface DespesaRepository extends JpaRepository<DespesaModel, Long> {
    Page<DespesaModel> findByCarteiraDividas_UserId(Long userId, Pageable pageable);
    List<DespesaModel> findByCarteiraDividasId(Long carteiraId);
    List<DespesaModel> findByPagoFalseAndDataVencimento(LocalDate dataVencimento);
    List<DespesaModel> findByCarteiraDividas_UserIdAndPagoFalseAndDataVencimento(Long userId, LocalDate dataVencimento);

    /** Carrega o destinatário antes de encerrar a sessão do job e exclui avisos já enviados. */
    @Query("""
            select d from DespesaModel d
            join fetch d.carteiraDividas c
            join fetch c.user u
            where d.dataVencimento <= :data
              and d.tipo = :tipoDespesa
              and (d.pago = false or d.pago is null)
              and u.enabled = true
              and not exists (
                  select e.id from EmailLogModel e
                  where e.usuario_id = u.id and e.tipo = :tipoEmail
                    and e.referencia_id = d.id and e.data_referencia = :data
              )
            """)
    List<DespesaModel> buscarContasParaNotificar(@Param("data") LocalDate data,
                                               @Param("tipoDespesa") TipoLancamento tipoDespesa,
                                               @Param("tipoEmail") TipoEmailLog tipoEmail);

    /**
     * Consulta ADMIN dos lançamentos (contas/saldos) de UM usuário-alvo.
     *
     * O {@code userId} é SEMPRE obrigatório e aplicado junto com os filtros
     * ({@code AND}), então nenhum filtro opcional consegue ampliar o escopo
     * para lançamentos de outros usuários. A carteira dona vem na MESMA query
     * (join fetch) para o admin ver nome/moeda sem N+1. Cada filtro tem uma
     * flag de presença ({@code temTipo}/{@code temPago}): usar
     * {@code :x is null} quebra no Postgres, que não consegue inferir o tipo
     * de um parâmetro nulo. Pelo mesmo motivo, o filtro de pagamento usa
     * {@code Boolean} (o {@code pago} de um SALDO é nulo): com
     * {@code temPago=false} a primeira condição é sempre verdadeira e o valor
     * nulo do parâmetro não filtra nada.
     */
    @Query(value = "select d from DespesaModel d "
            + "join fetch d.carteiraDividas "
            + "where d.carteiraDividas.user.id = :userId "
            + "and (:temTipo = false or d.tipo = :tipo) "
            + "and (:temPago = false or d.pago = :pago)",
            countQuery = "select count(d) from DespesaModel d "
            + "where d.carteiraDividas.user.id = :userId "
            + "and (:temTipo = false or d.tipo = :tipo) "
            + "and (:temPago = false or d.pago = :pago)")
    Page<DespesaModel> buscarDoUsuario(@Param("userId") Long userId,
                                       @Param("temTipo") boolean temTipo,
                                       @Param("tipo") TipoLancamento tipo,
                                       @Param("temPago") boolean temPago,
                                       @Param("pago") Boolean pago,
                                       Pageable pageable);

    /** Despesas sem carteira (dados legados): invisíveis em todas as telas. */
    List<DespesaModel> findByCarteiraDividasIsNull();

    long countByCarteiraDividasIsNull();

    /**
     * Lançamentos do usuário. `despesa` NÃO tem coluna user_id: a posse vem da
     * carteira ({@code despesa.carteira_dividas_id -> carteira_dividas.user_id}).
     */
    long countByCarteiraDividas_UserId(Long userId);
}
