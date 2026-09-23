package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroRepository;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Calculo.PercentualCalculator;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.CarteiraIdealRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.CarteiraIdealResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.MeusAtivosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ResumoIdealDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia.EstrategiaService;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.MetaAtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.MetaAtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.CarteiraLookup;

/**
 * Carteira Ideal (Módulo 1) + comparativo com a carteira real (Módulo 10).
 *
 * O sistema não impõe metodologia: aqui só são armazenados, validados e
 * comparados os percentuais definidos pelo USUÁRIO.
 */
@Service
public class CarteiraIdealService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal TOLERANCIA = new BigDecimal("0.01");

    private final CarteiraIdealClasseRepository classeRepository;
    private final CarteiraIdealSubclasseRepository subclasseRepository;
    private final CarteiraIdealSetorRepository setorRepository;
    private final MetaAtivoRepository metaAtivoRepository;
    private final AtivoCadastroRepository ativoCadastroRepository;
    private final AtivoRepository ativoRepository;
    private final EstrategiaService estrategiaService;
    private final CarteiraLookup carteiraLookup;
    private final PercentualCalculator calculator;

    public CarteiraIdealService(CarteiraIdealClasseRepository classeRepository,
                                CarteiraIdealSubclasseRepository subclasseRepository,
                                CarteiraIdealSetorRepository setorRepository,
                                MetaAtivoRepository metaAtivoRepository,
                                AtivoCadastroRepository ativoCadastroRepository,
                                AtivoRepository ativoRepository,
                                EstrategiaService estrategiaService,
                                CarteiraLookup carteiraLookup,
                                PercentualCalculator calculator) {
        this.classeRepository = classeRepository;
        this.subclasseRepository = subclasseRepository;
        this.setorRepository = setorRepository;
        this.metaAtivoRepository = metaAtivoRepository;
        this.ativoCadastroRepository = ativoCadastroRepository;
        this.ativoRepository = ativoRepository;
        this.estrategiaService = estrategiaService;
        this.carteiraLookup = carteiraLookup;
        this.calculator = calculator;
    }

    /* ══════════════════════════════════════════════════════════════════
       Leitura da configuração
       ══════════════════════════════════════════════════════════════════ */

    @Transactional(readOnly = true)
    public CarteiraIdealResponseDTO get(Long carteiraId) {
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(carteiraId);
        List<CarteiraIdealClasseModel> classes = classeRepository
                .findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId);
        List<CarteiraIdealSubclasseModel> subclasses = subclassesDasClasses(classes);
        List<MetaAtivoModel> metas = metaAtivoRepository
                .findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId);

        BigDecimal soma = somarPercentuaisClasses(classes);
        return new CarteiraIdealResponseDTO(
                carteira.getId(),
                carteira.getMoeda(),
                carteira.getEstrategia() != null ? carteira.getEstrategia().getId() : null,
                carteira.getEstrategia() != null ? carteira.getEstrategia().getNome() : null,
                soma,
                montarClassesResposta(classes, subclasses),
                montarMetasResposta(metas),
                montarAvisos(classes, subclasses, metas, soma, 0));
    }

    /* ══════════════════════════════════════════════════════════════════
       Gravação (replace-all transacional)
       ══════════════════════════════════════════════════════════════════ */

    /**
     * Salva a configuração completa da Carteira Ideal.
     *
     * REPLACE-ALL: o que não vier no payload deixa de existir. Como é uma tela
     * de configuração, isso mantém a validação das somas em um único lugar e
     * evita estados intermediários inconsistentes.
     *
     * Erros (400): soma das classes ≠ 100%, classe repetida, subclasse
     * repetida, meta repetida, ativo do catálogo inexistente, subclasse
     * referenciada inexistente.
     * Avisos (não bloqueiam): subclasses/metas somando mais que a classe.
     */
    @Transactional
    public CarteiraIdealResponseDTO save(Long carteiraId, CarteiraIdealRequestDTO dto) {
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(carteiraId);

        List<CarteiraIdealRequestDTO.ClasseIdealRequestDTO> classesReq =
                (dto.classes() == null) ? List.of() : dto.classes();
        List<CarteiraIdealRequestDTO.MetaIdealRequestDTO> metasReq =
                (dto.metas() == null) ? List.of() : dto.metas();

        validarClasses(classesReq);
        Map<UUID, AtivoCadastroModel> catalogo = carregarCatalogo(metasReq);

        // ── Classificacao das POSICOES (V26/V28) ──
        // Subclasses e setores sao RECRIADOS a cada save, e ativo.subclasse_id /
        // ativo.setor_id apontam para eles: sem soltar essas FKs o delete estoura
        // ("registros vinculados que impedem a exclusao") — era o que impedia
        // salvar a Carteira Ideal depois de classificar uma renda fixa.
        // A classificacao e guardada POR NOME, liberada antes do delete e
        // religada depois de recriar: o replace-all nao pode apagar o trabalho
        // que o usuario fez na tela.
        List<ClassificacaoPosicao> classificacoes = new ArrayList<>();
        for (AtivoModel posicao : ativoRepository.findByCarteiraInvestimentoId(carteiraId)) {
            if (posicao.getSubclasse() == null && posicao.getSetor() == null) {
                continue;
            }
            classificacoes.add(new ClassificacaoPosicao(
                    posicao.getId(),
                    (posicao.getSubclasse() != null) ? posicao.getSubclasse().getNome() : null,
                    (posicao.getSetor() != null) ? posicao.getSetor().getNome() : null));
            posicao.setSubclasse(null);
            posicao.setSetor(null);
        }
        if (!classificacoes.isEmpty()) {
            ativoRepository.flush();
        }

        // ── Apaga a configuração anterior ──
        // Ordem importa: os SETORES apontam para as subclasses (e a posição/meta
        // aponta para o setor), então os filhos saem antes dos pais.
        Long metaComSetor = metaAtivoRepository
                .findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId).stream()
                .filter(m -> m.getSetor() != null)
                .count();
        if (metaComSetor > 0) {
            // Limpa as referências antes de remover os setores (sem cascade).
            for (MetaAtivoModel m : metaAtivoRepository
                    .findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId)) {
                m.setSetor(null);
            }
            metaAtivoRepository.flush();
        }
        metaAtivoRepository.deleteByCarteiraInvestimentoId(carteiraId);
        metaAtivoRepository.flush();

        List<Long> subclasseIds = subclassesDasClasses(
                classeRepository.findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId)).stream()
                .map(CarteiraIdealSubclasseModel::getId)
                .toList();
        if (!subclasseIds.isEmpty()) {
            setorRepository.deleteBySubclasseIdIn(subclasseIds);
            setorRepository.flush();
        }
        classeRepository.deleteByCarteiraInvestimentoId(carteiraId);
        classeRepository.flush();

        // ── Recria classes + subclasses ──
        Map<String, CarteiraIdealSubclasseModel> subclassePorChave = new HashMap<>();
        Map<String, CarteiraIdealSetorModel> setorPorChave = new HashMap<>();
        for (CarteiraIdealRequestDTO.ClasseIdealRequestDTO c : classesReq) {
            CarteiraIdealClasseModel classe = new CarteiraIdealClasseModel();
            classe.setClasse(c.classe());
            classe.setPercentualIdeal(calculator.percentualNormalizado(c.percentual_ideal()));
            classe.setTolerancia(toleranciaDe(c.tolerancia()));
            classe.setLimiteMaximo(c.limite_maximo());
            classe.setOrdem(c.ordem() == null ? 0 : c.ordem());
            classe.setCarteiraInvestimento(carteira);
            classe = classeRepository.save(classe);

            int ordemSub = 0;
            for (CarteiraIdealRequestDTO.SubclasseIdealRequestDTO s : subclassesDe(c)) {
                CarteiraIdealSubclasseModel sub = new CarteiraIdealSubclasseModel();
                sub.setNome(s.nome().trim());
                sub.setPercentualIdeal(calculator.percentualNormalizado(s.percentual_ideal()));
                sub.setTolerancia(toleranciaDe(s.tolerancia()));
                sub.setLimiteMaximo(s.limite_maximo());
                sub.setOrdem(s.ordem() == null ? ordemSub : s.ordem());
                sub.setClasse(classe);
                sub = subclasseRepository.save(sub);
                subclassePorChave.put(chaveSubclasse(c.classe(), s.nome()), sub);
                ordemSub++;

                // ── SETORES da subclasse (nível opcional, V28) ──
                int ordemSetor = 0;
                for (CarteiraIdealRequestDTO.SetorIdealRequestDTO st : setoresDe(s)) {
                    CarteiraIdealSetorModel setor = new CarteiraIdealSetorModel();
                    setor.setNome(st.nome().trim());
                    setor.setPercentualIdeal(calculator.percentualNormalizado(st.percentual_ideal()));
                    setor.setTolerancia(toleranciaDe(st.tolerancia()));
                    setor.setLimiteMaximo(st.limite_maximo());
                    setor.setOrdem(st.ordem() == null ? ordemSetor : st.ordem());
                    setor.setSubclasse(sub);
                    setor = setorRepository.save(setor);
                    setorPorChave.put(chaveSetor(c.classe(), s.nome(), st.nome()), setor);
                    ordemSetor++;
                }
            }
        }

        // ── Recria metas (subclasse resolvida por nome) ──
        int ordemMeta = 0;
        for (CarteiraIdealRequestDTO.MetaIdealRequestDTO m : metasReq) {
            MetaAtivoModel meta = new MetaAtivoModel();
            meta.setCarteiraInvestimento(carteira);
            meta.setAtivoCadastro(catalogo.get(m.ativo_cadastro_id()));
            meta.setClasse(m.classe());
            if (m.subclasse_nome() != null && !m.subclasse_nome().isBlank()) {
                CarteiraIdealSubclasseModel sub = subclassePorChave.get(chaveSubclasse(m.classe(), m.subclasse_nome()));
                if (sub == null) {
                    throw new BusinessRuleException("A subclasse \"" + m.subclasse_nome().trim()
                            + "\" não existe na classe " + m.classe() + " da Carteira Ideal.");
                }
                meta.setSubclasse(sub);
            }
            if (m.setor_nome() != null && !m.setor_nome().isBlank()) {
                if (meta.getSubclasse() == null) {
                    throw new BusinessRuleException("O ativo \"" + m.ativo_cadastro_id()
                            + "\" foi colocado num setor sem informar a subclasse dele.");
                }
                CarteiraIdealSetorModel setor = setorPorChave.get(
                        chaveSetor(m.classe(), m.subclasse_nome(), m.setor_nome()));
                if (setor == null) {
                    throw new BusinessRuleException("O setor \"" + m.setor_nome().trim()
                            + "\" não existe na subclasse \"" + m.subclasse_nome().trim() + "\".");
                }
                meta.setSetor(setor);
            }
            meta.setPercentualIdeal(calculator.percentualNormalizado(m.percentual_ideal()));
            meta.setTolerancia(toleranciaDe(m.tolerancia()));
            meta.setLimiteMaximo(m.limite_maximo());
            meta.setPrioridadeManual(m.prioridade_manual() == null ? 0 : m.prioridade_manual());
            meta.setOrdem(m.ordem() == null ? ordemMeta : m.ordem());
            metaAtivoRepository.save(meta);
            ordemMeta++;
        }

        // ── Religadura das POSICOES: mesma subclasse/setor, pelos NOMES salvos ──
        // Se a subclasse (ou o setor) saiu da configuracao, a posicao fica sem
        // classificacao — ela continua contando no alvo da CLASSE normalmente.
        for (ClassificacaoPosicao posicao : classificacoes) {
            AtivoModel ativo = ativoRepository.findById(posicao.ativoId()).orElse(null);
            if (ativo == null || posicao.subclasse() == null) {
                continue;
            }
            CategoriaInvestimento classeDaPosicao = ativo.getCategoriaInvestimento();
            if (classeDaPosicao == null) {
                continue;
            }
            ativo.setSubclasse(subclassePorChave.get(chaveSubclasse(classeDaPosicao, posicao.subclasse())));
            if (posicao.setor() != null) {
                ativo.setSetor(setorPorChave.get(
                        chaveSetor(classeDaPosicao, posicao.subclasse(), posicao.setor())));
            }
        }
        if (!classificacoes.isEmpty()) {
            ativoRepository.flush();
        }

        // ── Estratégia (authoritative: null/ausente desvincula) ──
        if (dto.estrategia_id() != null) {
            carteira.setEstrategia(estrategiaService.exigirDoUsuario(dto.estrategia_id()));
        } else {
            carteira.setEstrategia(null);
        }

        return get(carteiraId);
    }

    /* ══════════════════════════════════════════════════════════════════
       Comparativo Carteira Atual × Carteira Ideal
       ══════════════════════════════════════════════════════════════════ */

    @Transactional(readOnly = true)
    public ComparativoResponseDTO comparativo(Long carteiraId) {
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(carteiraId);

        List<CarteiraIdealClasseModel> classes = classeRepository
                .findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId);
        List<CarteiraIdealSubclasseModel> subclasses = subclassesDasClasses(classes);
        List<MetaAtivoModel> metas = metaAtivoRepository
                .findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId);

        List<PercentualCalculator.PosicaoSnapshot> posicoes = calculator.posicoes(carteiraId);
        double total = calculator.valorTotal(posicoes);
        Map<CategoriaInvestimento, Double> valorPorClasse = calculator.valorPorClasse(posicoes);
        Map<UUID, Double> valorPorTicker = calculator.valorPorAtivoCadastro(posicoes);
        Map<UUID, String> nomePorTicker = calculator.nomePorAtivoCadastro(posicoes);
        Map<UUID, CategoriaInvestimento> classePorTicker = classePorTicker(posicoes);
        int ativosSemMeta = 0;

        Map<Long, List<CarteiraIdealSubclasseModel>> subclassesPorClasse = subclasses.stream()
                .collect(Collectors.groupingBy(s -> s.getClasse().getId()));
        Map<CategoriaInvestimento, List<MetaAtivoModel>> metasPorClasse = metas.stream()
                .collect(Collectors.groupingBy(MetaAtivoModel::getClasse));

        // Posição → subclasse. A atribuição EXPLÍCITA na posição (V26, o caminho
        // da renda fixa/caixinhas sem ticker) tem prioridade; na falta dela, vale
        // a subclasse da meta do ticker (caminho dos ativos com ticker).
        Map<Long, Double> valorPorSubclasse = new HashMap<>();
        Map<Long, Double> valorPorSetor = new HashMap<>();
        for (AtivoModel posicao : ativoRepository.findByCarteiraInvestimentoId(carteiraId)) {
            Long subclasseId = subclasseDaPosicao(posicao, metas);
            if (subclasseId != null) {
                valorPorSubclasse.merge(subclasseId, calculator.valorPosicao(posicao), Double::sum);
            }
            Long setorId = setorDaPosicao(posicao, metas);
            if (setorId != null) {
                valorPorSetor.merge(setorId, calculator.valorPosicao(posicao), Double::sum);
            }
        }

        // Setores das subclasses (nível opcional): o percentual do setor é uma
        // FATIA DA SUBCLASSE, então o valor ideal sai do valor ideal da subclasse.
        Map<Long, List<CarteiraIdealSetorModel>> setoresPorSubclasse = setoresDasSubclasses(subclasses).stream()
                .collect(Collectors.groupingBy(s -> s.getSubclasse().getId()));

        BigDecimal soma = somarPercentuaisClasses(classes);

        // Classes do ideal + classes que existem só nas posições (para mostrar excesso).
        Set<CategoriaInvestimento> todasClasses = new LinkedHashSet<>();
        classes.forEach(c -> todasClasses.add(c.getClasse()));
        valorPorClasse.keySet().stream()
                .sorted(Comparator.comparing(CategoriaInvestimento::name))
                .forEach(todasClasses::add);

        List<ComparativoResponseDTO.ClasseComparativoDTO> classesDto = new ArrayList<>();
        for (CategoriaInvestimento classeEnum : todasClasses) {
            CarteiraIdealClasseModel ideal = classes.stream()
                    .filter(c -> c.getClasse() == classeEnum)
                    .findFirst()
                    .orElse(null);

            BigDecimal pIdeal = (ideal != null) ? ideal.getPercentualIdeal() : BigDecimal.ZERO;
            double vAtual = valorPorClasse.getOrDefault(classeEnum, 0d);
            double vIdeal = valorIdeal(pIdeal, total);

            List<CarteiraIdealSubclasseModel> subsDaClasse = (ideal != null)
                    ? subclassesPorClasse.getOrDefault(ideal.getId(), List.of())
                    : List.of();
            List<MetaAtivoModel> metasDaClasse = metasPorClasse.getOrDefault(classeEnum, List.of());

            List<ComparativoResponseDTO.SubclasseComparativoDTO> subDtos = new ArrayList<>();
            for (CarteiraIdealSubclasseModel sub : subsDaClasse) {
                double vSubAtual = valorPorSubclasse.getOrDefault(sub.getId(), 0d);
                // O percentual da subclasse é uma FATIA DA CLASSE (a soma das
                // subclasses fecha em 100% da classe, não da carteira). Sem isto,
                // uma subclasse de 60% aparecia como 60% do patrimônio inteiro e o
                // déficit dela ficava inflado, estragando o rateio do aporte.
                double vSubIdeal = valorIdeal(sub.getPercentualIdeal(), vIdeal);
                List<ComparativoResponseDTO.SetorComparativoDTO> setorDtos = new ArrayList<>();
                for (CarteiraIdealSetorModel st : setoresPorSubclasse.getOrDefault(sub.getId(), List.of())) {
                    double vSetorAtual = valorPorSetor.getOrDefault(st.getId(), 0d);
                    double alvoSetor = vSubIdeal * st.getPercentualIdeal().doubleValue() / 100d;
                    setorDtos.add(new ComparativoResponseDTO.SetorComparativoDTO(
                            st.getId(),
                            st.getNome(),
                            st.getPercentualIdeal(),
                            calculator.percentual(vSetorAtual, vSubAtual),
                            calculator.moeda(alvoSetor),
                            calculator.moeda(vSetorAtual),
                            calculator.moeda(Math.max(0d, alvoSetor - vSetorAtual)),
                            calculator.moeda(Math.max(0d, vSetorAtual - alvoSetor)),
                            st.getTolerancia(),
                            st.getLimiteMaximo()));
                }
                subDtos.add(new ComparativoResponseDTO.SubclasseComparativoDTO(
                        sub.getId(),
                        sub.getNome(),
                        sub.getPercentualIdeal(),
                        calculator.percentual(vSubAtual, vAtual),
                        calculator.moeda(vSubIdeal),
                        calculator.moeda(vSubAtual),
                        calculator.moeda(Math.max(0d, vSubIdeal - vSubAtual)),
                        calculator.moeda(Math.max(0d, vSubAtual - vSubIdeal)),
                        sub.getTolerancia(),
                        sub.getLimiteMaximo(),
                        setorDtos));
            }

            List<ComparativoResponseDTO.AtivoComparativoDTO> ativosDtos = new ArrayList<>();
            Set<UUID> tickersComMeta = new LinkedHashSet<>();
            for (MetaAtivoModel meta : metasDaClasse) {
                if (meta.getAtivoCadastro() == null) {
                    continue;
                }
                UUID cadastroId = meta.getAtivoCadastro().getId();
                tickersComMeta.add(cadastroId);
                double vAtivoAtual = valorPorTicker.getOrDefault(cadastroId, 0d);
                double vAtivoIdeal = valorIdeal(meta.getPercentualIdeal(), total);
                ativosDtos.add(new ComparativoResponseDTO.AtivoComparativoDTO(
                        meta.getId(),
                        cadastroId,
                        nomePorTicker.getOrDefault(cadastroId, meta.getAtivoCadastro().getNome()),
                        (meta.getSubclasse() != null) ? meta.getSubclasse().getId() : null,
                        (meta.getSetor() != null) ? meta.getSetor().getId() : null,
                        meta.getPercentualIdeal(),
                        calculator.percentual(vAtivoAtual, total),
                        calculator.moeda(vAtivoIdeal),
                        calculator.moeda(vAtivoAtual),
                        calculator.moeda(Math.max(0d, vAtivoIdeal - vAtivoAtual)),
                        calculator.moeda(Math.max(0d, vAtivoAtual - vAtivoIdeal)),
                        meta.getTolerancia(),
                        meta.getLimiteMaximo(),
                        meta.getPrioridadeManual(),
                        true));
            }

            // Ativos que o usuário TEM nesta classe mas ainda não têm meta: o
            // comparativo é da carteira real, não só do que já foi planejado.
            for (UUID cadastroId : valorPorTicker.keySet()) {
                if (classePorTicker.get(cadastroId) != classeEnum || tickersComMeta.contains(cadastroId)) {
                    continue;
                }
                double vAtivoAtual = valorPorTicker.get(cadastroId);
                ativosDtos.add(new ComparativoResponseDTO.AtivoComparativoDTO(
                        null,
                        cadastroId,
                        nomePorTicker.get(cadastroId),
                        null,
                        null,
                        BigDecimal.ZERO.setScale(PercentualCalculator.ESCALA_PERCENTUAL),
                        calculator.percentual(vAtivoAtual, total),
                        calculator.moeda(0d),
                        calculator.moeda(vAtivoAtual),
                        calculator.moeda(0d),
                        calculator.moeda(vAtivoAtual),
                        null,
                        null,
                        0,
                        false));
            }
            ativosSemMeta += (int) ativosDtos.stream().filter(a -> !a.possui_meta()).count();

            classesDto.add(new ComparativoResponseDTO.ClasseComparativoDTO(
                    classeEnum,
                    pIdeal,
                    calculator.percentual(vAtual, total),
                    calculator.moeda(vIdeal),
                    calculator.moeda(vAtual),
                    calculator.moeda(Math.max(0d, vIdeal - vAtual)),
                    calculator.moeda(Math.max(0d, vAtual - vIdeal)),
                    (ideal != null) ? ideal.getTolerancia() : BigDecimal.ZERO,
                    (ideal != null) ? ideal.getLimiteMaximo() : null,
                    subDtos,
                    ativosDtos));
        }

        return new ComparativoResponseDTO(
                carteira.getId(),
                carteira.getMoeda(),
                calculator.moeda(total),
                soma,
                classesDto,
                montarAvisos(classes, subclasses, metas, soma, ativosSemMeta));
    }

    /* ══════════════════════════════════════════════════════════════════
       Ativos que o usuário JÁ TEM — base da tela de metas
       ══════════════════════════════════════════════════════════════════ */

    /**
     * Ativos que o usuário já tem nesta carteira, agregados por ticker, com o
     * percentual atual e a meta (quando já existe).
     *
     * É o que permite montar as metas a partir do que EXISTE: o usuário não
     * precisa recadastrar os ativos, e a tela deixa de ser um espaço paralelo
     * ao da carteira. Só entram posições vinculadas ao catálogo (a meta
     * individual é por ticker); posições sem vínculo são contadas em
     * `posicoes_sem_catalogo` para a interface avisar — elas continuam
     * participando do cálculo por CLASSE normalmente.
     */
    @Transactional(readOnly = true)
    public MeusAtivosResponseDTO meusAtivos(Long carteiraId) {
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(carteiraId);

        List<AtivoModel> posicoes = ativoRepository.findByCarteiraInvestimentoId(carteiraId);
        double total = posicoes.stream().mapToDouble(calculator::valorPosicao).sum();

        // Agrupa por ativo do catálogo quando vinculado; senão, pelo NOME da
        // posição (é o que permite mostrar a renda fixa e ativos que ficaram
        // sem vínculo, em vez de escondê-los da tela).
        Map<String, Acumulado> porGrupo = new LinkedHashMap<>();
        int semCatalogo = 0;
        for (AtivoModel posicao : posicoes) {
            UUID catalogoId = (posicao.getAtivoCadastro() != null) ? posicao.getAtivoCadastro().getId() : null;
            if (catalogoId == null) {
                semCatalogo++;
            }
            String chave = (catalogoId != null)
                    ? "cat:" + catalogoId
                    : "nome:" + normalizarNome(posicao.getNome());

            Acumulado acumulado = porGrupo.computeIfAbsent(chave, k -> new Acumulado(
                    catalogoId,
                    (posicao.getAtivoCadastro() != null) ? posicao.getAtivoCadastro().getNome() : posicao.getNome(),
                    (posicao.getCategoriaInvestimento() != null)
                            ? posicao.getCategoriaInvestimento()
                            : CategoriaInvestimento.OUTROS,
                    calculator.precoAtual(posicao)));
            acumulado.quantidade += (posicao.getQuantidade() != null) ? posicao.getQuantidade() : 0f;
            acumulado.valor += calculator.valorPosicao(posicao);
            acumulado.ativoIds.add(posicao.getId());
            // Renda fixa / caixinha: a subclasse vem da própria POSIÇÃO (V26).
            if (acumulado.subclasse == null && posicao.getSubclasse() != null) {
                acumulado.subclasse = posicao.getSubclasse();
            }
            if (acumulado.setor == null && posicao.getSetor() != null) {
                acumulado.setor = posicao.getSetor();
            }
        }

        Map<UUID, MetaAtivoModel> metasPorTicker = new LinkedHashMap<>();
        for (MetaAtivoModel meta : metaAtivoRepository
                .findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId)) {
            if (meta.getAtivoCadastro() != null && meta.getAtivoCadastro().getId() != null) {
                metasPorTicker.putIfAbsent(meta.getAtivoCadastro().getId(), meta);
            }
        }

        List<MeusAtivosResponseDTO.MeuAtivoDTO> ativos = porGrupo.values().stream()
                .map(acumulado -> {
                    MetaAtivoModel meta = (acumulado.catalogoId != null)
                            ? metasPorTicker.get(acumulado.catalogoId)
                            : null;
                    // Ativo sem vínculo: se existir um ticker com o MESMO nome no
                    // catálogo, sugerimos vincular (um clique resolve).
                    AtivoCadastroModel sugestao = (acumulado.catalogoId == null)
                            ? ativoCadastroRepository.findByNomeIgnoreCase(acumulado.nome).orElse(null)
                            : null;

                    // A subclasse da LINHA: a da meta (ativos com ticker) ou a da
                    // própria posição (renda fixa sem ticker, atribuída na V26).
                    CarteiraIdealSubclasseModel subclasseLinha =
                            (meta != null && meta.getSubclasse() != null) ? meta.getSubclasse() : acumulado.subclasse;
                    // O setor segue a mesma regra (V28).
                    CarteiraIdealSetorModel setorLinha =
                            (meta != null && meta.getSetor() != null) ? meta.getSetor() : acumulado.setor;

                    return new MeusAtivosResponseDTO.MeuAtivoDTO(
                            acumulado.catalogoId,
                            acumulado.catalogoId != null,
                            acumulado.nome,
                            List.copyOf(acumulado.ativoIds),
                            (sugestao != null) ? sugestao.getId() : null,
                            (sugestao != null) ? sugestao.getNome() : null,
                            acumulado.classe,
                            BigDecimal.valueOf(acumulado.quantidade).setScale(8, RoundingMode.HALF_UP)
                                    .stripTrailingZeros(),
                            (acumulado.precoAtual != null) ? BigDecimal.valueOf(acumulado.precoAtual) : null,
                            calculator.moeda(acumulado.valor),
                            calculator.percentual(acumulado.valor, total),
                            (meta != null) ? meta.getId() : null,
                            (meta != null) ? meta.getPercentualIdeal() : null,
                            (meta != null) ? meta.getTolerancia() : null,
                            (meta != null) ? meta.getLimiteMaximo() : null,
                            (meta != null) ? meta.getPrioridadeManual() : null,
                            (subclasseLinha != null) ? subclasseLinha.getId() : null,
                            (subclasseLinha != null) ? subclasseLinha.getNome() : null,
                            (setorLinha != null) ? setorLinha.getId() : null,
                            (setorLinha != null) ? setorLinha.getNome() : null);
                })
                .sorted(Comparator.comparing(MeusAtivosResponseDTO.MeuAtivoDTO::vinculado).reversed()
                        .thenComparing(MeusAtivosResponseDTO.MeuAtivoDTO::valor_atual, Comparator.reverseOrder()))
                .toList();

        return new MeusAtivosResponseDTO(carteira.getId(), carteira.getMoeda(),
                calculator.moeda(total), semCatalogo, ativos);
    }

    /** Nome normalizado (agrupa posições sem vínculo que têm o mesmo nome). */
    private String normalizarNome(String nome) {
        return (nome == null) ? "" : nome.trim().toLowerCase();
    }

    /**
     * Subclasse de uma POSIÇÃO: a atribuição explícita na posição manda; sem
     * ela, vale a subclasse da meta do ticker (quando existe meta com subclasse).
     * Devolve null quando a posição não está em nenhuma subclasse.
     */
    private Long subclasseDaPosicao(AtivoModel posicao, List<MetaAtivoModel> metas) {
        if (posicao.getSubclasse() != null) {
            return posicao.getSubclasse().getId();
        }
        if (posicao.getAtivoCadastro() == null) {
            return null;
        }
        UUID ticker = posicao.getAtivoCadastro().getId();
        for (MetaAtivoModel meta : metas) {
            if (meta.getAtivoCadastro() != null && meta.getAtivoCadastro().getId().equals(ticker)
                    && meta.getSubclasse() != null) {
                return meta.getSubclasse().getId();
            }
        }
        return null;
    }

    /**
     * SETOR de uma POSIÇÃO: atribuição explícita na posição (renda fixa) ou o
     * setor da meta do ticker. null quando a posição não está em nenhum setor.
     */
    private Long setorDaPosicao(AtivoModel posicao, List<MetaAtivoModel> metas) {
        if (posicao.getSetor() != null) {
            return posicao.getSetor().getId();
        }
        if (posicao.getAtivoCadastro() == null) {
            return null;
        }
        UUID ticker = posicao.getAtivoCadastro().getId();
        for (MetaAtivoModel meta : metas) {
            if (meta.getAtivoCadastro() != null && meta.getAtivoCadastro().getId().equals(ticker)
                    && meta.getSetor() != null) {
                return meta.getSetor().getId();
            }
        }
        return null;
    }

    /** Acumulador por ativo (o usuário pode ter mais de uma posição do mesmo ativo). */
    private static final class Acumulado {
        private final UUID catalogoId;
        private final String nome;
        private final CategoriaInvestimento classe;
        private final Float precoAtual;
        private final List<Long> ativoIds = new ArrayList<>();
        private CarteiraIdealSubclasseModel subclasse;
        private CarteiraIdealSetorModel setor;
        private float quantidade;
        private double valor;

        private Acumulado(UUID catalogoId, String nome, CategoriaInvestimento classe, Float precoAtual) {
            this.catalogoId = catalogoId;
            this.nome = nome;
            this.classe = classe;
            this.precoAtual = precoAtual;
        }
    }

    /** Resumo enxuto por classe, para o widget do Dashboard (Módulo 10). */
    @Transactional(readOnly = true)
    public ResumoIdealDTO resumo(Long carteiraId) {
        ComparativoResponseDTO comparativo = comparativo(carteiraId);
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(carteiraId);
        long totalMetas = metaAtivoRepository
                .findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId).size();

        List<ResumoIdealDTO.ClasseResumoDTO> classes = comparativo.classes().stream()
                .map(c -> new ResumoIdealDTO.ClasseResumoDTO(
                        c.classe(),
                        c.percentual_ideal(),
                        c.percentual_atual(),
                        c.valor_ideal(),
                        c.valor_atual(),
                        c.deficit(),
                        c.excesso()))
                .toList();

        return new ResumoIdealDTO(
                carteira.getId(),
                comparativo.moeda(),
                comparativo.valor_total(),
                comparativo.soma_percentuais_ideal(),
                classes.size(),
                (int) totalMetas,
                classes,
                comparativo.avisos());
    }

    /* ══════════════════════════════════════════════════════════════════
       Validações
       ══════════════════════════════════════════════════════════════════ */

    private void validarClasses(List<CarteiraIdealRequestDTO.ClasseIdealRequestDTO> classes) {
        Set<CategoriaInvestimento> vistas = new LinkedHashSet<>();
        for (CarteiraIdealRequestDTO.ClasseIdealRequestDTO c : classes) {
            if (!vistas.add(c.classe())) {
                throw new BusinessRuleException("A classe " + c.classe()
                        + " aparece mais de uma vez na Carteira Ideal.");
            }
            Set<String> nomesSubclasses = new LinkedHashSet<>();
            for (CarteiraIdealRequestDTO.SubclasseIdealRequestDTO s : subclassesDe(c)) {
                if (s.nome() == null || s.nome().isBlank()) {
                    throw new BusinessRuleException("Toda subclasse precisa de um nome.");
                }
                if (!nomesSubclasses.add(s.nome().trim().toLowerCase())) {
                    throw new BusinessRuleException("A subclasse \"" + s.nome().trim()
                            + "\" está repetida na classe " + c.classe() + ".");
                }
            }
        }

        if (classes.isEmpty()) {
            return; // limpar a configuração é permitido
        }

        BigDecimal soma = classes.stream()
                .map(c -> calculator.percentualNormalizado(c.percentual_ideal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (soma.subtract(CEM).abs().compareTo(TOLERANCIA) > 0) {
            throw new BusinessRuleException("A soma das classes da Carteira Ideal deve ser 100%. "
                    + "A soma informada é " + formatar(soma) + "%.");
        }
    }

    private Map<UUID, AtivoCadastroModel> carregarCatalogo(
            List<CarteiraIdealRequestDTO.MetaIdealRequestDTO> metas) {

        Map<UUID, AtivoCadastroModel> catalogo = new LinkedHashMap<>();
        Set<UUID> vistas = new LinkedHashSet<>();
        for (CarteiraIdealRequestDTO.MetaIdealRequestDTO m : metas) {
            UUID id = m.ativo_cadastro_id();
            if (!vistas.add(id)) {
                throw new BusinessRuleException("O ativo " + id
                        + " aparece em mais de uma meta da mesma carteira.");
            }
            AtivoCadastroModel ativo = ativoCadastroRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ativo do catálogo com ID " + id + " não encontrado"));
            catalogo.put(id, ativo);
        }
        return catalogo;
    }

    /* ══════════════════════════════════════════════════════════════════
       Helpers
       ══════════════════════════════════════════════════════════════════ */

    /** Subclasses do payload (nunca null). */
    private List<CarteiraIdealRequestDTO.SubclasseIdealRequestDTO> subclassesDe(
            CarteiraIdealRequestDTO.ClasseIdealRequestDTO c) {
        return (c.subclasses() == null) ? List.of() : c.subclasses();
    }

    private List<CarteiraIdealSubclasseModel> subclassesDasClasses(List<CarteiraIdealClasseModel> classes) {
        List<Long> ids = classes.stream().map(CarteiraIdealClasseModel::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return subclasseRepository.findByClasseIdInOrderByOrdemAscIdAsc(ids);
    }

    /** Setores das subclasses informadas (nível opcional da hierarquia). */
    private List<CarteiraIdealSetorModel> setoresDasSubclasses(List<CarteiraIdealSubclasseModel> subclasses) {
        List<Long> ids = subclasses.stream().map(CarteiraIdealSubclasseModel::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return setorRepository.findBySubclasseIdInOrderByOrdemAscIdAsc(ids);
    }

    private BigDecimal somarPercentuaisClasses(List<CarteiraIdealClasseModel> classes) {
        return classes.stream()
                .map(CarteiraIdealClasseModel::getPercentualIdeal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(PercentualCalculator.ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
    }

    private double valorIdeal(BigDecimal percentualIdeal, double total) {
        if (percentualIdeal == null) {
            return 0d;
        }
        return percentualIdeal.doubleValue() / 100d * total;
    }

    /** Tolerância nunca negativa (null vira 0 = sem faixa de equilíbrio extra). */
    private BigDecimal toleranciaDe(BigDecimal tolerancia) {
        if (tolerancia == null || tolerancia.signum() < 0) {
            return BigDecimal.ZERO.setScale(PercentualCalculator.ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
        }
        return calculator.percentualNormalizado(tolerancia);
    }

    /** Classificação de uma posição (por NOME) — sobrevive ao replace-all do save. */
    private record ClassificacaoPosicao(Long ativoId, String subclasse, String setor) {}

    private String chaveSubclasse(CategoriaInvestimento classe, String nome) {
        return classe.name() + "|" + nome.trim().toLowerCase();
    }

    /** Chave do setor: classe + subclasse + nome (o nome do setor só é único dentro da subclasse). */
    private String chaveSetor(CategoriaInvestimento classe, String subclasse, String setor) {
        return classe.name() + "|" + subclasse.trim().toLowerCase() + "|" + setor.trim().toLowerCase();
    }

    /** Setores declarados em uma subclasse (nunca null). */
    private List<CarteiraIdealRequestDTO.SetorIdealRequestDTO> setoresDe(
            CarteiraIdealRequestDTO.SubclasseIdealRequestDTO s) {
        return (s.setores() == null) ? List.of() : s.setores();
    }

    private String formatar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private List<CarteiraIdealResponseDTO.ClasseIdealResponseDTO> montarClassesResposta(
            List<CarteiraIdealClasseModel> classes,
            List<CarteiraIdealSubclasseModel> subclasses) {

        List<CarteiraIdealSetorModel> setores = setoresDasSubclasses(subclasses);
        Map<Long, List<CarteiraIdealSetorModel>> setoresPorSubclasse = setores.stream()
                .collect(Collectors.groupingBy(s -> s.getSubclasse().getId()));
        Map<Long, List<CarteiraIdealSubclasseModel>> porClasse = subclasses.stream()
                .collect(Collectors.groupingBy(s -> s.getClasse().getId()));

        return classes.stream()
                .map(c -> new CarteiraIdealResponseDTO.ClasseIdealResponseDTO(
                        c.getId(),
                        c.getClasse(),
                        c.getPercentualIdeal(),
                        c.getTolerancia(),
                        c.getLimiteMaximo(),
                        c.getOrdem(),
                        porClasse.getOrDefault(c.getId(), List.of()).stream()
                                .map(s -> new CarteiraIdealResponseDTO.SubclasseIdealResponseDTO(
                                        s.getId(), s.getNome(), s.getPercentualIdeal(),
                                        s.getTolerancia(), s.getLimiteMaximo(), s.getOrdem(),
                                        setoresPorSubclasse.getOrDefault(s.getId(), List.of()).stream()
                                                .map(st -> new CarteiraIdealResponseDTO.SetorIdealResponseDTO(
                                                        st.getId(), st.getNome(), st.getPercentualIdeal(),
                                                        st.getTolerancia(), st.getLimiteMaximo(), st.getOrdem()))
                                                .toList()))
                                .toList()))
                .toList();
    }

    private List<CarteiraIdealResponseDTO.MetaIdealResponseDTO> montarMetasResposta(List<MetaAtivoModel> metas) {
        return metas.stream()
                .map(m -> new CarteiraIdealResponseDTO.MetaIdealResponseDTO(
                        m.getId(),
                        (m.getAtivoCadastro() != null) ? m.getAtivoCadastro().getId() : null,
                        (m.getAtivoCadastro() != null) ? m.getAtivoCadastro().getNome() : null,
                        m.getClasse(),
                        (m.getSubclasse() != null) ? m.getSubclasse().getId() : null,
                        (m.getSubclasse() != null) ? m.getSubclasse().getNome() : null,
                        (m.getSetor() != null) ? m.getSetor().getId() : null,
                        (m.getSetor() != null) ? m.getSetor().getNome() : null,
                        m.getPercentualIdeal(),
                        m.getTolerancia(),
                        m.getLimiteMaximo(),
                        m.getPrioridadeManual(),
                        m.getOrdem()))
                .toList();
    }

    /**
     * Avisos são informativos: representam configurações inconsistentes que o
     * usuário pode querer manter temporariamente (ex.: carteira em transição).
     */
    private List<String> montarAvisos(List<CarteiraIdealClasseModel> classes,
                                      List<CarteiraIdealSubclasseModel> subclasses,
                                      List<MetaAtivoModel> metas,
                                      BigDecimal somaClasses,
                                      int ativosSemMeta) {

        List<String> avisos = new ArrayList<>();

        if (!classes.isEmpty() && somaClasses.subtract(CEM).abs().compareTo(TOLERANCIA) > 0) {
            avisos.add("A soma das classes está em " + formatar(somaClasses)
                    + "% (o esperado é 100%).");
        }

        Map<Long, List<CarteiraIdealSubclasseModel>> subsPorClasse = subclasses.stream()
                .collect(Collectors.groupingBy(s -> s.getClasse().getId()));
        Map<CategoriaInvestimento, List<MetaAtivoModel>> metasPorClasse = metas.stream()
                .collect(Collectors.groupingBy(MetaAtivoModel::getClasse));
        Map<CategoriaInvestimento, CarteiraIdealClasseModel> idealPorClasse = classes.stream()
                .collect(Collectors.toMap(CarteiraIdealClasseModel::getClasse, Function.identity(),
                        (a, b) -> a));

        for (CarteiraIdealClasseModel classe : classes) {
            BigDecimal somaSub = subsPorClasse.getOrDefault(classe.getId(), List.of()).stream()
                    .map(CarteiraIdealSubclasseModel::getPercentualIdeal)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // O percentual da SUBCLASSE é uma fatia da CLASSE (a soma fecha em
            // 100% da classe, não em 100% da carteira). Comparar com o percentual
            // da classe acusava erro em TODA classe com alvo menor que 100%.
            if (!subsPorClasse.getOrDefault(classe.getId(), List.of()).isEmpty()
                    && somaSub.subtract(CEM).abs().compareTo(TOLERANCIA) > 0) {
                avisos.add("As subclasses de " + classe.getClasse() + " somam " + formatar(somaSub)
                        + "% da classe (o esperado é 100% dela).");
            }
        }

        for (Map.Entry<CategoriaInvestimento, List<MetaAtivoModel>> entry : metasPorClasse.entrySet()) {
            BigDecimal somaMetas = entry.getValue().stream()
                    .map(MetaAtivoModel::getPercentualIdeal)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            CarteiraIdealClasseModel classe = idealPorClasse.get(entry.getKey());
            if (classe == null) {
                avisos.add("Existem metas de ativos na classe " + entry.getKey()
                        + ", mas essa classe não está na Carteira Ideal.");
            } else if (somaMetas.compareTo(classe.getPercentualIdeal()) > 0) {
                avisos.add("As metas de " + entry.getKey() + " somam " + formatar(somaMetas)
                        + "%, acima dos " + formatar(classe.getPercentualIdeal()) + "% da classe.");
            }
        }

        if (ativosSemMeta > 0) {
            avisos.add(ativosSemMeta + " ativo(s) da sua carteira ainda não têm meta individual: "
                    + "eles entram como excesso da classe até você definir um alvo.");
        }

        return avisos;
    }

    /** Classe (categoria) de cada ticker, conforme as posições atuais. */
    private Map<UUID, CategoriaInvestimento> classePorTicker(
            List<PercentualCalculator.PosicaoSnapshot> posicoes) {
        Map<UUID, CategoriaInvestimento> mapa = new HashMap<>();
        for (PercentualCalculator.PosicaoSnapshot posicao : posicoes) {
            if (posicao.ativoCadastroId() == null) {
                continue;
            }
            CategoriaInvestimento classe = (posicao.classe() != null)
                    ? posicao.classe()
                    : CategoriaInvestimento.OUTROS;
            mapa.putIfAbsent(posicao.ativoCadastroId(), classe);
        }
        return mapa;
    }
}
