package dev.LzGuimaraes.FocusLifeHub.Carteira;

import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.PlanejamentoCleanup;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

@Service
public class CarteiraDividasService extends AbstractCarteiraService<CarteiraDividasModel> {

    public CarteiraDividasService(
            CarteiraDividasRepository repository,
            UserRepository userRepository,
            AtivoRepository ativoRepository,
            DespesaRepository despesaRepository,
            PlanejamentoCleanup planejamentoCleanup) {
        super(repository, userRepository, ativoRepository, despesaRepository, planejamentoCleanup);
    }

    @Override
    protected CarteiraDividasModel createEmpty() {
        return new CarteiraDividasModel();
    }
}
