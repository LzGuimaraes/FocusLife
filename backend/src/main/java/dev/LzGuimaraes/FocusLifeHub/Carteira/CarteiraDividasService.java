package dev.LzGuimaraes.FocusLifeHub.Carteira;

import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Contas.ContasRepository;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

@Service
public class CarteiraDividasService extends AbstractCarteiraService<CarteiraDividasModel> {

    public CarteiraDividasService(
            CarteiraDividasRepository repository,
            UserRepository userRepository,
            ContasRepository contasRepository) {
        super(repository, userRepository, contasRepository);
    }

    @Override
    protected CarteiraDividasModel createEmpty() {
        return new CarteiraDividasModel();
    }
}
