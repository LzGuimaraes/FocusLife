package dev.LzGuimaraes.FocusLifeHub.Email;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaModel;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasModel;

/**
 * Modelos HTML dos avisos agendados. O lembrete de contas usa a paleta do
 * sistema em tabelas com estilos inline; o lembrete de tarefas usa a lista simples.
 */
public final class EmailTemplate {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String FRONTEND_PADRAO = "https://focus.lzguimaraes.com.br";

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

    public static String contasVencendo(LocalDate data, List<DespesaModel> contas) {
        return contasVencendo(data, contas, FRONTEND_PADRAO);
    }

    public static String assuntoContas(LocalDate data, List<DespesaModel> contas) {
        return "FocusLife Hub — " + tituloContas(data, contas) + " (" + data + ")";
    }

    /** HTML de e-mail com tabelas e estilos inline, sem depender do CSS do navegador. */
    public static String contasVencendo(LocalDate data, List<DespesaModel> contas, String frontendUrl) {
        List<DespesaModel> ordenadas = contas.stream().sorted(Comparator
                .comparing(DespesaModel::getDataVencimento, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DespesaModel::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))).toList();
        long atrasadas = contas.stream().filter(c -> atrasada(c, data)).count();
        long hoje = contas.stream().filter(c -> data.equals(c.getDataVencimento())).count();
        String titulo = tituloContas(data, contas);
        Map<String, BigDecimal> totais = new LinkedHashMap<>();
        ordenadas.forEach(c -> totais.merge(moedaDe(c), valorDe(c), BigDecimal::add));
        StringBuilder totalHtml = new StringBuilder();
        totais.forEach((moeda, valor) -> totalHtml.append("<div style=\"font-size:30px;line-height:1.3;font-weight:700;color:#4f46e5;\">")
                .append(escape(formatarMoeda(valor, moeda))).append("</div>"));
        if (totais.isEmpty()) totalHtml.append("<div style=\"font-size:30px;color:#4f46e5;\">R$ 0,00</div>");

        StringBuilder linhas = new StringBuilder();
        for (DespesaModel conta : ordenadas) {
            boolean vencida = atrasada(conta, data);
            String fundo = vencida ? "#fee2e2" : "#fef3c7";
            String status = vencida ? "Atrasada" : data.equals(conta.getDataVencimento()) ? "Vence hoje" : "Pendente";
            String vencimento = conta.getDataVencimento() == null ? "Vencimento não informado"
                    : "Vencimento: " + DATA.format(conta.getDataVencimento());
            if (vencida) {
                long dias = ChronoUnit.DAYS.between(conta.getDataVencimento(), data);
                vencimento += " · " + dias + (dias == 1 ? " dia de atraso" : " dias de atraso");
            }
            String carteira = conta.getCarteiraDividas() == null ? null : conta.getCarteiraDividas().getNome();
            linhas.append("""
                    <tr><td style="padding:16px 0;border-bottom:1px solid #e2e8f0;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"><tr>
                    <td valign="top" style="padding-right:12px;word-break:break-word;">
                    <div style="font-size:16px;line-height:1.5;font-weight:700;color:#0f172a;">%s</div>
                    <div style="margin-top:5px;font-size:12px;line-height:1.6;color:%s;">%s</div>
                    %s</td>
                    <td align="right" valign="top" style="white-space:nowrap;">
                    <div style="font-size:16px;line-height:1.5;font-weight:700;color:#0f172a;">%s</div>
                    <span style="display:inline-block;margin-top:7px;padding:4px 8px;border-radius:6px;font-size:11px;font-weight:700;background:%s;color:%s;">%s</span>
                    </td></tr></table></td></tr>
                    """.formatted(escape(conta.getNome() == null ? "Conta sem nome" : conta.getNome()),
                    vencida ? "#ef4444" : "#64748b", escape(vencimento),
                    carteira == null || carteira.isBlank() ? "" : "<div style=\"font-size:12px;line-height:1.6;color:#64748b;\">" + escape(carteira) + "</div>",
                    escape(formatarMoeda(valorDe(conta), moedaDe(conta))), fundo, vencida ? "#b91c1c" : "#92400e", status));
        }
        String descricao = atrasadas > 0
                ? "Há contas com o vencimento ultrapassado. Confira os valores e organize os pagamentos abaixo."
                : "Confira as contas que precisam da sua atenção e organize os pagamentos de hoje.";
        String resumo = atrasadas + (atrasadas == 1 ? " atrasada" : " atrasadas") + " · " + hoje + " vencendo hoje";
        String link = linkContas(frontendUrl);

