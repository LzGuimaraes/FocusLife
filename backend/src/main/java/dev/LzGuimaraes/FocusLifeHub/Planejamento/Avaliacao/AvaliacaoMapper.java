package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.PerguntaDTO;

/**
 * Mapeamento e SNAPSHOT das perguntas de checklist.
 *
 * Está em um só lugar porque MODELO e CHECKLIST DO ATIVO compartilham a mesma
 * forma de pergunta/regra: o que muda é o pai e o fato de a instância guardar
 * a resposta. Assim a cópia "modelo → checklist" (snapshot) também tem um
 * único ponto de verdade.
 */
@Component
public class AvaliacaoMapper {

    /* ── Escrita ── */

    /** Copia a definição vinda do request para a entidade. */
    public void copiarDefinicao(PerguntaBaseModel destino, PerguntaDTO.PerguntaRequest req, boolean pontua, int ordem) {
        destino.setTitulo(req.titulo().trim());
        destino.setDescricao(req.descricao());
        destino.setTipo(req.tipo());
        destino.setPeso((req.peso() != null) ? req.peso() : BigDecimal.ONE);
        destino.setNotaMaxima((req.nota_maxima() != null) ? req.nota_maxima() : BigDecimal.TEN);
        destino.setContaNoScore(pontua);
        destino.setBloqueadora(req.bloqueadora() != null && req.bloqueadora());
        destino.setNotaMinima(req.nota_minima());
        destino.setOrdem((req.ordem() != null) ? req.ordem() : ordem);
    }

    /** Copia a definição de uma entidade para outra (usado no snapshot). */
    public void copiarDefinicao(PerguntaBaseModel destino, PerguntaBaseModel origem) {
        destino.setTitulo(origem.getTitulo());
        destino.setDescricao(origem.getDescricao());
        destino.setTipo(origem.getTipo());
        destino.setPeso(origem.getPeso());
        destino.setNotaMaxima(origem.getNotaMaxima());
        destino.setContaNoScore(origem.getContaNoScore());
        destino.setBloqueadora(origem.getBloqueadora());
        destino.setNotaMinima(origem.getNotaMinima());
        destino.setOrdem(origem.getOrdem());
    }

    public <R extends RegraBaseModel> R novaRegra(Supplier<R> factory, PerguntaDTO.RegraRequest req, int ordem) {
        R regra = factory.get();
        regra.setTexto((req.texto() != null) ? req.texto().trim() : null);
        regra.setValorMin(req.valor_min());
        regra.setValorMax(req.valor_max());
        regra.setNota((req.nota() != null) ? req.nota() : BigDecimal.ZERO);
        regra.setOrdem((req.ordem() != null) ? req.ordem() : ordem);
        return regra;
    }

    /** Copia uma regra existente (snapshot modelo → checklist). */
    public void copiarRegra(RegraBaseModel destino, RegraBaseModel origem, int ordem) {
        destino.setTexto(origem.getTexto());
        destino.setValorMin(origem.getValorMin());
        destino.setValorMax(origem.getValorMax());
        destino.setNota(origem.getNota());
        destino.setOrdem((origem.getOrdem() != null) ? origem.getOrdem() : ordem);
    }

    /**
     * SNAPSHOT: cria a pergunta do checklist a partir da pergunta do modelo,
     * copiando definição e regras e deixando a resposta vazia.
     */
    public ChecklistAtivoPerguntaModel snapshot(ChecklistModeloPerguntaModel origem) {
        ChecklistAtivoPerguntaModel nova = new ChecklistAtivoPerguntaModel();
        copiarDefinicao(nova, origem);

        int ordem = 0;
        for (ChecklistModeloPerguntaRegraModel regra : origem.getRegras()) {
            ChecklistAtivoPerguntaRegraModel copia = new ChecklistAtivoPerguntaRegraModel();
            copiarRegra(copia, regra, ordem++);
            copia.setPergunta(nova);
            nova.getRegras().add(copia);
        }
        return nova;
    }

    /** Duplica a ESTRUTURA de uma pergunta de checklist (sem copiar a resposta). */
    public ChecklistAtivoPerguntaModel duplicarEstrutura(ChecklistAtivoPerguntaModel origem) {
        ChecklistAtivoPerguntaModel nova = new ChecklistAtivoPerguntaModel();
        copiarDefinicao(nova, origem);

        int ordem = 0;
        for (ChecklistAtivoPerguntaRegraModel regra : origem.getRegras()) {
            ChecklistAtivoPerguntaRegraModel copia = new ChecklistAtivoPerguntaRegraModel();
            copiarRegra(copia, regra, ordem++);
            copia.setPergunta(nova);
            nova.getRegras().add(copia);
        }
        return nova;
    }

    /* ── Leitura ── */

    public PerguntaDTO.RegraResponse toRegraResponse(RegraBaseModel r) {
        return new PerguntaDTO.RegraResponse(
                r.getId(), r.getTexto(), r.getValorMin(), r.getValorMax(), r.getNota(), r.getOrdem());
    }

    public List<PerguntaDTO.RegraResponse> toRegrasResponse(List<? extends RegraBaseModel> regras) {
        return regras.stream().map(this::toRegraResponse).toList();
    }

    public PerguntaDTO.PerguntaResponse toPerguntaResponse(PerguntaBaseModel p,
                                                           Boolean obrigatoria,
                                                           List<? extends RegraBaseModel> regras,
                                                           BigDecimal notaAtribuida,
                                                           BigDecimal valorNumerico,
                                                           String respostaTexto,
                                                           String observacao,
                                                           LocalDateTime respondidoEm) {
        return new PerguntaDTO.PerguntaResponse(
                p.getId(),
                p.getTitulo(),
                p.getDescricao(),
                p.getTipo(),
                p.getPeso(),
                p.getNotaMaxima(),
                p.getContaNoScore(),
                obrigatoria,
                p.getBloqueadora(),
                p.getNotaMinima(),
                p.getOrdem(),
                toRegrasResponse(regras),
                notaAtribuida,
                valorNumerico,
                respostaTexto,
                observacao,
                respondidoEm);
    }

    /** Ordena por `ordem` e, em empate, por id (mantém a ordem definida pelo usuário). */
    public <T extends PerguntaBaseModel> List<T> ordenar(List<T> perguntas) {
        List<T> copia = new ArrayList<>(perguntas);
        copia.sort(Comparator.comparing((T p) -> (p.getOrdem() != null) ? p.getOrdem() : 0)
                .thenComparing(T::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return copia;
    }
}
