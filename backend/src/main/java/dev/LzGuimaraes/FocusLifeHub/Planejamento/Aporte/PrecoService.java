package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Calculo.PercentualCalculator;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.AporteRegistroModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.AporteRegistroRepository;

/**
 * Preço dos ativos para o motor de aporte (elegibilidade + prioridade).
 *
 * Três números MUITO diferentes, de propósito:
 *
 *   • PREÇO ATUAL        — o preço de mercado (catálogo quando vinculado,
 *                          senão o preço informado na posição).
 *   • PREÇO MÉDIO        — quanto o investidor pagou, em média, nas compras
 *                          registradas no histórico de aportes
 *                          (`aporte_registro`: valor / quantidade).
 *                          Fallback: o valor unitário informado na posição.
 *                          É INFORMAÇÃO — nunca, sozinho, regra de compra
 *                          (§7 do spec: preço atual < preço médio não significa
 *                          "comprar").
 *   • PREÇO MÁXIMO       — a REGRA DE COMPRA do investidor (vem da meta do
 *     DE COMPRA            ativo). Acima dele o ativo é INELEGÍVEL.
 *
 * Da relação entre atual e máximo sai a OPORTUNIDADE DE PREÇO (0..1), que é o
 * termo configurável do Priority Score.
 */
@Service
public class PrecoService {

    private final AporteRegistroRepository registroRepository;
    private final AtivoRepository ativoRepository;
    private final PercentualCalculator calculator;

    public PrecoService(AporteRegistroRepository registroRepository,
                        AtivoRepository ativoRepository,
                        PercentualCalculator calculator) {
        this.registroRepository = registroRepository;
        this.ativoRepository = ativoRepository;
        this.calculator = calculator;
    }

    /** Preços de um ativo (todos podem ser null: nem todo ativo tem cotação). */
    public record Precos(BigDecimal atual, BigDecimal medio, BigDecimal maximoCompra,
                         BigDecimal oportunidade) {}

    /** Preço médio indexado por ativo do catálogo (UUID) e por posição (id). */
    public record IndicePrecoMedio(Map<UUID, BigDecimal> porCatalogo, Map<Long, BigDecimal> porPosicao) {

        /** Preço médio do candidato: pelo ticker e, na falta, pelas posições dele. */
        public BigDecimal de(UUID catalogoId, List<Long> ativoIds) {
            if (catalogoId != null) {
                BigDecimal porTicker = porCatalogo.get(catalogoId);
                if (porTicker != null) {
                    return porTicker;
                }
            }
            for (Long posicao : ativoIds) {
                BigDecimal daPosicao = porPosicao.get(posicao);
                if (daPosicao != null) {
                    return daPosicao;
                }
            }
            return null;
        }
    }

