package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Regra de PREÇO TETO / PREÇO MÍNIMO por usuário e ativo.
 *
 * Serve para sinalizar visualmente quando o preço atual está acima do teto
 * (hora de vender/tomar cuidado) ou abaixo do mínimo (possível oportunidade).
 * A regra é por CATÁLOGO ({@code ativoCadastroId}) OU por POSIÇÃO
 * ({@code ativoId}) — nunca os dois. A unicidade por usuário+ativo é garantida
 * por dois índices únicos PARCIAIS no banco (o Postgres não deduplica NULL num
 * unique comum). Mesmo padrão informativo de {@code operacao_compra_venda}.
 */
@Entity
@Table(name = "ativo_preco_alvo")
@Getter
@Setter
public class AtivoPrecoAlvoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ativo_cadastro_id")
    private UUID ativoCadastroId;

    @Column(name = "ativo_id")
    private Long ativoId;

    /** Snapshot do ticker — sobrevive à exclusão do catálogo/posição. */
    private String ticker;

    @Column(name = "preco_teto", precision = 14, scale = 2)
    private BigDecimal precoTeto;

    @Column(name = "preco_minimo", precision = 14, scale = 2)
    private BigDecimal precoMinimo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