        return """
                <!DOCTYPE html>
                <html lang="pt-BR"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>%s — FocusLife Hub</title></head>
                <body style="margin:0;padding:0;background:#f1f5f9;color:#0f172a;font-family:Arial,Helvetica,sans-serif;-webkit-text-size-adjust:100%%;">
                <div style="display:none;font-size:1px;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;">%s. Abra suas contas no FocusLife Hub.</div>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" bgcolor="#f1f5f9"><tr><td align="center" style="padding:28px 12px;">
                <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="width:100%%;max-width:600px;background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;overflow:hidden;">
                <tr><td bgcolor="#6366f1" style="padding:26px 24px;border-radius:16px 16px 0 0;">
                <div style="font-size:22px;line-height:1.3;font-weight:700;letter-spacing:-0.5px;color:#ffffff;">FocusLife Hub</div>
                <div style="margin-top:6px;font-size:12px;line-height:1.5;color:#eef2ff;">Suas finanças, com mais clareza.</div>
                </td></tr>
                <tr><td style="padding:28px 24px 12px;">
                <div style="font-size:11px;line-height:1.5;font-weight:700;letter-spacing:1px;color:#6366f1;">LEMBRETE DE PAGAMENTO · %s</div>
                <h1 style="margin:10px 0 12px;font-size:26px;line-height:1.3;letter-spacing:-0.5px;color:#0f172a;">%s</h1>
                <p style="margin:0;font-size:14px;line-height:1.7;color:#64748b;">%s</p>
                </td></tr>
                <tr><td style="padding:12px 24px 20px;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" bgcolor="#eef2ff" style="border:1px solid #e0e7ff;border-radius:12px;">
                <tr><td style="padding:20px;">
                <div style="font-size:12px;line-height:1.5;color:#64748b;">Total pendente neste lembrete</div>
                %s
                <div style="margin-top:8px;font-size:12px;line-height:1.5;color:#475569;">%s</div>
                </td></tr></table></td></tr>
                <tr><td style="padding:0 24px;">
                <div style="font-size:13px;font-weight:700;line-height:1.6;color:#0f172a;">Contas para conferir</div>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">%s</table>
                </td></tr>
                <tr><td style="padding:24px;">
                <table role="presentation" cellspacing="0" cellpadding="0"><tr><td bgcolor="#6366f1" style="border-radius:10px;">
                <a href="%s" style="display:inline-block;padding:14px 24px;border:1px solid #6366f1;border-radius:10px;font-size:14px;font-weight:700;color:#ffffff;text-decoration:none;">Ver minhas contas</a>
                </td></tr></table>
                <p style="margin:16px 0 0;font-size:12px;line-height:1.7;color:#64748b;">Já pagou alguma dessas contas? Marque como paga no sistema para encerrar os lembretes.</p>
                <p style="margin:12px 0 0;font-size:11px;line-height:1.7;color:#94a3b8;">Se o botão não abrir, acesse <a href="%s" style="color:#6366f1;text-decoration:underline;">suas contas por este link</a>.</p>
                </td></tr>
                <tr><td style="padding:20px 24px;border-top:1px solid #e2e8f0;background:#f8fafc;border-radius:0 0 16px 16px;">
                <p style="margin:0;font-size:11px;line-height:1.7;color:#64748b;">FocusLife Hub · Lembrete automático de contas pendentes.<br>Enquanto uma conta vencida estiver pendente, ela poderá aparecer no lembrete do dia seguinte.</p>
                </td></tr></table></td></tr></table></body></html>
                """.formatted(escape(titulo), escape(resumo), DATA.format(data), escape(titulo), descricao,
                totalHtml, escape(resumo), linhas, escape(link), escape(link));
    }

    private static boolean atrasada(DespesaModel conta, LocalDate data) {
        return conta.getDataVencimento() != null && conta.getDataVencimento().isBefore(data);
    }

    private static String tituloContas(LocalDate data, List<DespesaModel> contas) {
        boolean atrasadas = contas.stream().anyMatch(c -> atrasada(c, data));
        boolean hoje = contas.stream().anyMatch(c -> data.equals(c.getDataVencimento()));
        return atrasadas ? hoje ? "Contas pendentes" : "Contas atrasadas" : hoje ? "Contas vencendo hoje" : "Contas pendentes";
    }

    private static BigDecimal valorDe(DespesaModel conta) {
        return conta.getSaldo() == null ? BigDecimal.ZERO : new BigDecimal(conta.getSaldo().toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private static String moedaDe(DespesaModel conta) {
        String moeda = conta.getCarteiraDividas() == null ? null : conta.getCarteiraDividas().getMoeda();
        return moeda == null || moeda.isBlank() ? "BRL" : moeda.trim().toUpperCase(Locale.ROOT);
    }

    private static String formatarMoeda(BigDecimal valor, String moeda) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(PT_BR);
        try {
            formato.setCurrency(Currency.getInstance(moeda));
        } catch (IllegalArgumentException ex) {
            return moeda + " " + NumberFormat.getNumberInstance(PT_BR).format(valor);
        }
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor).replace('\u00a0', ' ');
    }

    private static String linkContas(String frontendUrl) {
        String base = frontendUrl == null ? "" : frontendUrl.trim();
        if (!base.startsWith("https://") && !base.startsWith("http://")) base = FRONTEND_PADRAO;
        return base.replaceAll("/+$", "") + "/contas";
    }

    private static String escape(String valor) {
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String tarefasDoDia(LocalDate data, List<TarefasModel> tarefas) {
        List<String> itens = tarefas.stream()
                .map(t -> "<b>" + t.getTitulo() + "</b>"
                        + (t.getHorario() != null ? " — 🕐 " + t.getHorario() : ""))
                .collect(Collectors.toList());
        return listHtml("Tarefas do dia (" + data + ")", "Você tem as seguintes tarefas hoje:", itens);
    }
}
