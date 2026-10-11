package dev.LzGuimaraes.FocusLifeHub.Admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminDespesaDTO;
import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminUserRecordsDTO;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaRepository;
import dev.LzGuimaraes.FocusLifeHub.Despesa.TipoLancamento;
import dev.LzGuimaraes.FocusLifeHub.Estudos.EstudosMapper;
import dev.LzGuimaraes.FocusLifeHub.Estudos.EstudosRepository;
import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Metas.Enum.MetaStatus;
import dev.LzGuimaraes.FocusLifeHub.Metas.MetasMapper;
import dev.LzGuimaraes.FocusLifeHub.Metas.MetasModel;
import dev.LzGuimaraes.FocusLifeHub.Metas.MetasRepository;
import dev.LzGuimaraes.FocusLifeHub.Metas.dto.MetasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.Prioridade;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasMapper;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasModel;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasRepository;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.dto.TarefasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * Regras das consultas administrativas por módulo (serviço puro, sem banco).
 *
 * Foco: (a) o ESCOPO é sempre o usuário-alvo recebido como argumento (o que
 * impede consultar terceiros via parâmetros); (b) filtros são repassados JUNTO
 * com o alvo (o banco aplica com AND); (c) a paginação é normalizada (teto de
 * tamanho, ordenação padrão). Alvo inexistente -> 404 sem tocar nos dados.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserRecordsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TarefasRepository tarefasRepository;
    @Mock private MetasRepository metasRepository;
    @Mock private EstudosRepository estudosRepository;
    @Mock private AtivoRepository ativoRepository;
    @Mock private DespesaRepository despesaRepository;
    @Mock private TarefasMapper tarefasMapper;
    @Mock private MetasMapper metasMapper;
    @Mock private EstudosMapper estudosMapper;

    @InjectMocks
    private AdminUserRecordsService service;

    // ------------------------------------------------------------------- TAREFAS

    @Test
    void tarefasUsamOUsuarioAlvoEIdentificamOAlvoNaResposta() {
        TarefasModel model = new TarefasModel();
        TarefasResponseDTO dto = new TarefasResponseDTO(1L, "T", TarefaStatus.PENDENTE, Prioridade.alta, null, null, 9L);

        when(userRepository.existsById(9L)).thenReturn(true);
        when(tarefasRepository.buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(model)));
        when(tarefasMapper.toResponse(model)).thenReturn(dto);

        AdminUserRecordsDTO<TarefasResponseDTO> resultado =
                service.tarefasDoUsuario(9L, null, null, PageRequest.of(0, 20));

        assertThat(resultado.usuarioId()).isEqualTo(9L);
        assertThat(resultado.pagina().getContent()).containsExactly(dto);

        // O repositório é chamado com o ALVO (9) e sem filtros (flags false).
        verify(tarefasRepository).buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), any(Pageable.class));
    }

    @Test
    void tamanhoDePaginaEhLimitadoAoTetoEEscopoPreservado() {
        when(userRepository.existsById(9L)).thenReturn(true);
        when(tarefasRepository.buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        // Cliente tenta puxar 5000 itens de uma vez -> teto de 100.
        service.tarefasDoUsuario(9L, null, null, PageRequest.of(0, 5000));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tarefasRepository).buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), captor.capture());
        Pageable usado = captor.getValue();
        assertThat(usado.getPageSize()).isEqualTo(AdminUserRecordsService.TAMANHO_MAXIMO_PAGINA);
        assertThat(usado.getPageNumber()).isZero();
        // Sem ordenação do cliente, aplica a padrão (id desc) -> resultado determinístico.
        assertThat(usado.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Test
    void paginacaoAusenteUsaPadroes() {
        when(userRepository.existsById(9L)).thenReturn(true);
        when(tarefasRepository.buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.tarefasDoUsuario(9L, null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tarefasRepository).buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(AdminUserRecordsService.TAMANHO_PADRAO_PAGINA);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    void ordenacaoInformadaPeloClienteEhRespeitada() {
        when(userRepository.existsById(9L)).thenReturn(true);
        when(tarefasRepository.buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.tarefasDoUsuario(9L, null, null, PageRequest.of(2, 10, Sort.by("titulo")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tarefasRepository).buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("titulo"));
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
    }

    @Test
    void filtrosDeTarefasSaoRepassadosComOAlvo() {
        when(userRepository.existsById(7L)).thenReturn(true);
        when(tarefasRepository.buscarDoUsuario(eq(7L), eq(true), eq(TarefaStatus.Concluida),
                eq(true), eq(Prioridade.alta), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.tarefasDoUsuario(7L, TarefaStatus.Concluida, Prioridade.alta, PageRequest.of(0, 20));

        // Alvo E filtros (com flags de presença) na MESMA chamada: o banco
        // aplica tudo com AND, então o filtro nunca troca o dono dos dados.
        verify(tarefasRepository).buscarDoUsuario(eq(7L), eq(true), eq(TarefaStatus.Concluida),
                eq(true), eq(Prioridade.alta), any(Pageable.class));
    }

    // --------------------------------------------------------------------- METAS

    @Test
    void metasUsamOUsuarioAlvoComFiltroDeStatus() {
        MetasModel model = new MetasModel();
        MetasResponseDTO dto = new MetasResponseDTO(2L, "M", null, 10f, null, MetaStatus.Ativa, 9L);

        when(userRepository.existsById(9L)).thenReturn(true);
        when(metasRepository.buscarDoUsuario(eq(9L), eq(true), eq(MetaStatus.Ativa), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(model)));
        when(metasMapper.toResponse(model)).thenReturn(dto);

        AdminUserRecordsDTO<MetasResponseDTO> resultado =
                service.metasDoUsuario(9L, MetaStatus.Ativa, PageRequest.of(0, 20));

        assertThat(resultado.usuarioId()).isEqualTo(9L);
        assertThat(resultado.pagina().getContent()).containsExactly(dto);
        verify(metasRepository).buscarDoUsuario(eq(9L), eq(true), eq(MetaStatus.Ativa), any(Pageable.class));
    }

    @Test
    void metasSemFiltroPassamFlagFalsaJuntoComOAlvo() {
        when(userRepository.existsById(9L)).thenReturn(true);
        when(metasRepository.buscarDoUsuario(eq(9L), eq(false), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.metasDoUsuario(9L, null, PageRequest.of(0, 20));

        verify(metasRepository).buscarDoUsuario(eq(9L), eq(false), isNull(), any(Pageable.class));
    }

    // ------------------------------------------------------------------- ESTUDOS

    @Test
    void estudosUsamOUsuarioAlvoJuntoComOsFiltros() {
        LocalDate de = LocalDate.of(2026, 10, 1);
        LocalDate ate = LocalDate.of(2026, 10, 31);

        when(userRepository.existsById(9L)).thenReturn(true);
        when(estudosRepository.buscarDoUsuario(eq(9L), eq(true), eq(55L), eq(true), eq(de), eq(true), eq(ate), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.estudosDoUsuario(9L, 55L, de, ate, PageRequest.of(0, 20));

        // Um materia_id de terceiro NÃO amplia o escopo: o alvo (9) vai sempre
        // junto e o repositório casa a posse pela matéria dona (AND no banco).
        verify(estudosRepository).buscarDoUsuario(eq(9L), eq(true), eq(55L), eq(true), eq(de), eq(true), eq(ate), any(Pageable.class));
    }

    @Test
    void estudosSemFiltrosPassamFlagsFalsasJuntoComOAlvo() {
        when(userRepository.existsById(9L)).thenReturn(true);
        when(estudosRepository.buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), eq(false), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.estudosDoUsuario(9L, null, null, null, PageRequest.of(0, 20));

        verify(estudosRepository).buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), eq(false), isNull(), any(Pageable.class));
    }

    // ------------------------------------------------------------- INVESTIMENTOS

    private AtivoModel ativo(String nome, CategoriaInvestimento categoria,
                             String nomeCarteira, String moeda) {
        CarteiraInvestimentoModel carteira = new CarteiraInvestimentoModel();
        carteira.setId(30L);
        carteira.setNome(nomeCarteira);
        carteira.setMoeda(moeda);

        AtivoModel ativo = new AtivoModel();
        ativo.setId(5L);
        ativo.setNome(nome);
        ativo.setCategoriaInvestimento(categoria);
        ativo.setQuantidade(10f);
        ativo.setValorUnitario(30f);
        ativo.setPrecoAtual(31f);
        ativo.setSaldo(310f);
        ativo.setCarteiraInvestimento(carteira);
        return ativo;
    }

    @Test
    void investimentosUsamOUsuarioAlvoComFiltroDeCategoriaEIncluemACarteira() {
        AtivoModel model = ativo("PETR4", CategoriaInvestimento.ACOES, "Carteira Teste", "BRL");

        when(userRepository.existsById(9L)).thenReturn(true);
        when(ativoRepository.buscarDoUsuario(eq(9L), eq(true), eq(CategoriaInvestimento.ACOES), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(model)));

        AdminUserRecordsDTO<AdminAtivoDTO> resultado =
                service.investimentosDoUsuario(9L, CategoriaInvestimento.ACOES, PageRequest.of(0, 20));

        assertThat(resultado.usuarioId()).isEqualTo(9L);
        assertThat(resultado.pagina().getContent()).hasSize(1);
        AdminAtivoDTO dto = resultado.pagina().getContent().get(0);
        assertThat(dto.nome()).isEqualTo("PETR4");
        assertThat(dto.categoriaInvestimento()).isEqualTo(CategoriaInvestimento.ACOES);
        // Sem catálogo vinculado, o preço guardado na posição é mantido.
        assertThat(dto.precoAtual()).isEqualTo(31f);
        assertThat(dto.carteiraId()).isEqualTo(30L);
        assertThat(dto.carteiraNome()).isEqualTo("Carteira Teste");
        assertThat(dto.carteiraMoeda()).isEqualTo("BRL");

        verify(ativoRepository).buscarDoUsuario(eq(9L), eq(true), eq(CategoriaInvestimento.ACOES), any(Pageable.class));
    }

    @Test
    void precoDoCatalogoPrevaleceNaVisaoAdminComoNoEndpointComum() {
        AtivoModel model = ativo("VALE3", CategoriaInvestimento.ACOES, "Carteira Teste", "BRL");
        AtivoCadastroModel catalogo = new AtivoCadastroModel();
        catalogo.setPrecoAtual(55f);
        model.setAtivoCadastro(catalogo);

        when(userRepository.existsById(9L)).thenReturn(true);
        when(ativoRepository.buscarDoUsuario(eq(9L), eq(false), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(model)));

        AdminUserRecordsDTO<AdminAtivoDTO> resultado =
                service.investimentosDoUsuario(9L, null, PageRequest.of(0, 20));

        assertThat(resultado.pagina().getContent().get(0).precoAtual()).isEqualTo(55f);
        verify(ativoRepository).buscarDoUsuario(eq(9L), eq(false), isNull(), any(Pageable.class));
    }

    // ------------------------------------------------------------------- CONTAS

    @Test
    void contasUsamOUsuarioAlvoComFiltrosDeTipoEPagoEIncluemACarteira() {
        CarteiraDividasModel carteira = new CarteiraDividasModel();
        carteira.setId(40L);
        carteira.setNome("Carteira de Dívidas");
        carteira.setMoeda("BRL");

        DespesaModel model = new DespesaModel();
        model.setId(7L);
        model.setNome("Aluguel");
        model.setSaldo(1500f);
        model.setPago(false);
        model.setTipo(TipoLancamento.DESPESA);
        model.setCarteiraDividas(carteira);

        when(userRepository.existsById(9L)).thenReturn(true);
        when(despesaRepository.buscarDoUsuario(eq(9L), eq(true), eq(TipoLancamento.DESPESA),
                eq(true), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(model)));

        AdminUserRecordsDTO<AdminDespesaDTO> resultado =
                service.contasDoUsuario(9L, TipoLancamento.DESPESA, false, PageRequest.of(0, 20));

        assertThat(resultado.usuarioId()).isEqualTo(9L);
        AdminDespesaDTO dto = resultado.pagina().getContent().get(0);
        assertThat(dto.nome()).isEqualTo("Aluguel");
        assertThat(dto.saldo()).isEqualTo(1500f);
        assertThat(dto.pago()).isFalse();
        assertThat(dto.tipo()).isEqualTo(TipoLancamento.DESPESA);
        assertThat(dto.carteiraNome()).isEqualTo("Carteira de Dívidas");

        verify(despesaRepository).buscarDoUsuario(eq(9L), eq(true), eq(TipoLancamento.DESPESA),
                eq(true), eq(false), any(Pageable.class));
    }

    @Test
    void contasSemFiltrosPassamFlagsFalsasJuntoComOAlvo() {
        when(userRepository.existsById(9L)).thenReturn(true);
        when(despesaRepository.buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.contasDoUsuario(9L, null, null, PageRequest.of(0, 20));

        verify(despesaRepository).buscarDoUsuario(eq(9L), eq(false), isNull(), eq(false), isNull(), any(Pageable.class));
    }

    // ------------------------------------------------------------------- SEGURANÇA

    @Test
    void alvoInexistenteLanca404SemConsultarDados() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.tarefasDoUsuario(99L, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> service.metasDoUsuario(99L, null, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> service.estudosDoUsuario(99L, null, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> service.investimentosDoUsuario(99L, null, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> service.contasDoUsuario(99L, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);

        // Nenhuma consulta de dados foi disparada para um alvo inexistente.
        verifyNoInteractions(tarefasRepository, metasRepository, estudosRepository,
                ativoRepository, despesaRepository);
    }
}
