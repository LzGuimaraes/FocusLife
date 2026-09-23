package dev.LzGuimaraes.FocusLifeHub.Planejamento.comum;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;

/**
 * Resolve a carteira de investimento do usuário autenticado em um único lugar.
 * Toda operação de Planejamento passa por aqui, garantindo que nenhum dado de
 * outro usuário seja acessível (404 em vez de 403 — não revela existência).
 */
@Component
public class CarteiraLookup {

    private final CarteiraInvestimentoRepository carteiraRepository;
    private final ContextoUsuario contextoUsuario;

    public CarteiraLookup(CarteiraInvestimentoRepository carteiraRepository,
                          ContextoUsuario contextoUsuario) {
        this.carteiraRepository = carteiraRepository;
        this.contextoUsuario = contextoUsuario;
    }

    /** Carteira do usuário autenticado ou ResourceNotFoundException (404). */
    public CarteiraInvestimentoModel exigirCarteiraDoUsuario(Long carteiraId) {
        Long userId = contextoUsuario.id();
        CarteiraInvestimentoModel carteira = carteiraRepository.findById(carteiraId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carteira de Investimento com ID " + carteiraId + " não encontrada"));
        if (carteira.getUser() == null || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException(
                    "Carteira de Investimento com ID " + carteiraId + " não encontrada");
        }
        return carteira;
    }
}