    /**
     * Preço médio por ativo da carteira.
     *
     * PRIORIDADE: compras registradas no histórico (valor ÷ quantidade). Sem
     * nenhuma compra registrada, cai para o valor unitário da posição — que é o
     * que o usuário digitou ao cadastrar o investimento.
     */
    @Transactional(readOnly = true)
    public IndicePrecoMedio precoMedio(Long carteiraId) {
        Map<UUID, double[]> somaPorCatalogo = new HashMap<>();
        Map<Long, double[]> somaPorPosicao = new HashMap<>();

        // 1) Histórico de aportes executados (o usuário informa preço e quantidade).
        for (AporteRegistroModel registro : registroRepository
                .findByCarteiraInvestimentoIdOrderByDataDescIdDesc(carteiraId)) {
            BigDecimal valor = (registro.getValor() != null) ? registro.getValor() : BigDecimal.ZERO;
            BigDecimal quantidade = (registro.getQuantidade() != null) ? registro.getQuantidade() : BigDecimal.ZERO;
            if (quantidade.signum() <= 0 || valor.signum() <= 0) {
                continue;
            }
            if (registro.getAtivoCadastroId() != null) {
                acumular(somaPorCatalogo, registro.getAtivoCadastroId(), valor, quantidade);
            }
            if (registro.getAtivoId() != null) {
                acumular(somaPorPosicao, registro.getAtivoId(), valor, quantidade);
            }
        }

        Map<UUID, BigDecimal> porCatalogo = new HashMap<>();
        somaPorCatalogo.forEach((id, soma) -> porCatalogo.put(id, media(soma)));

        Map<Long, BigDecimal> porPosicao = new HashMap<>();
        somaPorPosicao.forEach((id, soma) -> porPosicao.put(id, media(soma)));

        // 2) Fallback: valor unitário das posições (quantidade × valor unitário).
        List<AtivoModel> posicoes = ativoRepository.findByCarteiraInvestimentoId(carteiraId);
        Map<UUID, double[]> unitarioPorCatalogo = new HashMap<>();
        for (AtivoModel posicao : posicoes) {
            BigDecimal unitario = (posicao.getValorUnitario() != null)
                    ? BigDecimal.valueOf(posicao.getValorUnitario())
                    : null;
            if (unitario == null || unitario.signum() <= 0) {
                continue;
            }
            double quantidade = (posicao.getQuantidade() != null) ? Math.abs(posicao.getQuantidade()) : 1d;
            if (posicao.getAtivoCadastro() != null) {
                // Média PONDERADA do valor unitário: preço médio = valor total ÷
                // quantidade. Acumular o unitário "cru" dividiria o preço pela
                // quantidade (100 cotas a R$ 28 viravam "preço médio" R$ 0,28).
                BigDecimal quantidadeBd = BigDecimal.valueOf(quantidade);
                acumular(unitarioPorCatalogo, posicao.getAtivoCadastro().getId(),
                        unitario.multiply(quantidadeBd), quantidadeBd);
            }
            porPosicao.putIfAbsent(posicao.getId(), unitario);
        }
        unitarioPorCatalogo.forEach((id, soma) -> porCatalogo.putIfAbsent(id, media(soma)));

        return new IndicePrecoMedio(porCatalogo, porPosicao);
    }

    /**
     * OPORTUNIDADE DE PREÇO (0 a 1):
     *
     *      oportunidade = (preço máximo − preço atual) / preço máximo
     *
     * 0 = no limite de compra · tende a 1 = preço muito abaixo do limite.
     * Devolve null quando o investidor não definiu preço máximo (o termo sai da
     * conta do ativo — não é penalidade) e 0 quando o preço já passou do limite
     * (nesse caso o ativo nem chega ao ranking: é descartado antes).
     */
    public static BigDecimal oportunidade(BigDecimal precoAtual, BigDecimal precoMaximoCompra) {
        if (precoAtual == null || precoMaximoCompra == null || precoMaximoCompra.signum() <= 0) {
            return null;
        }
        BigDecimal diferenca = precoMaximoCompra.subtract(precoAtual);
        if (diferenca.signum() <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal oportunidade = diferenca.divide(precoMaximoCompra, 6, RoundingMode.HALF_UP);
        if (oportunidade.compareTo(BigDecimal.ONE) > 0) {
            oportunidade = BigDecimal.ONE;
        }
        return oportunidade.setScale(4, RoundingMode.HALF_UP);
    }

    private void acumular(Map<?, double[]> mapa, Object chave, BigDecimal valor, BigDecimal quantidade) {
        @SuppressWarnings("unchecked")
        Map<Object, double[]> alvo = (Map<Object, double[]>) mapa;
        double[] soma = alvo.computeIfAbsent(chave, k -> new double[2]);
        soma[0] += valor.doubleValue();
        soma[1] += quantidade.doubleValue();
    }

    private BigDecimal media(double[] soma) {
        if (soma[1] <= 0d) {
            return null;
        }
        return BigDecimal.valueOf(soma[0] / soma[1]).setScale(2, RoundingMode.HALF_UP);
    }
}
