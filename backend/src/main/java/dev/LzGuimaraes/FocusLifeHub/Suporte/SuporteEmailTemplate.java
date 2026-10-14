package dev.LzGuimaraes.FocusLifeHub.Suporte;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.web.util.HtmlUtils;

final class SuporteEmailTemplate {
    private SuporteEmailTemplate() {}
    static String html(SuporteChamadoModel chamado) {
        String data = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Cuiaba")).format(chamado.getCriadoEm());
        return """
                <!doctype html><html lang="pt-BR"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:24px 12px;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;margin:auto;background:#ffffff;border:1px solid #e2e8f0;border-radius:14px;overflow:hidden;">
                <tr><td style="padding:24px;background:#6366f1;color:white;"><strong style="font-size:22px;">FocusLife · Suporte</strong><div style="margin-top:8px;">Novo chamado · %s</div></td></tr>
                <tr><td style="padding:24px;">
                <div style="color:#6366f1;font-weight:bold;font-size:13px;">%s</div>
                <h1 style="font-size:22px;line-height:1.4;overflow-wrap:anywhere;">%s</h1>
                <p style="font-size:13px;line-height:1.8;color:#64748b;"><strong>Solicitante:</strong> %s<br><strong>E-mail:</strong> %s<br><strong>Recebido em:</strong> %s (America/Cuiaba)</p>
                <div style="padding:16px;background:#eef2ff;border-radius:10px;font-size:14px;line-height:1.8;overflow-wrap:anywhere;">%s</div>
                <p style="margin-top:24px;font-size:12px;color:#64748b;">Use Responder para entrar em contato com o solicitante. Este e-mail foi gerado por um chamado aberto no FocusLife.</p>
                </td></tr></table></body></html>
                """.formatted(escape(SuporteDTO.protocolo(chamado.getId())), escape(chamado.getTipo().descricao()), escape(chamado.getTitulo()),
                escape(chamado.getUsuarioNome()), escape(chamado.getUsuarioEmail()), escape(data), escape(chamado.getDescricao()).replace("\n", "<br>"));
    }
    private static String escape(String text) { return HtmlUtils.htmlEscape(text == null ? "" : text); }
}
