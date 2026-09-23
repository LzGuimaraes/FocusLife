package dev.LzGuimaraes.FocusLifeHub.Planejamento.comum;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealClasseRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.MetaAtivoRepository;

/**
 * Remove os dados de Planejamento de uma carteira de investimento que está
 * sendo excluída, evitando violação de FK (as tabelas do módulo referenciam a
 * carteira). Chamado por {@code AbstractCarteiraService.delete} antes de
 * apagar a carteira — mantém o módulo Carteira sem depender das entidades de
 * Planejamento.
 *
 * A ordem importa: metas (filhas da subclasse) antes das classes.
 */
@Component
public class PlanejamentoCleanup {

    private final MetaAtivoRepository metaAtivoRepository;
    private final CarteiraIdealClasseRepository classeRepository;

    public PlanejamentoCleanup(MetaAtivoRepository metaAtivoRepository,
                               CarteiraIdealClasseRepository classeRepository) {
        this.metaAtivoRepository = metaAtivoRepository;
        this.classeRepository = classeRepository;
    }

    @Transactional
    public void limparCarteira(Long carteiraId) {
        metaAtivoRepository.deleteByCarteiraInvestimentoId(carteiraId);
        metaAtivoRepository.flush();
        classeRepository.deleteByCarteiraInvestimentoId(carteiraId);
        classeRepository.flush();
    }
}
