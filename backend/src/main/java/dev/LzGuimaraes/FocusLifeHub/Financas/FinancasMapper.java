package dev.LzGuimaraes.FocusLifeHub.Financas;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Financas.dto.FinancasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Financas.dto.FinancasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;

@Component
public class FinancasMapper {

    public FinancasModel toModel(FinancasRequestDTO dto, UserModel user) {
        if (dto == null) {
            return null;
        }

        FinancasModel financa = new FinancasModel();
        financa.setNome(dto.nome());
        financa.setMoeda(dto.moeda().toUpperCase());
        financa.setTipoCarteira(dto.tipoCarteira());
        financa.setUser(user);

        return financa;
    }

    public FinancasResponseDTO toResponse(FinancasModel financa) {
        if (financa == null) {
            return null;
        }

        Long userId = (financa.getUser() != null) ? financa.getUser().getId() : null;

        return new FinancasResponseDTO(
            financa.getId(),
            financa.getNome(),
            financa.getMoeda(),
            financa.getTipoCarteira(),
            userId
        );
    }
}