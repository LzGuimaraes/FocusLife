package dev.LzGuimaraes.FocusLifeHub.Carteira;

import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Contas.ContasRepository;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

@Service
public class CarteiraInvestimentoService extends AbstractCarteiraService<CarteiraInvestimentoModel> {

    public CarteiraInvestimentoService(
            CarteiraInvestimentoRepository repository,
            UserRepository userRepository,
            ContasRepository contasRepository) {
        super(repository, userRepository, contasRepository);
    }

    @Override
    protected CarteiraInvestimentoModel createEmpty() {
        return new CarteiraInvestimentoModel();
    }
}
