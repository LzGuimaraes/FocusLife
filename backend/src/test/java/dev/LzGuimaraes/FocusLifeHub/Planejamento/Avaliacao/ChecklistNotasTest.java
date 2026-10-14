package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;

class ChecklistNotasTest {
    private final ChecklistAtivoRepository repository = mock(ChecklistAtivoRepository.class);
    private final ContextoUsuario contexto = mock(ContextoUsuario.class);
    private final ChecklistAtivoService servico = new ChecklistAtivoService(repository, null, null, null, null,
            contexto, null, new ScoreCalculator(), new AvaliacaoMapper());

    @Test
    void checklistDesativadoNaoMudaNotaNemBloqueiaAtivo() {
        var ativo = catalogo();
        var valido = checklist(ativo, true, "10", "1");
        var desativado = checklist(ativo, false, "0", "100");
        desativado.getPerguntas().get(0).setBloqueadora(true);
        desativado.getPerguntas().get(0).setTitulo("Critério antigo");
        preparar(List.of(valido, desativado));
        var resultado = servico.resumoPorAtivo().get(0);
        assertThat(resultado.quality_score()).isEqualByComparingTo("100");
        assertThat(resultado.bloqueios()).isEmpty();
        assertThat(resultado.total_checklists()).isEqualTo(1);
        assertThat(resultado.total_perguntas()).isEqualTo(1);
    }

    @Test
    void apenasChecklistsDesativadosNaoProduzemAvaliacao() {
        preparar(List.of(checklist(catalogo(), false, "10", "1")));
        assertThat(servico.resumoPorAtivo()).isEmpty();
    }

    @Test
    void notaNormalizaEscalasERespeitaPesosSemConfundirMomentoComQualidade() {
        var ativo = catalogo();
        var fundamentos = checklist(ativo, true, "8", "3");
        fundamentos.getPerguntas().get(0).setPeso(new BigDecimal("3"));
        var risco = pergunta("4");
        risco.setNotaMaxima(new BigDecimal("20"));
        fundamentos.getPerguntas().add(risco);
        var governanca = checklist(ativo, true, "10", "1");
        var momento = checklist(ativo, true, "0", "100");
        momento.setTipo(TipoChecklist.MOMENTO);
        preparar(List.of(fundamentos, governanca, momento));
        var resultado = servico.resumoPorAtivo().get(0);
        // Fundamentos: (3 × 80 + 1 × 20) / 4 = 65.
        // Qualidade: (3 × 65 + 1 × 100) / 4 = 73,75.
        assertThat(resultado.quality_score()).isEqualByComparingTo("73.75");
        assertThat(resultado.momento_score()).isZero();
    }

    @Test
    void perguntaSemRespostaNaoViraNotaZeroENotaParcialMantemContagem() {
        var checklist = checklist(catalogo(), true, "10", "1");
        checklist.getPerguntas().add(pergunta(null));
        preparar(List.of(checklist));
        var resultado = servico.resumoPorAtivo().get(0);
        assertThat(resultado.quality_score()).isEqualByComparingTo("100");
        assertThat(resultado.total_respondidas()).isEqualTo(1);
        assertThat(resultado.total_perguntas()).isEqualTo(2);
    }

    private void preparar(List<ChecklistAtivoModel> checklists) {
        when(contexto.id()).thenReturn(1L);
        when(repository.findAllComPerguntas(1L)).thenReturn(checklists);
    }

    private AtivoCadastroModel catalogo() {
        var catalogo = new AtivoCadastroModel();
        catalogo.setId(UUID.randomUUID());
        catalogo.setNome("ACAO");
        return catalogo;
    }

    private ChecklistAtivoModel checklist(AtivoCadastroModel ativo, boolean ativa, String nota, String peso) {
        var checklist = new ChecklistAtivoModel();
        checklist.setAtivoCadastro(ativo);
        checklist.setAtiva(ativa);
        checklist.setNome("Checklist");
        checklist.setTipo(TipoChecklist.QUALIDADE);
        checklist.setPeso(new BigDecimal(peso));
        checklist.getPerguntas().add(pergunta(nota));
        return checklist;
    }

    private ChecklistAtivoPerguntaModel pergunta(String nota) {
        var pergunta = new ChecklistAtivoPerguntaModel();
        pergunta.setPeso(BigDecimal.ONE);
        pergunta.setNotaMaxima(BigDecimal.TEN);
        pergunta.setContaNoScore(true);
        pergunta.setNotaAtribuida(nota == null ? null : new BigDecimal(nota));
        return pergunta;
    }
}
