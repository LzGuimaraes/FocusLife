package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.ChecklistAtivoService;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealService;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.MeusAtivosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.CarteiraLookup;

class AporteServiceRankingTest {
    private final CarteiraIdealService ideal = mock(CarteiraIdealService.class);
    private final ChecklistAtivoService checklists = mock(ChecklistAtivoService.class);
    private final AporteConfigService config = mock(AporteConfigService.class);
    private final CarteiraLookup lookup = mock(CarteiraLookup.class);
    private final AlocacaoService alocacao = new AlocacaoService();
    private final AporteService servico = new AporteService(ideal, checklists, alocacao, new UtilizacaoAporteService(alocacao),
            new ElegibilidadeService(), config, lookup);

    @Test
    void posicaoSemMetaHerdaDeficitERecebeAntesDeAcaoAcimaDaMetaComNota100() {
        UUID catalogo = UUID.randomUUID();
        preparar();
        when(checklists.resumoPorAtivo()).thenReturn(List.of(new ChecklistAtivoDTO.AtivoAvaliado(
                catalogo, null, "ACAO", 1, 5, 5, new BigDecimal("100"), null, List.of())));
        when(ideal.comparativo(1L)).thenReturn(AporteTestes.comparativo(10000, List.of(
                AporteTestes.classe(CategoriaInvestimento.ACOES, 10000, 9000, 8000),
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 10000, 1000, 2000))));
        when(ideal.meusAtivos(1L)).thenReturn(new MeusAtivosResponseDTO(1L, "BRL", new BigDecimal("10000"), 1,
                List.of(posicao(catalogo, "ACAO", CategoriaInvestimento.ACOES, "9000", "80"),
                        posicao(null, "PICPAY", CategoriaInvestimento.RENDA_FIXA, "1000", null))));

