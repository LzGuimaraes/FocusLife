package dev.LzGuimaraes.FocusLifeHub.Carteira;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Carteira.dto.DiagnosticoFinanceiroDTO;
import dev.LzGuimaraes.FocusLifeHub.Carteira.dto.DiagnosticoFinanceiroDTO.CarteiraResumoDTO;
import dev.LzGuimaraes.FocusLifeHub.Carteira.dto.DiagnosticoFinanceiroDTO.ReparoResultadoDTO;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Calculo.PercentualCalculator;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * Diagnóstico e reparo dos dados financeiros legados.
 *
 * POR QUE ISSO EXISTE
 * As carteiras foram divididas em tabelas novas (V8..V11). Na V8 o vínculo das
 * contas antigas só era preenchido quando a carteira de origem tinha
 * `tipo_carteira = 'INVESTIMENTO'` (ou 'DESPESAS'). Se o tipo fosse nulo ou
 * qualquer outro valor, a posição ficou ÓRFÃ — e como todas as consultas passam
 * pela carteira, ela SUMIU da interface: sem erro, sem aviso, e a tabela antiga
 * ainda foi removida pela V11. Este serviço mostra onde os dados estão e permite
 * religar as posições órfãs a uma carteira.
 *
 * O reparo é restrito a ADMIN porque `ativo`/`despesa` não têm dono próprio: a
 * posse vem da carteira, então uma linha órfã não permite saber a qual usuário
 * pertence. Só um administrador pode afetá-la conscientemente.
 */
@Service
public class DiagnosticoFinanceiroService {

    private static final String MOEDA_PADRAO = "BRL";
    private static final String NOME_RECUPERADA = "Investimentos recuperados";
    private static final String NOME_RECUPERADA_DIVIDAS = "Despesas recuperadas";

    private final CarteiraInvestimentoRepository carteiraRepository;
    private final CarteiraDividasRepository carteiraDividasRepository;
    private final AtivoRepository ativoRepository;
    private final DespesaRepository despesaRepository;
    private final UserRepository userRepository;
    private final ContextoUsuario contextoUsuario;
    private final PercentualCalculator calculator;

