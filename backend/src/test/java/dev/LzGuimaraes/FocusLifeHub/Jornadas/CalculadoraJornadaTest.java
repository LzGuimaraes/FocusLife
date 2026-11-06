package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class CalculadoraJornadaTest {
    final CalculadoraJornada calc=new CalculadoraJornada();
    final LocalDate hoje=LocalDate.of(2026,11,5);
    Atividade atividade() {
        var a=new Atividade();a.setInicio(LocalDate.of(2026,10,15));a.setPrazo(LocalDate.of(2026,11,15));
        a.setSaldoData(a.getInicio().minusDays(1));a.setTotal(new BigDecimal("20"));a.setUnidade("unidades");a.setStatus(StatusAtividade.EM_ANDAMENTO);return a;
    }
    Progresso progresso(long id,String data,String qtd,Progresso.Modo modo) { var p=new Progresso();p.setId(id);p.setData(LocalDate.parse(data));p.setQuantidade(new BigDecimal(qtd));p.setModo(modo);return p; }
    @Test void cenarioDeAceitacaoFotografiaAoFimDoDia() {
        var a=atividade();var h=List.of(progresso(1,"2026-10-18","2",Progresso.Modo.INCREMENTO),progresso(2,"2026-11-05","2",Progresso.Modo.INCREMENTO));
        var i=calc.calcular(a,h,hoje);
        assertThat(i.realizado()).isEqualTo(4);assertThat(i.percentual()).isEqualTo(20);assertThat(i.restante()).isEqualTo(16);
        assertThat(i.esperado()).isEqualTo(13.75);assertThat(i.diferenca()).isEqualTo(-9.75);
        assertThat(i.diasObservados()).isEqualTo(22);assertThat(i.diasDisponiveis()).isEqualTo(10);
        assertThat(i.ritmoObservado()).isCloseTo(4.0/22,within(1e-9));
        assertThat(i.projetadoNoPrazo()).isCloseTo(4+4.0/22*10,within(1e-9));
        assertThat(i.necessarioDia()).isEqualTo(1.6);assertThat(i.necessarioSemana()).isCloseTo(11.2,within(1e-9));
        assertThat(i.praticoDia()).isEqualTo(2);assertThat(i.praticoSemana()).isEqualTo(12);
        assertThat(i.conclusaoEstimada()).isEqualTo(LocalDate.of(2027,2,1));assertThat(i.posicao()).isEqualTo("ATRASADA");assertThat(i.tendencia()).isEqualTo("FORA_DO_PRAZO");
    }
    @Test void saldoInicialNaoInventaProducaoNemRitmo() {
        var a=atividade();a.setSaldoInicial(new BigDecimal("4"));a.setSaldoData(hoje);
        var i=calc.calcular(a,List.of(),hoje);assertThat(i.realizado()).isEqualTo(4);assertThat(i.ritmoObservado()).isNull();assertThat(i.conclusaoEstimada()).isNull();assertThat(i.recomendacao()).contains("Sem dados suficientes");
        var depois=calc.calcular(a,List.of(progresso(1,"2026-11-06","2",Progresso.Modo.INCREMENTO)),hoje.plusDays(2));
        assertThat(depois.ritmoObservado()).isEqualTo(1);assertThat(depois.realizado()).isEqualTo(6);
    }
    @Test void acumuladosRetroativosNaoSomamDuasVezes() {
        var a=atividade();a.setTotal(new BigDecimal("350"));a.setUnidade("páginas");
        var h=List.of(progresso(1,"2026-10-20","100",Progresso.Modo.ACUMULADO),progresso(2,"2026-11-01","120",Progresso.Modo.ACUMULADO),progresso(3,"2026-10-25","5",Progresso.Modo.INCREMENTO));
        assertThat(calc.saldo(a,h,hoje).quantidade()).isEqualByComparingTo("120");
        assertThat(calc.saldo(a,List.of(h.get(0),h.get(2)),hoje).quantidade()).isEqualByComparingTo("105");
        assertThatThrownBy(()->calc.saldo(a,List.of(h.get(0),progresso(4,"2026-10-25","30",Progresso.Modo.INCREMENTO),h.get(1)),hoje)).hasMessageContaining("reduzir");
    }
    @Test void diasPlanejadosContamOportunidadesEPrevisaoPulaFimDeSemana() {
        var a=atividade();a.setDiasSemana(Set.of(DayOfWeek.MONDAY,DayOfWeek.WEDNESDAY,DayOfWeek.FRIDAY));
        var i=calc.calcular(a,List.of(progresso(1,"2026-10-16","4",Progresso.Modo.INCREMENTO)),hoje);
        assertThat(i.diasObservados()).isEqualTo(9);assertThat(i.diasDisponiveis()).isEqualTo(4);assertThat(i.esperado()).isCloseTo(20.0*9/13,within(1e-9));
        assertThat(i.necessarioDia()).isEqualTo(4);assertThat(i.necessarioSemana()).isEqualTo(12);assertThat(i.baseDias()).isEqualTo("DIAS_PLANEJADOS");
        assertThat(a.getDiasSemana()).contains(i.conclusaoEstimada().getDayOfWeek());
    }
    @Test void nenhumDiaDisponivelNaoGeraDivisaoPorZero() {
        var a=atividade();a.setInicio(hoje);a.setPrazo(hoje);a.setDiasSemana(Set.of(DayOfWeek.MONDAY));
        var i=calc.calcular(a,List.of(),hoje);assertThat(i.esperado()).isNull();assertThat(i.necessarioDia()).isNull();assertThat(i.tendencia()).isEqualTo("SEM_DIAS_DISPONIVEIS");
    }
    @Test void inicioPrazoMesmoDiaHojeEVencidoSemHistorico() {
        var a=atividade();a.setInicio(hoje);a.setSaldoData(hoje.minusDays(1));a.setPrazo(hoje);
        var i=calc.calcular(a,List.of(),hoje);assertThat(i.esperado()).isEqualTo(20);assertThat(i.situacaoPrazo()).isEqualTo("HOJE");assertThat(i.necessarioDia()).isNull();assertThat(i.recomendacao()).contains("prazo é hoje");
        var vencida=calc.calcular(a,List.of(),hoje.plusDays(1));assertThat(vencida.situacaoPrazo()).isEqualTo("VENCIDA");assertThat(vencida.ritmoObservado()).isNull();assertThat(vencida.projetadoNoPrazo()).isNull();
    }
    @Test void antesDoInicioNaoTemProgressoEsperadoOuRitmo() {
        var a=atividade();var i=calc.calcular(a,List.of(),a.getInicio().minusDays(1));assertThat(i.esperado()).isZero();assertThat(i.ritmoObservado()).isNull();assertThat(i.tendencia()).isEqualTo("NAO_INICIADA");
    }
    @Test void conclusaoAntesEDepoisDoPrazoEMigradaSemDataInventada() {
        var a=atividade();a.setStatus(StatusAtividade.CONCLUIDA);
        assertThat(calc.calcular(a,List.of(progresso(1,"2026-11-01","20",Progresso.Modo.INCREMENTO)),hoje).recomendacao()).contains("dentro do prazo");
        assertThat(calc.calcular(a,List.of(progresso(1,"2026-11-20","20",Progresso.Modo.INCREMENTO)),LocalDate.of(2026,11,20)).recomendacao()).contains("depois do prazo");
        a.setSaldoInicial(new BigDecimal("20"));a.setOrigem("TAREFA");assertThat(calc.calcular(a,List.of(),hoje).concluidaEm()).isNull();
        a.setOrigem(null);assertThat(calc.calcular(a,List.of(),hoje).concluidaEm()).isNull();
        assertThat(calc.calcular(a,List.of(progresso(2,"2026-11-05","20",Progresso.Modo.ACUMULADO)),hoje).concluidaEm()).isNull();
    }
    @Test void pausadaECanceladaSuspendemPrevisoesSemEstenderPrazo() {
        var a=atividade();var h=List.of(progresso(1,"2026-11-01","4",Progresso.Modo.INCREMENTO));
        for(var status:List.of(StatusAtividade.PAUSADA,StatusAtividade.CANCELADA)) {
            a.setStatus(status);var i=calc.calcular(a,h,hoje);assertThat(i.conclusaoEstimada()).isNull();assertThat(i.necessarioDia()).isNull();assertThat(i.projetadoNoPrazo()).isNull();assertThat(i.tendencia()).isEqualTo("SUSPENSA");assertThat(i.ritmoObservado()).isNotNull();assertThat(a.getPrazo()).isEqualTo(LocalDate.of(2026,11,15));
        }
    }
    @Test void horasFracionariasESimplesUmaUnidade() {
        var a=atividade();a.setFracionavel(true);a.setTotal(new BigDecimal("40"));a.setUnidade("horas");
        var i=calc.calcular(a,List.of(progresso(1,"2026-11-01","1.25",Progresso.Modo.INCREMENTO)),hoje);assertThat(i.realizado()).isEqualTo(1.25);assertThat(i.praticoDia()).isEqualTo(i.necessarioDia());
        a.setTotal(BigDecimal.ONE);a.setFracionavel(false);a.setUnidade("unidades");i=calc.calcular(a,List.of(progresso(1,"2026-11-01","1",Progresso.Modo.ACUMULADO)),hoje);assertThat(i.percentual()).isEqualTo(100);assertThat(i.restante()).isZero();
    }
    @Test void posicaoAdiantadaPodeTerTendenciaForaDoPrazo() {
        var a=atividade();a.setSaldoInicial(new BigDecimal("14"));a.setSaldoData(a.getInicio());
        var i=calc.calcular(a,List.of(progresso(1,"2026-11-05","1",Progresso.Modo.INCREMENTO)),hoje);
        assertThat(i.posicao()).isEqualTo("ADIANTADA");assertThat(i.tendencia()).isEqualTo("FORA_DO_PRAZO");
    }
    @Test void semTotalNaoInventaQuantidadeRestanteMesmoNoPrazoHoje() {
        var a=atividade();a.setTotal(null);a.setPrazo(hoje);
        var i=calc.calcular(a,List.of(),hoje);assertThat(i.restante()).isNull();assertThat(i.percentual()).isNull();assertThat(i.recomendacao()).contains("Complete a quantidade");
    }

}
