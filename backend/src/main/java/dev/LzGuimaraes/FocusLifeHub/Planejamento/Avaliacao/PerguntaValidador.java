package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.PerguntaDTO;

/**
 * Validação das perguntas criadas pelo usuário (Módulo 3).
 *
 * Regras centrais:
 *   • peso > 0 e nota máxima > 0;
 *   • TEXTO/LISTA nunca entram no score (o sistema não inventa nota);
 *   • NUMERO/PERCENTUAL que pontuam PRECISAM de faixas cadastradas, e as
 *     faixas não podem se sobrepor;
 *   • MULTIPLA_ESCOLHA que pontua precisa de ao menos uma opção, com rótulos únicos;
 *   • nenhuma nota de regra pode passar da nota máxima da pergunta.
 */
@Component
public class PerguntaValidador {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /** Valida a lista de perguntas de um modelo ou de um checklist criado à mão. */
    public void validar(List<PerguntaDTO.PerguntaRequest> perguntas) {
        if (perguntas == null) {
            return;
        }
        for (PerguntaDTO.PerguntaRequest p : perguntas) {
            validarUma(p);
        }
    }

    public void validarUma(PerguntaDTO.PerguntaRequest p) {
        TipoPergunta tipo = p.tipo();
        BigDecimal notaMaxima = (p.nota_maxima() != null) ? p.nota_maxima() : BigDecimal.TEN;
        BigDecimal peso = (p.peso() != null) ? p.peso() : BigDecimal.ONE;

        if (peso.compareTo(ZERO) <= 0) {
            throw new BusinessRuleException("O peso da pergunta \"" + p.titulo() + "\" deve ser maior que zero.");
        }
        if (notaMaxima.compareTo(ZERO) <= 0) {
            throw new BusinessRuleException("A nota máxima da pergunta \"" + p.titulo() + "\" deve ser maior que zero.");
        }

        List<PerguntaDTO.RegraRequest> regras = (p.regras() == null) ? List.of() : p.regras();
        boolean pontua = pontua(tipo, p.conta_no_score());

        if (pontua && tipo.usaFaixas() && regras.isEmpty()) {
            throw new BusinessRuleException("A pergunta \"" + p.titulo()
                    + "\" pontua pelo valor informado, então precisa de ao menos uma faixa de pontuação.");
        }
        if (pontua && tipo.usaOpcoes() && regras.isEmpty()) {
            throw new BusinessRuleException("A pergunta \"" + p.titulo()
                    + "\" é de múltipla escolha e precisa de ao menos uma opção.");
        }

        for (PerguntaDTO.RegraRequest r : regras) {
            if (r.nota() == null) {
                throw new BusinessRuleException("Toda regra da pergunta \"" + p.titulo() + "\" precisa de uma nota.");
            }
            if (r.nota().compareTo(ZERO) < 0 || r.nota().compareTo(notaMaxima) > 0) {
                throw new BusinessRuleException("A nota das regras da pergunta \"" + p.titulo()
                        + "\" deve ficar entre 0 e " + notaMaxima.stripTrailingZeros().toPlainString() + ".");
            }
            if (r.valor_min() != null && r.valor_max() != null && r.valor_min().compareTo(r.valor_max()) > 0) {
                throw new BusinessRuleException("Na pergunta \"" + p.titulo()
                        + "\" o valor mínimo de uma faixa é maior que o valor máximo.");
            }
        }

        if (!tipo.usaFaixas()) {
            validarRotulosUnicos(p, regras);
        } else {
            validarFaixasSemSobreposicao(p, regras);
        }
    }

    /** Decide se a pergunta entra no cálculo, respeitando o padrão do tipo. */
    public boolean pontua(TipoPergunta tipo, Boolean contaNoScore) {
        if (tipo.somenteInformativo()) {
            return false;
        }
        return (contaNoScore == null) ? tipo.pontuaPorPadrao() : contaNoScore;
    }

    private void validarRotulosUnicos(PerguntaDTO.PerguntaRequest p, List<PerguntaDTO.RegraRequest> regras) {
        Set<String> vistos = new LinkedHashSet<>();
        for (PerguntaDTO.RegraRequest r : regras) {
            if (r.texto() == null || r.texto().isBlank()) {
                if (p.tipo().usaOpcoes()) {
                    throw new BusinessRuleException("Toda opção da pergunta \"" + p.titulo() + "\" precisa de um rótulo.");
                }
                continue;
            }
            if (!vistos.add(r.texto().trim().toLowerCase())) {
                throw new BusinessRuleException("A opção \"" + r.texto().trim()
                        + "\" está repetida na pergunta \"" + p.titulo() + "\".");
            }
        }
    }

    /** Faixas ordenadas não podem se sobrepor (o valor precisa casar com uma única faixa). */
    private void validarFaixasSemSobreposicao(PerguntaDTO.PerguntaRequest p, List<PerguntaDTO.RegraRequest> regras) {
        List<PerguntaDTO.RegraRequest> ordenadas = new ArrayList<>(regras);
        ordenadas.sort((a, b) -> {
            BigDecimal minA = (a.valor_min() != null) ? a.valor_min() : BigDecimal.valueOf(Double.MIN_VALUE);
            BigDecimal minB = (b.valor_min() != null) ? b.valor_min() : BigDecimal.valueOf(Double.MIN_VALUE);
            return minA.compareTo(minB);
        });

        BigDecimal maiorMaximoAnterior = null;
        for (PerguntaDTO.RegraRequest r : ordenadas) {
            if (maiorMaximoAnterior != null && r.valor_min() != null
                    && r.valor_min().compareTo(maiorMaximoAnterior) < 0) {
                throw new BusinessRuleException("As faixas da pergunta \"" + p.titulo() + "\" se sobrepõem.");
            }
            if (r.valor_max() != null
                    && (maiorMaximoAnterior == null || r.valor_max().compareTo(maiorMaximoAnterior) > 0)) {
                maiorMaximoAnterior = r.valor_max();
            }
        }
    }
}