    public DiagnosticoFinanceiroService(CarteiraInvestimentoRepository carteiraRepository,
                                        CarteiraDividasRepository carteiraDividasRepository,
                                        AtivoRepository ativoRepository,
                                        DespesaRepository despesaRepository,
                                        UserRepository userRepository,
                                        ContextoUsuario contextoUsuario,
                                        PercentualCalculator calculator) {
        this.carteiraRepository = carteiraRepository;
        this.carteiraDividasRepository = carteiraDividasRepository;
        this.ativoRepository = ativoRepository;
        this.despesaRepository = despesaRepository;
        this.userRepository = userRepository;
        this.contextoUsuario = contextoUsuario;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public DiagnosticoFinanceiroDTO diagnostico() {
        Long userId = contextoUsuario.id();

        List<CarteiraResumoDTO> carteiras = new ArrayList<>();
        long totalPosicoes = 0L;
        for (CarteiraInvestimentoModel carteira : carteiraRepository.findByUserId(userId, Pageable.unpaged())) {
            List<AtivoModel> posicoes = ativoRepository.findByCarteiraInvestimentoId(carteira.getId());
            double valor = posicoes.stream().mapToDouble(calculator::valorPosicao).sum();
            totalPosicoes += posicoes.size();
            carteiras.add(new CarteiraResumoDTO(carteira.getId(), carteira.getNome(), carteira.getMoeda(),
                    posicoes.size(), calculator.moeda(valor)));
        }

        List<CarteiraResumoDTO> carteirasDividas = new ArrayList<>();
        for (CarteiraDividasModel carteira : carteiraDividasRepository.findByUserId(userId, Pageable.unpaged())) {
            List<DespesaModel> despesas = despesaRepository.findByCarteiraDividasId(carteira.getId());
            double valor = despesas.stream()
                    .mapToDouble(d -> d.getSaldo() != null ? Math.abs(d.getSaldo()) : 0d)
                    .sum();
            carteirasDividas.add(new CarteiraResumoDTO(carteira.getId(), carteira.getNome(), carteira.getMoeda(),
                    despesas.size(), calculator.moeda(valor)));
        }

        // Órfãos são globais (não têm dono) — só fazem sentido para quem repara.
        boolean admin = contextoUsuario.isAdmin();
        boolean instanciaUnica = userRepository.count() == 1L;
        boolean podeReparar = admin || instanciaUnica;

        long posicoesOrfas = podeReparar ? ativoRepository.countByCarteiraInvestimentoIsNull() : 0L;
        long despesasOrfas = podeReparar ? despesaRepository.countByCarteiraDividasIsNull() : 0L;

        String limitacao = null;
        if (!podeReparar) {
            limitacao = "Existem registros financeiros sem carteira, mas eles não guardam o dono (a tabela antiga já "
                    + "foi removida). Peça a um administrador para executar a recuperação.";
        }

        List<String> alertas = new ArrayList<>();
        if (carteiras.isEmpty()) {
            alertas.add("Você não tem nenhuma carteira de investimento cadastrada.");
        } else if (totalPosicoes == 0) {
            alertas.add("Nenhuma das suas carteiras de investimento tem posições cadastradas.");
        }
        if (posicoesOrfas > 0) {
            alertas.add(posicoesOrfas + " posição(ões) de investimento ficaram SEM carteira numa migração antiga e, "
                    + "por isso, não aparecem em nenhuma tela do sistema.");
        }
        if (despesasOrfas > 0) {
            alertas.add(despesasOrfas + " despesa(s) também ficaram sem carteira (mesma causa, do lado das contas a pagar).");
        }
        if (podeReparar && posicoesOrfas == 0 && despesasOrfas == 0) {
            alertas.add("Nenhum registro órfão encontrado — todos os dados estão vinculados a uma carteira.");
        }

        return new DiagnosticoFinanceiroDTO(carteiras, carteirasDividas, totalPosicoes,
                posicoesOrfas, despesasOrfas, podeReparar, limitacao, alertas);
    }

    /**
     * Religa as posições e despesas órfãs a uma carteira.
     *
     * @param carteiraInvestimentoId destino das posições (null = primeira carteira do usuário, ou cria uma)
     * @param carteiraDividasId      destino das despesas (null = primeira carteira de dívidas, ou cria uma)
     */
    @Transactional
    public ReparoResultadoDTO reparar(Long carteiraInvestimentoId, Long carteiraDividasId) {
        Long userId = contextoUsuario.id();
        exigirPermissaoDeReparo();

        List<String> mensagens = new ArrayList<>();
        int posicoes = 0;
        int despesas = 0;
        Long destinoInvestimento = null;
        Long destinoDividas = null;

        List<AtivoModel> posicoesOrfas = ativoRepository.findByCarteiraInvestimentoIsNull();
        if (!posicoesOrfas.isEmpty()) {
            CarteiraInvestimentoModel destino = resolverCarteiraInvestimento(userId, carteiraInvestimentoId, mensagens);
            destinoInvestimento = destino.getId();
            for (AtivoModel posicao : posicoesOrfas) {
                posicao.setCarteiraInvestimento(destino);
            }
            ativoRepository.saveAll(posicoesOrfas);
            posicoes = posicoesOrfas.size();
            mensagens.add(posicoes + " posição(ões) religada(s) à carteira \"" + destino.getNome() + "\".");
        }

        List<DespesaModel> despesasOrfas = despesaRepository.findByCarteiraDividasIsNull();
        if (!despesasOrfas.isEmpty()) {
            CarteiraDividasModel destino = resolverCarteiraDividas(userId, carteiraDividasId, mensagens);
            destinoDividas = destino.getId();
            for (DespesaModel despesa : despesasOrfas) {
                despesa.setCarteiraDividas(destino);
            }
            despesaRepository.saveAll(despesasOrfas);
            despesas = despesasOrfas.size();
            mensagens.add(despesas + " despesa(s) religada(s) à carteira \"" + destino.getNome() + "\".");
        }

        if (posicoes == 0 && despesas == 0) {
            mensagens.add("Nada a reparar: não há registros sem carteira.");
        }

        return new ReparoResultadoDTO(posicoes, despesas, destinoInvestimento, destinoDividas, mensagens);
    }

    private void exigirPermissaoDeReparo() {
        if (contextoUsuario.isAdmin() || userRepository.count() == 1L) {
            return;
        }
        throw new BusinessRuleException("Apenas administradores podem reparar os dados financeiros legados: as "
                + "linhas sem carteira não guardam o dono e a atribuição seria um chute.");
    }

    /** Carteira escolhida → validada; não informada → a primeira do usuário (ou criada). */
    private CarteiraInvestimentoModel resolverCarteiraInvestimento(Long userId, Long escolhida, List<String> mensagens) {
        if (escolhida != null) {
            return carteiraDoUsuario(carteiraRepository.findById(escolhida)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Carteira de Investimento com ID " + escolhida + " não encontrada")), userId, escolhida);
        }
        CarteiraInvestimentoModel carteira = carteiraRepository.findByUserId(userId, Pageable.ofSize(1))
                .stream().findFirst().orElse(null);
        if (carteira != null) {
            mensagens.add("Destino das posições: carteira existente \"" + carteira.getNome() + "\".");
            return carteira;
        }
        CarteiraInvestimentoModel nova = new CarteiraInvestimentoModel();
        nova.setNome(NOME_RECUPERADA);
        nova.setMoeda(MOEDA_PADRAO);
        nova.setUser(usuario(userId));
        CarteiraInvestimentoModel salva = carteiraRepository.save(nova);
        mensagens.add("Nenhuma carteira existia, então \"" + NOME_RECUPERADA + "\" foi criada para receber as posições.");
        return salva;
    }

    private CarteiraDividasModel resolverCarteiraDividas(Long userId, Long escolhida, List<String> mensagens) {
        if (escolhida != null) {
            return carteiraDoUsuario(carteiraDividasRepository.findById(escolhida)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Carteira de Dívidas com ID " + escolhida + " não encontrada")), userId, escolhida);
        }
        CarteiraDividasModel carteira = carteiraDividasRepository.findByUserId(userId, Pageable.ofSize(1))
                .stream().findFirst().orElse(null);
        if (carteira != null) {
            mensagens.add("Destino das despesas: carteira existente \"" + carteira.getNome() + "\".");
            return carteira;
        }
        CarteiraDividasModel nova = new CarteiraDividasModel();
        nova.setNome(NOME_RECUPERADA_DIVIDAS);
        nova.setMoeda(MOEDA_PADRAO);
        nova.setUser(usuario(userId));
        CarteiraDividasModel salva = carteiraDividasRepository.save(nova);
        mensagens.add("Nenhuma carteira de dívidas existia, então \"" + NOME_RECUPERADA_DIVIDAS
                + "\" foi criada para receber as despesas.");
        return salva;
    }

    /** Garante que a carteira escolhida pertence ao usuário autenticado. */
    private <T extends CarteiraModel> T carteiraDoUsuario(T carteira, Long userId, Long id) {
        if (carteira.getUser() == null || !userId.equals(carteira.getUser().getId())) {
            throw new ResourceNotFoundException("Carteira com ID " + id + " não encontrada");
        }
        return carteira;
    }

    private UserModel usuario(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
    }
}
