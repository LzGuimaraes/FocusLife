package dev.LzGuimaraes.FocusLifeHub.Email;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import dev.LzGuimaraes.FocusLifeHub.Contas.ContasModel;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasModel;

/**
 * Monta o corpo (HTML) dos e-mails do job diário sem duplicar a estrutura.
 * O método privado {@link #listHtml} é o template compartilhado por todos os
 * tipos de e-mail (contas vencendo, tarefas do dia, etc.).
 */
public final class EmailTemplate {

    private EmailTemplate() {
    }

    private static String listHtml(String titulo, String subtitulo, List<String> itens) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>FocusLife Hub — ").append(titulo).append("</h2>");
        if (subtitulo != null && !subtitulo.isBlank()) {
            html.append("<p>").append(subtitulo).append("</p>");
        }
        html.append("<ul>");
        for (String item : itens) {
            html.append("<li>").append(item).append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    public static String contasVencendo(LocalDate data, List<ContasModel> contas) {
        List<String> itens = contas.stream()
                .map(c -> "<b>" + c.getNome() + "</b> — R$ "
                        + String.format("%.2f", c.getSaldo() != null ? c.getSaldo() : 0f))
                .collect(Collectors.toList());
        return listHtml("Contas vencendo hoje (" + data + ")", "As contas abaixo vencem hoje:", itens);
    }

    public static String tarefasDoDia(LocalDate data, List<TarefasModel> tarefas) {
        List<String> itens = tarefas.stream()
                .map(t -> "<b>" + t.getTitulo() + "</b>"
                        + (t.getHorario() != null ? " — 🕐 " + t.getHorario() : ""))
                .collect(Collectors.toList());
        return listHtml("Tarefas do dia (" + data + ")", "Você tem as seguintes tarefas hoje:", itens);
    }
}
