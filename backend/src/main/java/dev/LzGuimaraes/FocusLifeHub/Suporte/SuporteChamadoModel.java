package dev.LzGuimaraes.FocusLifeHub.Suporte;

import java.time.Instant;
import java.util.UUID;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "suporte_chamado", uniqueConstraints = @UniqueConstraint(name = "uk_suporte_usuario_solicitacao", columnNames = {"user_id", "solicitacao_id"}))
@Getter @Setter
public class SuporteChamadoModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private UserModel user;
    @Column(name = "solicitacao_id", nullable = false) private UUID solicitacaoId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TipoChamado tipo;
    @Column(nullable = false, length = 120) private String titulo;
    @Column(nullable = false, length = 4000) private String descricao;
    @Column(name = "usuario_nome", nullable = false) private String usuarioNome;
    @Column(name = "usuario_email", nullable = false) private String usuarioEmail;
    @Column(name = "criado_em", nullable = false) private Instant criadoEm;
    @Column(name = "email_enviado", nullable = false) private boolean emailEnviado;
    @Column(name = "email_enviado_em") private Instant emailEnviadoEm;
    @Column(nullable = false) private int tentativas;
    @Column(name = "ultima_tentativa_em") private Instant ultimaTentativaEm;
}
