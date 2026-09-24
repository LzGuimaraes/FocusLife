package dev.LzGuimaraes.FocusLifeHub.SetorMercado;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * SETOR DE MERCADO — catálogo de referência (global, não é dado do usuário).
 *
 * Nasceu porque "setor" só existia DENTRO de uma carteira
 * (`carteira_ideal_setor`, com % alvo): o mesmo setor ganhava um id diferente
 * em cada carteira e o ticker (`ativo_cadastro`) não carregava setor nenhum.
 * Sem identidade estável não dá para somar exposição por setor, comparar
 * setores entre carteiras nem decidir aporte por setor.
 *
 * Aqui a identidade é do SETOR:
 *   • `slug` é a chave de reuso (nome normalizado, sem acento e em minúsculas),
 *     então "Bancos", "bancos" e "BANCOS" caem na MESMA linha;
 *   • `segmento` é o agrupamento acima do setor (ex.: "Financeiro"), opcional;
 *   • `ativo = false` ESCONDE o setor das listas sem apagar o histórico — é o
 *     caminho para quem classifica o catálogo manualmente no banco.
 *
 * Como `ativo_cadastro`, este catálogo é mantido manualmente (banco/script) e o
 * app só o complementa quando o usuário cria um setor novo na Carteira Ideal.
 */
@Entity
@Table(name = "setor_mercado")
@Getter
@Setter
public class SetorMercadoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Nome normalizado (minúsculo, sem acento, com hífen) — chave de reuso. */
    @Column(nullable = false, length = 120)
    private String slug;

    /** Agrupamento acima do setor (ex.: "Financeiro", "Bens Industriais"). */
    @Column(length = 120)
    private String segmento;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Nome normalizado de qualquer texto: "Águas e Saneamento" → "aguas-e-saneamento". */
    public static String slugDe(String texto) {
        if (texto == null) {
            return "";
        }
        return java.text.Normalizer.normalize(texto.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