        var resultado = servico.ranking(1L, new BigDecimal("1000"));
        var rf = resultado.itens().stream().filter(i -> "PICPAY".equals(i.ticker())).findFirst().orElseThrow();
        var acao = resultado.itens().stream().filter(i -> "ACAO".equals(i.ticker())).findFirst().orElseThrow();
        assertThat(rf.deficit()).isEqualByComparingTo("1200");
        assertThat(rf.sugestao_aporte()).isEqualByComparingTo("1000");
        assertThat(rf.posicao()).isEqualTo(1);
        assertThat(acao.nota()).isEqualByComparingTo("100");
        assertThat(acao.sugestao_aporte()).isZero();
        assertThat(resultado.valor_nao_alocado()).isZero();
    }

    @Test
    void rankingAplicaCentavosMesmoEmPatrimonioDeUmMilhao() {
        preparar();
        when(ideal.comparativo(1L)).thenReturn(AporteTestes.comparativo(1000000, List.of(
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 1000000, 1000000, 1000000))));
        when(ideal.meusAtivos(1L)).thenReturn(new MeusAtivosResponseDTO(1L, "BRL", new BigDecimal("1000000"), 1,
                List.of(posicao(null, "RF", CategoriaInvestimento.RENDA_FIXA, "1000000", null))));
        var resultado = servico.ranking(1L, new BigDecimal("0.51"));
        assertThat(resultado.valor_alocado()).isEqualByComparingTo("0.51");
        assertThat(resultado.valor_nao_alocado()).isZero();
    }

    @Test
    void percentualArredondadoNaoDescartaCapacidadeRealEmCentavos() {
        preparar();
        when(ideal.comparativo(1L)).thenReturn(AporteTestes.comparativo(1000000, List.of(
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 1000000, 1000000, 1000000))));
        var ativo = new MeusAtivosResponseDTO.MeuAtivoDTO(UUID.randomUUID(), true, "RF", "RF", List.of(1L),
                null, null, CategoriaInvestimento.RENDA_FIXA, BigDecimal.ONE, null, new BigDecimal("1000000"),
                new BigDecimal("100"), 1L, new BigDecimal("100"), BigDecimal.ZERO, new BigDecimal("100"), null, null);
        when(ideal.meusAtivos(1L)).thenReturn(new MeusAtivosResponseDTO(1L, "BRL", new BigDecimal("1000000"), 0, List.of(ativo)));
        var resultado = servico.ranking(1L, new BigDecimal("0.49"));
        assertThat(resultado.itens().get(0).percentual_atual()).isEqualByComparingTo("100");
        assertThat(resultado.itens().get(0).limite_atingido()).isFalse();
        assertThat(resultado.valor_alocado()).isEqualByComparingTo("0.49");
    }

    @Test
    void rankingCompletaCasoEquivalenteSemAlterarConfiguracaoNemReabilitarBloqueado() {
        preparar();
        UUID btc = UUID.randomUUID();
        UUID vale = UUID.randomUUID();
        UUID bloqueado = UUID.randomUUID();
        when(checklists.resumoPorAtivo()).thenReturn(List.of(
                new ChecklistAtivoDTO.AtivoAvaliado(btc, null, "BTC", 1, 5, 5, new BigDecimal("100"), null, List.of()),
                new ChecklistAtivoDTO.AtivoAvaliado(vale, null, "VALE3", 1, 5, 5, new BigDecimal("100"), null, List.of()),
                new ChecklistAtivoDTO.AtivoAvaliado(bloqueado, null, "Bloqueado", 1, 5, 5, new BigDecimal("100"), null,
                        List.of("Critério eliminatório reprovado"))));
        when(ideal.comparativo(1L)).thenReturn(AporteTestes.comparativo(18300, List.of(
                AporteTestes.classe(CategoriaInvestimento.CRIPTOMOEDAS, 18300, 3263.12, 2957.28),
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 18300, 1101.22, 1830),
                AporteTestes.classe(CategoriaInvestimento.ACOES, 18300, 13935.66, 13512.72))));
        var acao = new MeusAtivosResponseDTO.MeuAtivoDTO(vale, true, "VALE3", "VALE3", List.of(2L),
                null, null, CategoriaInvestimento.ACOES, BigDecimal.ONE, new BigDecimal("67.16"),
                new BigDecimal("201.61"), BigDecimal.ZERO, 2L, new BigDecimal("1.02"), BigDecimal.ZERO, null, null, null);
        when(ideal.meusAtivos(1L)).thenReturn(new MeusAtivosResponseDTO(1L, "BRL", new BigDecimal("18300"), 1,
                List.of(posicao(btc, "BTC", CategoriaInvestimento.CRIPTOMOEDAS, "3263.12", "16.16"),
                        posicao(null, "PICPAY", CategoriaInvestimento.RENDA_FIXA, "1101.22", null), acao,
                        posicao(bloqueado, "Bloqueado", CategoriaInvestimento.ACOES, "13734.05", "72.82"))));

        var resultado = servico.ranking(1L, new BigDecimal("1500"));
        assertThat(resultado.valor_alocado()).isEqualByComparingTo("1425.49");
        assertThat(resultado.valor_nao_alocado()).isEqualByComparingTo("74.51");
        assertThat(resultado.valor_minimo_alocar()).isEqualByComparingTo("1425");
        assertThat(resultado.valor_sobra_maxima()).isEqualByComparingTo("75");
        assertThat(resultado.meta_utilizacao_atingida()).isTrue();
        assertThat(resultado.margem_configurada_percentual()).isEqualByComparingTo("5");
        assertThat(resultado.margem_percentual()).isEqualByComparingTo("11.78");
        assertThat(resultado.itens().stream().filter(i -> "Bloqueado".equals(i.ticker())).findFirst().orElseThrow().sugestao_aporte()).isZero();
        assertThat(resultado.itens().stream().filter(i -> "PICPAY".equals(i.ticker())).findFirst().orElseThrow().sugestao_aporte()).isEqualByComparingTo("1112.02");
        assertThat(resultado.itens().stream().filter(i -> "BTC".equals(i.ticker())).findFirst().orElseThrow().sugestao_aporte()).isEqualByComparingTo("313.47");
        verify(config, never()).salvar(any());
    }

    @Test
    void informaMetaNaoAtingidaQuandoLimiteMaximoCadastradoImpede95PorCento() {
        preparar();
        when(ideal.comparativo(1L)).thenReturn(AporteTestes.comparativo(8500, List.of(
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 8500, 0, 850))));
        var rf = new MeusAtivosResponseDTO.MeuAtivoDTO(UUID.randomUUID(), true, "RF", "RF", List.of(1L),
                null, null, CategoriaInvestimento.RENDA_FIXA, BigDecimal.ONE, null, BigDecimal.ZERO, BigDecimal.ZERO,
                1L, new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("10.5"), null, null);
        when(ideal.meusAtivos(1L)).thenReturn(new MeusAtivosResponseDTO(1L, "BRL", new BigDecimal("8500"), 0, List.of(rf)));
        var resultado = servico.ranking(1L, new BigDecimal("1500"));
        assertThat(resultado.valor_alocado()).isEqualByComparingTo("1050");
        assertThat(resultado.meta_utilizacao_atingida()).isFalse();
        assertThat(resultado.margem_percentual()).isEqualByComparingTo("5");
        assertThat(resultado.avisos()).anyMatch(a -> a.contains("Não foi possível aplicar o mínimo de R$ 1425,00"));
    }

    private void preparar() {
        CarteiraInvestimentoModel carteira = new CarteiraInvestimentoModel();
        carteira.setId(1L);
        carteira.setMoeda("BRL");
        when(lookup.exigirCarteiraDoUsuario(1L)).thenReturn(carteira);
        when(config.margemPercentual()).thenReturn(5d);
        when(checklists.resumoPorAtivo()).thenReturn(List.of());
    }

    private MeusAtivosResponseDTO.MeuAtivoDTO posicao(UUID catalogo, String nome, CategoriaInvestimento classe,
                                                    String valor, String meta) {
        return new MeusAtivosResponseDTO.MeuAtivoDTO(catalogo, catalogo != null, nome, nome, List.of(1L),
                null, null, classe, BigDecimal.ONE, null, new BigDecimal(valor), BigDecimal.ZERO,
                meta == null ? null : 1L, meta == null ? null : new BigDecimal(meta), BigDecimal.ZERO, null, null, null);
    }
}
