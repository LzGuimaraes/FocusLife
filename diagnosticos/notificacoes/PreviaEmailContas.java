package dev.LzGuimaraes.FocusLifeHub.Email;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaModel;

/** Gera prévias locais com dados fictícios; não instancia o serviço de envio. */
public class PreviaEmailContas {
    public static void main(String[] args) throws Exception {
        Path pasta = Path.of(args.length == 0 ? "diagnosticos/notificacoes" : args[0]);
        Files.createDirectories(pasta);
        LocalDate data = LocalDate.of(2026, 10, 13);
        var aluguel = conta("Aluguel", 890.90f, data.minusDays(6));
        var energia = conta("Energia elétrica", 289.90f, data.minusDays(3));
        var internet = conta("Internet", 150f, data);
        Files.writeString(pasta.resolve("previa-contas-atrasadas.html"),
                EmailTemplate.contasVencendo(data, List.of(aluguel, energia)), StandardCharsets.UTF_8);
        Files.writeString(pasta.resolve("previa-contas-pendentes.html"),
                EmailTemplate.contasVencendo(data, List.of(aluguel, energia, internet)), StandardCharsets.UTF_8);
        System.out.println("Prévias geradas em " + pasta.toAbsolutePath());
    }

    private static DespesaModel conta(String nome, float valor, LocalDate vencimento) {
        var carteira = new CarteiraDividasModel();
        carteira.setNome("Contas pessoais");
        carteira.setMoeda("BRL");
        var conta = new DespesaModel();
        conta.setNome(nome);
        conta.setSaldo(valor);
        conta.setDataVencimento(vencimento);
        conta.setCarteiraDividas(carteira);
        return conta;
    }
}
