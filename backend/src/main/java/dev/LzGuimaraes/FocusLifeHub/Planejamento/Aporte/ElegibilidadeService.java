package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * ELEGIBILIDADE — "esse ativo PODE receber dinheiro agora?"
 *
 * Esta fase roda ANTES de qualquer ranking e é o único lugar que decide
 * descarte. Ela existe porque déficit alto, Quality Score alto ou preço baixo,
 * sozinhos, NÃO autorizam uma compra:
 *
 *   • déficit          → quanto falta para a carteira desejada (necessidade);
 *   • elegibilidade    → se o ativo pode receber (permissão);
 *   • priority score   → entre os permitidos, qual vem primeiro (ordem);
 *   • alocação         → quanto cabe em cada um sem violar tetos (valor).
 *
 * Nenhum score reabilita um ativo descartado aqui: quem violou uma regra de
 * compra (preço máximo, critério eliminatório, limite de concentração) fica de
 * fora, e o MOTIVO volta na resposta para a tela poder explicar.
 */
@Service
public class ElegibilidadeService {

    /**
     * Motivos possíveis de descarte, na ordem em que a tela costuma explicar.
     * São derivados das travas configuradas — nenhuma regra nova é inventada
     * aqui.
     */
    public enum Motivo {
        BLOQUEIO("BLOQUEIO", StatusElegibilidade.CRITERIO_ELIMINATORIO),
        LIMITE("LIMITE", StatusElegibilidade.LIMITE_ATINGIDO),
        PRECO("PRECO", StatusElegibilidade.PRECO_ACIMA_DO_LIMITE),
        NIVEL("CLASSE", StatusElegibilidade.CLASSE_SEM_CAPACIDADE),
        CAPACIDADE("TETO_ATIVO", StatusElegibilidade.SEM_CAPACIDADE),
        MOMENTO("MOMENTO", StatusElegibilidade.MOMENTO_ZERO);

        private final String trava;
        private final StatusElegibilidade status;

        Motivo(String trava, StatusElegibilidade status) {
            this.trava = trava;
            this.status = status;
        }

        public String getTrava() {
            return trava;
        }

        public StatusElegibilidade getStatus() {
            return status;
        }

        static Motivo daTrava(String trava) {
            for (Motivo m : values()) {
                if (m.trava.equalsIgnoreCase(trava)) {
                    return m;
                }
            }
            return null;
        }
    }

    /**
     * Fatos observados do ativo. Quem monta é o orquestrador — esta fase só
     * julga, para poder ser testada isoladamente.
     */
    public record Entrada(
            /** Critérios eliminatórios reprovados no checklist do usuário. */
            List<String> bloqueios,
            /** Ativo sem checklist de qualidade E sem checklist de momento. */
            boolean semAvaliacao,
            /** Já atingiu o limite máximo de concentração da meta. */
            boolean limiteAtingido,
            /** Preço atual acima do preço máximo de compra (null = sem regra). */
            boolean precoAcimaDoLimite,
            /** Fator de momento 0 (nota de momento abaixo da primeira faixa). */
            boolean momentoZero,
            /** A classe/subclasse/setor do ativo não tem déficit. */
            boolean nivelSemCapacidade,
            /** Capacidade do ativo (déficit + tolerância, respeitando o limite). */
            BigDecimal capacidade
    ) {}

    /**
     * Veredito: elegível? qual o status (a PRIMEIRA trava violada na ordem
     * configurada)? quais motivos (todos)?
     *
     * A ordem configura a EXPLICAÇÃO, não a decisão: qualquer trava violada
     * descarta o ativo, independente da posição dela na lista.
     */
    public Veredito avaliar(Entrada entrada, List<String> precedencia) {
        List<Motivo> violados = violados(entrada);
        if (violados.isEmpty()) {
            // Sem trava violada o ativo é ELEGÍVEL. "Sem avaliação" não é
            // descarte: significa só que os termos sem dado saem do score.
            StatusElegibilidade status = entrada.semAvaliacao()
                    ? StatusElegibilidade.SEM_AVALIACAO
                    : StatusElegibilidade.ELEGIVEL;
            return new Veredito(true, status, List.of(), List.of(), capacidadeDe(entrada));
        }

        StatusElegibilidade status = primeiroNaOrdem(violados, precedencia);
        List<String> motivos = new ArrayList<>();
        List<String> explicacoes = new ArrayList<>();
        for (Motivo m : violados) {
            motivos.add(m.name());
            explicacoes.add(m.getStatus().getLabel() + ": " + m.getStatus().getDescricao());
        }
        return new Veredito(false, status, List.copyOf(motivos), List.copyOf(explicacoes),
                capacidadeDe(entrada));
    }

    /** Veredito de elegibilidade. */
    public record Veredito(
            boolean elegivel,
            StatusElegibilidade status,
            /** Códigos dos motivos (ex.: "PRECO_ACIMA_DO_LIMITE"). */
            List<String> motivos,
            /** Explicação pronta para a tela, uma linha por motivo. */
            List<String> explicacoes,
            BigDecimal capacidade
    ) {}

    private List<Motivo> violados(Entrada entrada) {
        List<Motivo> violados = new ArrayList<>();
        if (entrada.bloqueios() != null && !entrada.bloqueios().isEmpty()) {
            violados.add(Motivo.BLOQUEIO);
        }
        if (entrada.limiteAtingido()) {
            violados.add(Motivo.LIMITE);
        }
        if (entrada.precoAcimaDoLimite()) {
            violados.add(Motivo.PRECO);
        }
        if (entrada.momentoZero()) {
            violados.add(Motivo.MOMENTO);
        }
        if (entrada.nivelSemCapacidade()) {
            violados.add(Motivo.NIVEL);
        }
        if (capacidadeDe(entrada).signum() <= 0) {
            violados.add(Motivo.CAPACIDADE);
        }
        return violados;
    }

    /** Status = primeira trava violada NA ORDEM CONFIGURADA (a mesma da tela). */
    private StatusElegibilidade primeiroNaOrdem(List<Motivo> violados, List<String> precedencia) {
        List<String> ordem = (precedencia == null || precedencia.isEmpty())
                ? List.of("BLOQUEIO", "LIMITE", "PRECO", "CLASSE", "SUBCLASSE", "SETOR", "TETO_ATIVO", "MOMENTO")
                : precedencia;
        for (String trava : ordem) {
            Motivo motivo = Motivo.daTrava(trava);
            if (motivo != null && violados.contains(motivo)) {
                return motivo.getStatus();
            }
        }
        return violados.get(0).getStatus();
    }

    private BigDecimal capacidadeDe(Entrada entrada) {
        return (entrada.capacidade() != null) ? entrada.capacidade().max(BigDecimal.ZERO) : BigDecimal.ZERO;
    }
}
