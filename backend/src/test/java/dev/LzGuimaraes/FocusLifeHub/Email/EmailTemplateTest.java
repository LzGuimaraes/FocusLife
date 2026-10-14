package dev.LzGuimaraes.FocusLifeHub.Email;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaModel;

class EmailTemplateTest {
    private static final LocalDate DIA = LocalDate.of(2026, 10, 13);

    @Test
    void contasAtrasadasMostramDiasDeAtrasoDatasETotalCorretos() {
        var aluguel = conta("Aluguel", 890.90f, DIA.minusDays(6), "BRL");
        var energia = conta("Energia", 289.90f, DIA.minusDays(1), "BRL");
        var html = EmailTemplate.contasVencendo(DIA, List.of(energia, aluguel), "https://focus.example.test/");
        assertThat(html).contains("Contas atrasadas", "6 dias de atraso", "1 dia de atraso", "07/10/2026", "R$ 1.180,80",
                "https://focus.example.test/contas", "Ver minhas contas");
        assertThat(html.indexOf("Aluguel")).isLessThan(html.indexOf("Energia"));
        assertThat(EmailTemplate.assuntoContas(DIA, List.of(aluguel, energia))).contains("Contas atrasadas");
    }

    @Test
    void avisoMistoDistingueVencidasDasContasQueVencemHoje() {
        var contas = List.of(conta("Atrasada", 100f, DIA.minusDays(1), "BRL"), conta("Hoje", 50f, DIA, "BRL"));
        var html = EmailTemplate.contasVencendo(DIA, contas);
        assertThat(html).contains("Contas pendentes", "Atrasada", "Vence hoje", "1 atrasada · 1 vencendo hoje", "R$ 150,00");
        assertThat(EmailTemplate.assuntoContas(DIA, contas)).contains("Contas pendentes");
    }

    @Test
    void nomesECarteirasSaoEscapadosSemInterpretarHtml() {
        var conta = conta("Conta <script>alert(1)</script> & taxa", 10f, DIA, "BRL");
        conta.getCarteiraDividas().setNome("Pessoal <img src=x> & família");
        assertThat(EmailTemplate.contasVencendo(DIA, List.of(conta)))
                .contains("&lt;script&gt;", "&lt;img src=x&gt;", "&amp; família")
                .doesNotContain("<script>", "<img src=x>");
    }

    @Test
    void totaisDeMoedasDiferentesNaoSaoSomadosComoSeFossemReais() {
        var html = EmailTemplate.contasVencendo(DIA, List.of(
                conta("Brasil", 100f, DIA, "BRL"), conta("Exterior", 200f, DIA, "USD")));
        assertThat(html).contains("R$ 100,00", "US$ 200,00").doesNotContain("R$ 300,00");
    }

    @Test
    void templateToleraDadosOpcionaisAusentesEUsaLinkSeguro() {
        var conta = new DespesaModel();
        assertThat(EmailTemplate.contasVencendo(DIA, List.of(conta), "javascript:alert(1)"))
                .contains("Conta sem nome", "Vencimento não informado", "R$ 0,00", "https://focus.lzguimaraes.com.br/contas")
                .doesNotContain("javascript:");
    }

    private DespesaModel conta(String nome, float valor, LocalDate vencimento, String moeda) {
        var carteira = new CarteiraDividasModel();
        carteira.setNome("Contas pessoais");
        carteira.setMoeda(moeda);
        var conta = new DespesaModel();
        conta.setNome(nome);
        conta.setSaldo(valor);
        conta.setDataVencimento(vencimento);
        conta.setCarteiraDividas(carteira);
        return conta;
    }
}
