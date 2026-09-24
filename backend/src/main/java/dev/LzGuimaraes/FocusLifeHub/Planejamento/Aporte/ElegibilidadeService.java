package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * ELEGIBILIDADE — "esse ativo PODE receber dinheiro agora?"
.
 *
 * Roda ANTES de olhar a nota e é o único lugar que descarta. Nenhuma nota alta
 * reabilita um ativo descartado: quem não tem espaço no nível, estourou o
 * próprio teto ou reprovou um critério eliminatório do checklist fica de fora —
 * e o MOTIVO volta na resposta para a tela explicar.
 *
 * A ordem das travas é FIXA (não é mais configurável): critério eliminatório,
 * limite de concentração, capacidade do nível e capacidade do ativo. É sempre a
 * primeira violada que vira o status.
 */
@Service
public class ElegibilidadeService {

    /** Fatos observados do ativo. Quem monta é o orquestrador. */
    public record Entrada(
            /** Critérios eliminatórios reprovados no checklist do usuário. */
            List<String> bloqueios,
            /** Ativo sem nenhum checklist respondido. */
            boolean semAvaliacao,
            /** Já atingiu o limite máximo de concentração da meta. */
            boolean limiteAtingido,
            /** A classe/subclasse do ativo não tem déficit. */
            boolean nivelSemCapacidade,
            /** Capacidade do ativo (déficit + tolerância, respeitando o limite). */
            BigDecimal capacidade
    ) {}

    /** Veredito de elegibilidade. */
    public record Veredito(
            boolean elegivel,
            StatusElegibilidade status,
            /** Códigos/explicações do descarte (vazio quando elegível). */
            List<String> motivos
    ) {}

    public Veredito avaliar(Entrada entrada) {
        List<StatusElegibilidade> violados = new ArrayList<>();
        if (entrada.bloqueios() != null && !entrada.bloqueios().isEmpty()) {
            violados.add(StatusElegibilidade.CRITERIO_ELIMINATORIO);
        }
        if (entrada.limiteAtingido()) {
            violados.add(StatusElegibilidade.LIMITE_ATINGIDO);
        }
        if (entrada.nivelSemCapacidade()) {
            violados.add(StatusElegibilidade.CLASSE_SEM_CAPACIDADE);
        }
        if (capacidade(entrada).signum() <= 0) {
            violados.add(StatusElegibilidade.SEM_CAPACIDADE);
        }

        if (violados.isEmpty()) {
            // Sem trava violada o ativo é ELEGÍVEL. "Sem avaliação" não é
            // descarte: significa só que ele entra na fila sem nota.
            StatusElegibilidade status = entrada.semAvaliacao()
                    ? StatusElegibilidade.SEM_AVALIACAO
                    : StatusElegibilidade.ELEGIVEL;
            return new Veredito(true, status, List.of());
        }

        List<String> motivos = violados.stream()
                .map(v -> v.getLabel() + ": " + v.getDescricao())
                .toList();
        return new Veredito(false, violados.get(0), motivos);
    }

    private BigDecimal capacidade(Entrada entrada) {
        return (entrada.capacidade() != null) ? entrada.capacidade().max(BigDecimal.ZERO) : BigDecimal.ZERO;
    }
}
