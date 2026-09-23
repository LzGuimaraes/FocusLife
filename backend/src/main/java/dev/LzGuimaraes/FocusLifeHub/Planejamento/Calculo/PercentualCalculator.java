package dev.LzGuimaraes.FocusLifeHub.Planejamento.Calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;

/**
 * Cálculos de valor/percentual da carteira usados pela Carteira Ideal.
 *
 * REGRAS (espelham `InvestInfo.tsx` do frontend, que é a referência visual):
 *   • valor da posição = preço × quantidade (preço atual do catálogo quando o
 *     card está vinculado a um ativo_cadastro — o catálogo é a fonte da
 *     verdade do preço); sem quantidade, cai para o saldo da posição;
 *   • ativos sem `ativo_cadastro_id` (ex.: renda fixa) entram na conta da
 *     CLASSE, mas não têm agregação por ticker.
 *
 * Toda matemática interna usa double e só é arredondada na saída
 * (2 casas para dinheiro, 4 para percentual).
 */
@Component
public class PercentualCalculator {

    public static final int ESCALA_MOEDA = 2;
    public static final int ESCALA_PERCENTUAL = 4;

    private final AtivoRepository ativoRepository;

    public PercentualCalculator(AtivoRepository ativoRepository) {
        this.ativoRepository = ativoRepository;
    }

    /** Fotografia de uma posição da carteira, já com valor calculado. */
    public record PosicaoSnapshot(
            Long ativoId,
            UUID ativoCadastroId,
            String nome,
            CategoriaInvestimento classe,
            double valor
    ) {}

    /** Posições da carteira com o valor de mercado calculado. */
    public List<PosicaoSnapshot> posicoes(Long carteiraId) {
        return ativoRepository.findByCarteiraInvestimentoId(carteiraId).stream()
                .map(this::paraSnapshot)
                .collect(Collectors.toList());
    }

    private PosicaoSnapshot paraSnapshot(AtivoModel ativo) {
        AtivoCadastroModel catalogo = ativo.getAtivoCadastro();
        UUID cadastroId = (catalogo != null) ? catalogo.getId() : null;
        String nome = (catalogo != null && catalogo.getNome() != null) ? catalogo.getNome() : ativo.getNome();
        return new PosicaoSnapshot(ativo.getId(), cadastroId, nome,
                ativo.getCategoriaInvestimento(), valorPosicao(ativo));
    }

    /** Valor de mercado da posição (nunca negativo). */
    public double valorPosicao(AtivoModel ativo) {
        Float quantidade = ativo.getQuantidade();
        if (quantidade != null) {
            Float preco = precoAtual(ativo);
            if (preco == null) {
                preco = ativo.getValorUnitario();
            }
            if (preco != null) {
                return Math.abs(preco * quantidade);
            }
        }
        Float saldo = ativo.getSaldo();
        return (saldo != null) ? Math.abs(saldo) : 0d;
    }

    /** Preço atual: o do catálogo quando vinculado (fonte da verdade). */
    public Float precoAtual(AtivoModel ativo) {
        AtivoCadastroModel catalogo = ativo.getAtivoCadastro();
        if (catalogo != null && catalogo.getPrecoAtual() != null) {
            return catalogo.getPrecoAtual();
        }
        return ativo.getPrecoAtual();
    }

    /** Valor total da carteira (soma das posições). */
    public double valorTotal(List<PosicaoSnapshot> posicoes) {
        return posicoes.stream().mapToDouble(PosicaoSnapshot::valor).sum();
    }

    /** Valor atual agregado por classe (posições sem classe caem em OUTROS). */
    public Map<CategoriaInvestimento, Double> valorPorClasse(List<PosicaoSnapshot> posicoes) {
        Map<CategoriaInvestimento, Double> acumulado = new EnumMap<>(CategoriaInvestimento.class);
        for (PosicaoSnapshot p : posicoes) {
            CategoriaInvestimento classe = (p.classe() != null) ? p.classe() : CategoriaInvestimento.OUTROS;
            acumulado.merge(classe, p.valor(), Double::sum);
        }
        return acumulado;
    }

    /** Valor atual agregado por ativo do catálogo (somente posições vinculadas). */
    public Map<UUID, Double> valorPorAtivoCadastro(List<PosicaoSnapshot> posicoes) {
        return posicoes.stream()
                .filter(p -> p.ativoCadastroId() != null)
                .collect(Collectors.groupingBy(PosicaoSnapshot::ativoCadastroId,
                        Collectors.summingDouble(PosicaoSnapshot::valor)));
    }

    /** Nome (ticker) das posições vinculadas, por ID do catálogo — evita N+1 no comparativo. */
    public Map<UUID, String> nomePorAtivoCadastro(List<PosicaoSnapshot> posicoes) {
        return posicoes.stream()
                .filter(p -> p.ativoCadastroId() != null && p.nome() != null)
                .collect(Collectors.toMap(PosicaoSnapshot::ativoCadastroId, PosicaoSnapshot::nome,
                        (a, b) -> a));
    }

    /** Percentual de `valor` sobre `total` (0 quando não há total). */
    public BigDecimal percentual(double valor, double total) {
        if (total <= 0d) {
            return BigDecimal.ZERO.setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(valor / total * 100d).setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
    }

    /** Arredonda um valor monetário para 2 casas. */
    public BigDecimal moeda(double valor) {
        return BigDecimal.valueOf(valor).setScale(ESCALA_MOEDA, RoundingMode.HALF_UP);
    }

    /** Normaliza um percentual vindo do request para 4 casas. */
    public BigDecimal percentualNormalizado(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
        }
        return valor.setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
    }
}
