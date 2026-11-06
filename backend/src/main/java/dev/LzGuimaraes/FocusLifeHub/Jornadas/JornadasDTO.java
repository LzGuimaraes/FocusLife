package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
public final class JornadasDTO {
    private JornadasDTO() {}
    public record Pedido(Long version,@NotBlank @Size(max=255) String titulo,@Size(max=10000) String descricao,
        LocalDate inicio,LocalDate prazo,@Size(max=80) String unidade,boolean fracionavel,
        @DecimalMin(value="0",inclusive=false) @Digits(integer=15,fraction=4) BigDecimal total,
        @NotNull @DecimalMin("0") @Digits(integer=15,fraction=4) BigDecimal saldoInicial,LocalDate saldoData,
        @NotNull StatusAtividade status,String prioridade,LocalTime horario,Set<DayOfWeek> diasSemana,@Size(max=255) String agrupamento) {}
    public record Lancamento(@NotNull Long version,@NotNull UUID requestId,@NotNull LocalDate data,
        @NotNull Progresso.Modo modo,@NotNull @DecimalMin("0") @Digits(integer=15,fraction=4) BigDecimal quantidade,
        @Size(max=4000) String observacao,@Min(1) @Max(5) Integer motivacao) {}
    public record LinkPedido(Long version,@NotBlank @Size(max=255) String titulo,@NotBlank @Size(max=2048) String url,
        @Size(max=4000) String descricao,Long atividadeId) {}
    public record Indicadores(double realizado,Double percentual,Double restante,Double esperado,Double diferenca,
        Double ritmoObservado,Double projetadoNoPrazo,LocalDate conclusaoEstimada,LocalDate concluidaEm,
        Double necessarioDia,Double necessarioSemana,Double praticoDia,Double praticoSemana,
        long diasObservados,long diasDisponiveis,String baseDias,String posicao,String tendencia,String situacaoPrazo,
        String mensagem,String recomendacao) {}
    public record Ponto(LocalDate data,double realizado,Double planejado) {}
    public record Sessao(Long id,OffsetDateTime inicio,OffsetDateTime fim,long duracaoSegundos) {}
    public record Anotacao(Long id,String nome,LocalDate data,int duracaoMin,String notas) {}
    public record EstudoPedido(@NotBlank @Size(max=255) String nome,@NotNull LocalDate data,@Min(0) int duracaoMin,@Size(max=10000) String notas) {}
    public record Resposta(Long id,long version,String titulo,String descricao,LocalDate inicio,LocalDate prazo,String unidade,
        boolean fracionavel,BigDecimal total,BigDecimal saldoInicial,LocalDate saldoData,StatusAtividade status,String prioridade,
        LocalTime horario,Set<DayOfWeek> diasSemana,String agrupamento,String origem,Double percentualLegado,LocalDate referencia,
        Indicadores indicadores,List<Progresso> historico,List<Ponto> grafico,List<Material> materiais,
        long totalSegundos,List<Sessao> sessoes,List<Anotacao> estudos) {}
}
