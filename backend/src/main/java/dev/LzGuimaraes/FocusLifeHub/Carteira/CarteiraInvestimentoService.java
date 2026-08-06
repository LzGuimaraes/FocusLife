package dev.LzGuimaraes.FocusLifeHub.Carteira;

import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaRepository;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

@Service
public class CarteiraInvestimentoService extends AbstractCarteiraService<CarteiraInvestimentoModel> {

    public CarteiraInvestimentoService(
            CarteiraInvestimentoRepository repository,
            UserRepository userRepository,
            AtivoRepository ativoRepository,
            DespesaRepository despesaRepository) {
        super(repository, userRepository, ativoRepository, despesaRepository);
    }

    @Override
    protected CarteiraInvestimentoModel createEmpty() {
        return new CarteiraInvestimentoModel();
    }
}
