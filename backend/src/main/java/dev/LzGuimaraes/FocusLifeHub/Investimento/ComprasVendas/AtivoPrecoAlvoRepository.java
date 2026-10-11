package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtivoPrecoAlvoRepository extends JpaRepository<AtivoPrecoAlvoModel, Long> {

    List<AtivoPrecoAlvoModel> findByUserIdOrderByIdDesc(Long userId);

    Optional<AtivoPrecoAlvoModel> findByIdAndUserId(Long id, Long userId);

    Optional<AtivoPrecoAlvoModel> findByUserIdAndAtivoCadastroId(Long userId, UUID ativoCadastroId);

    Optional<AtivoPrecoAlvoModel> findByUserIdAndAtivoId(Long userId, Long ativoId);

    /** Preços-alvo do usuário (ativo_preco_alvo.user_id, coluna escalar). */
    long countByUserId(Long userId);
}
