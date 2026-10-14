package dev.LzGuimaraes.FocusLifeHub.Musica;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.List;
import java.util.function.Function;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;

/** Migração V38, Hibernate, repositórios e serviço reais em uma base H2 isolada. */
class MusicaCatalogoTest {
    private static final String URL = "jdbc:h2:mem:musica-catalogo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    private static final String A = "https://youtube.com/watch?v=X4VbdwhkE10";
    private static final String B = "https://youtu.be/abcdefghijk";
    private static final String C = "https://youtu.be/0123456789_";
    private static SessionFactory banco;

    @BeforeAll
    static void iniciar() throws Exception {
        try (var connection = DriverManager.getConnection(URL, "sa", "");
             var statement = connection.createStatement();
             var resource = MusicaCatalogoTest.class.getResourceAsStream("/db/migration/V38__catalogo_musical.sql")) {
            String sql = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            for (String parte : sql.split(";")) if (!parte.isBlank()) statement.execute(parte);
        }
        banco = new Configuration().setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", URL).setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.connection.password", "").setProperty("hibernate.hbm2ddl.auto", "validate")
                .addAnnotatedClass(EstiloMusicalModel.class).addAnnotatedClass(MusicaVideoModel.class).buildSessionFactory();
    }
    @AfterAll static void fechar() { if (banco != null) banco.close(); }
    @BeforeEach void limpar() {
        try (var em = banco.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("delete from MusicaVideoModel v where v.estilo.nomeChave <> 'lo-fi'").executeUpdate();
            em.createQuery("delete from EstiloMusicalModel e where e.nomeChave <> 'lo-fi'").executeUpdate();
            em.getTransaction().commit();
        }
    }
    private <T> T executar(Function<MusicaService, T> acao) {
        try (var em = banco.createEntityManager()) {
            em.getTransaction().begin();
            try {
                var factory = new JpaRepositoryFactory(em);
                var service = new MusicaService(factory.getRepository(EstiloMusicalRepository.class), factory.getRepository(MusicaVideoRepository.class));
                T result = acao.apply(service);
                em.getTransaction().commit();
                return result;
            } catch (RuntimeException ex) { em.getTransaction().rollback(); throw ex; }
        }
    }
    private List<MusicaDTO.Video> listar(Long id) { return executar(s -> s.listarVideos(id)); }
    private Long criar(String nome) { return executar(s -> s.salvarEstilo(null, nome).id()); }

    @Test void migracaoPreservaVideoAnterior() {
        assertThat(executar(MusicaService::listarEstilos)).anySatisfy(e -> { assertThat(e.nome()).isEqualTo("Lo-fi"); assertThat(e.totalVideos()).isEqualTo(1); });
    }
    @Test void estilosEUrlsPersistemEntreSessoes() {
        Long id = criar("Rock");
        var resultado = executar(s -> s.adicionarVideos(id, List.of(A, "https://youtu.be/X4VbdwhkE10", B)));
        assertThat(resultado).isEqualTo(new MusicaDTO.Adicao(2, 1));
        assertThat(listar(id)).hasSize(2).allSatisfy(v -> {
            assertThat(v.estiloId()).isEqualTo(id);
            assertThat(v.url()).startsWith("https://www.youtube.com/watch?v=");
            assertThat(v.embedUrl()).startsWith("https://www.youtube.com/embed/");
        });
        assertThat(executar(MusicaService::listarEstilos)).filteredOn(e -> e.id().equals(id)).extracting(MusicaDTO.Estilo::totalVideos).containsExactly(2L);
    }
    @Test void nomeDoEstiloIgnoraCaixaAcentosEEspacos() {
        Long id = criar("  Sertanéjo  ");
        assertThatThrownBy(() -> criar("sertanejo")).isInstanceOf(BusinessRuleException.class);
        assertThat(executar(s -> s.salvarEstilo(id, "Sertanejo")).nome()).isEqualTo("Sertanejo");
    }
    @Test void loteInvalidoNaoGravaNenhumVideo() {
        Long id = criar("Rock");
        assertThatThrownBy(() -> executar(s -> s.adicionarVideos(id, List.of(A, "https://example.com")))).isInstanceOf(BusinessRuleException.class).hasMessageStartingWith("Link 2:");
        assertThat(listar(id)).isEmpty();
    }
    @Test void sorteioFicaNoEstiloEEvitaRepeticaoImediata() {
        Long rock = criar("Rock"); Long sertanejo = criar("Sertanejo");
        executar(s -> s.adicionarVideos(rock, List.of(A, B)));
        executar(s -> s.adicionarVideos(sertanejo, List.of(C)));
        var previous = executar(s -> s.sortear(rock, null));
        for (int i = 0; i < 15; i++) {
            Long anterior = previous.id();
            var next = executar(s -> s.sortear(rock, anterior));
            assertThat(next.estiloId()).isEqualTo(rock);
            assertThat(next.id()).isNotEqualTo(anterior);
            assertThat(next.url()).isNotEqualTo("https://www.youtube.com/watch?v=0123456789_");
            previous = next;
        }
    }
    @Test void umUnicoVideoPodeSerSorteadoNovamente() {
        Long id = criar("Sertanejo"); executar(s -> s.adicionarVideos(id, List.of(A)));
        var first = executar(s -> s.sortear(id, null));
        assertThat(executar(s -> s.sortear(id, first.id())).id()).isEqualTo(first.id());
    }
    @Test void estiloVazioOuInexistenteRetornaErroClaro() {
        Long id = criar("Vazio");
        assertThatThrownBy(() -> executar(s -> s.sortear(id, null))).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("não possui vídeos");
        assertThatThrownBy(() -> executar(s -> s.sortear(99999L, null))).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("não encontrado");
    }
    @Test void mesmoVideoPodeEstarEmEstilosDiferentes() {
        Long rock = criar("Rock"); Long pop = criar("Pop");
        executar(s -> s.adicionarVideos(rock, List.of(A))); executar(s -> s.adicionarVideos(pop, List.of(A)));
        assertThat(listar(rock)).hasSize(1);
        assertThat(listar(pop)).hasSize(1);
    }
    @Test void editarERemoverAtualizaCatalogoENaoPermiteDuplicatas() {
        Long id = criar("Rock"); executar(s -> s.adicionarVideos(id, List.of(A, B)));
        var lista = executar(s -> s.listarVideos(id));
        Long videoId = lista.get(0).id();
        assertThatThrownBy(() -> executar(s -> s.editarVideo(videoId, A))).isInstanceOf(BusinessRuleException.class);
        assertThat(executar(s -> s.editarVideo(videoId, C)).url()).endsWith("0123456789_");
        executar(s -> { s.excluirVideo(videoId); return null; });
        assertThat(listar(id)).hasSize(1);
    }
    @Test void exigeRemoverVideosAntesDeExcluirEstilo() {
        Long id = criar("Rock"); executar(s -> s.adicionarVideos(id, List.of(A)));
        assertThatThrownBy(() -> executar(s -> { s.excluirEstilo(id); return null; })).isInstanceOf(BusinessRuleException.class);
        Long videoId = executar(s -> s.listarVideos(id)).get(0).id();
        executar(s -> { s.excluirVideo(videoId); s.excluirEstilo(id); return null; });
        assertThat(executar(MusicaService::listarEstilos)).noneMatch(e -> e.id().equals(id));
    }
}
