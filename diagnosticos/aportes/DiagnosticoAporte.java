package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.util.List;
import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

public class DiagnosticoAporte {
    private static final AlocacaoService MOTOR = new AlocacaoService();

    public static void main(String[] args) {
        imprimir("Caso informado: BTC, PICPAY e VALE3", List.of(
            AporteTestes.candidato(CategoriaInvestimento.CRIPTOMOEDAS, "BTC", 100, 0, 96.54, 419558.09),
            AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "PICPAY", 100, 0, 977.78, null),
            AporteTestes.candidato(CategoriaInvestimento.ACOES, "VALE3", 100, 0, 10.45, 67.16)),
            List.of(AporteTestes.classe(CategoriaInvestimento.CRIPTOMOEDAS, 10000, 0, 96.54),
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 10000, 0, 977.78),
                AporteTestes.classe(CategoriaInvestimento.ACOES, 10000, 0, 10.45)), "1500");

        imprimirUtilizacao();

        var sub = comLimite(AporteTestes.subclasse(1, "Limitada", 100, 100, 1000), 1);
        var bloqueada = AporteTestes.classe(CategoriaInvestimento.ACOES, 10000, 100, 1000, List.of(sub));
        var livre = AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 10000, 0, 1000);
        var c = List.of(
            AporteTestes.candidatoNaSubclasse(CategoriaInvestimento.ACOES, "Limitado", 90, 100, 99100, 1.0, 1, "Limitada"),
            AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "Livre", 80, 0, 1000, null));
        imprimir("Capacidade bloqueada da subclasse prende dinheiro de outra classe", c, List.of(bloqueada, livre), "1000");

        imprimir("Subclasse bloqueada impede ativo sem subclasse na mesma classe", List.of(
            c.get(0), AporteTestes.candidato(CategoriaInvestimento.ACOES, "Sem subclasse", 80, 0, 1000, null)),
            List.of(bloqueada), "1000");

        imprimir("Troco ultrapassa limite explicito de classe", List.of(
            AporteTestes.candidato(CategoriaInvestimento.ACOES, "Acao", 90, 0, 1000, 100.0),
            AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "RF", 80, 0, 1000, null)),
            List.of(AporteTestes.classeComLimite(CategoriaInvestimento.ACOES, 10000, 0, 1000, 1.5),
                AporteTestes.classeComLimite(CategoriaInvestimento.RENDA_FIXA, 10000, 0, 1000, 0.1)), "500");

        imprimir("Sobra por falta de capacidade nao aparece no troco", List.of(
            AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "RF", 80, 0, 100, null)),
            List.of(livre), "1000");

        imprimir("Tolerancia descarta aporte pequeno em patrimonio grande", List.of(
            AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "RF", 80, 0, 1000, null)),
            List.of(AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 1000000, 0, 1000)), "50");
    }

    private static ComparativoResponseDTO.SubclasseComparativoDTO comLimite(
            ComparativoResponseDTO.SubclasseComparativoDTO s, double limite) {
        return new ComparativoResponseDTO.SubclasseComparativoDTO(s.id(), s.nome(), s.percentual_ideal(),
            s.percentual_atual(), s.valor_ideal(), s.valor_atual(), s.deficit(), s.excesso(),
            s.tolerancia(), BigDecimal.valueOf(limite));
    }

    private static void imprimirUtilizacao() {
        var referencia = AporteTestes.comparativo(19800, List.of(
            AporteTestes.classe(CategoriaInvestimento.CRIPTOMOEDAS, 19800, 3263.12, 3199.68),
            AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 19800, 1101.22, 1980),
            AporteTestes.classe(CategoriaInvestimento.ACOES, 19800, 201.61, 201.96)));
        var aporte = new BigDecimal("1500");
        var resultado = new UtilizacaoAporteService(MOTOR).calcular(referencia, aporte, 5d, margem -> List.of(
            AporteTestes.comMeta(CategoriaInvestimento.CRIPTOMOEDAS, "BTC", 100, 16.16, 3263.12, 19800, margem, null, 419558.09),
            AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "PICPAY", 100, 10, 1101.22, 19800, margem, null, null),
            AporteTestes.comMeta(CategoriaInvestimento.ACOES, "VALE3", 100, 1.02, 201.61, 19800, margem, null, 67.16)));
        System.out.println("Nova regra: ampliar a margem do plano para buscar no maximo 5% de sobra");
        System.out.printf("aporte=%s alocado=%s sobra=%s sobraMaxima=%s margemInicial=%s margemPlano=%s metaAtingida=%s%n%n",
            aporte, resultado.orcamento().alocado(), resultado.orcamento().troco(), MetaUtilizacaoAporte.sobraMaxima(aporte),
            resultado.margemConfigurada(), resultado.margemAplicada(), MetaUtilizacaoAporte.atingida(aporte, resultado.orcamento()));
    }

    private static void imprimir(String nome, List<AporteCandidato> candidatos,
            List<ComparativoResponseDTO.ClasseComparativoDTO> classes, String aporte) {
        double patrimonio = nome.startsWith("Tolerancia") ? 1000000 : 10000;
        var valor = new BigDecimal(aporte);
        var resultado = MOTOR.ratear(candidatos, AporteTestes.comparativo(patrimonio, classes), valor);
        System.out.println(nome);
        System.out.printf("aporte=%s alocado=%s naoAlocado=%s troco=%s classes=%s%n%n",
            valor, resultado.alocado(), valor.subtract(resultado.alocado()), resultado.troco(), resultado.porClasse());
    }
}
