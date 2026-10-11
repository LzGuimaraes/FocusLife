package dev.LzGuimaraes.FocusLifeHub.Ativo;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AtivoRepository extends JpaRepository<AtivoModel, Long> {
    Page<AtivoModel> findByCarteiraInvestimento_UserId(Long userId, Pageable pageable);
    List<AtivoModel> findByCarteiraInvestimentoId(Long carteiraId);
    List<AtivoModel> findByAtivoCadastroId(UUID ativoCadastroId);

    /**
     * Consulta ADMIN das posições de investimento de UM usuário-alvo.
     *
     * O {@code userId} é SEMPRE obrigatório e aplicado junto com o filtro de
     * categoria ({@code AND}), então nenhum filtro opcional consegue ampliar o
     * escopo para posições de outros usuários. A carteira dona e o ativo do
     * catálogo vêm na MESMA query (join fetch): a tela do admin mostra o nome/
     * moeda da carteira e o preço "ao vivo" do catálogo — o que o usuário vê —
     * sem N+1. A flag {@code temCategoria} evita o teste {@code :categoria is
     * null}, que o Postgres não consegue tipar.
     */
    @Query(value = "select a from AtivoModel a "
            + "join fetch a.carteiraInvestimento "
            + "left join fetch a.ativoCadastro "
            + "where a.carteiraInvestimento.user.id = :userId "
            + "and (:temCategoria = false or a.categoriaInvestimento = :categoria)",
            countQuery = "select count(a) from AtivoModel a "
            + "where a.carteiraInvestimento.user.id = :userId "
            + "and (:temCategoria = false or a.categoriaInvestimento = :categoria)")
    Page<AtivoModel> buscarDoUsuario(@Param("userId") Long userId,
                                     @Param("temCategoria") boolean temCategoria,
                                     @Param("categoria") CategoriaInvestimento categoria,
                                     Pageable pageable);

    /** Posições do usuário, restritas a um conjunto de IDs (usado ao vincular ao catálogo). */
    List<AtivoModel> findByIdInAndCarteiraInvestimento_UserId(Collection<Long> ids, Long userId);

    /**
     * Posições SEM carteira (dados legados anteriores à divisão das carteiras).
     * Como todo acesso a `ativo` passa pela carteira, uma linha assim fica
     * invisível em todas as telas — é o que o diagnóstico financeiro detecta.
     */
    List<AtivoModel> findByCarteiraInvestimentoIsNull();

    long countByCarteiraInvestimentoIsNull();

    /**
     * Posições do usuário. `ativo` NÃO tem coluna user_id: a posse vem da carteira
     * ({@code ativo.carteira_investimento_id -> carteira_investimento.user_id}).
     */
    long countByCarteiraInvestimento_UserId(Long userId);
}
