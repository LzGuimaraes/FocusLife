package dev.LzGuimaraes.FocusLifeHub.Contas;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraModel;
import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContasResponseDTO;

@Component
public class ContasMapper {

    public ContasModel toModel(ContasRequestDTO dto, CarteiraModel carteira) {
        if (dto == null) {
            return null;
        }

        ContasModel ativo = new ContasModel();
        ativo.setNome(dto.nome());
        ativo.setCategoria(dto.categoria());
        ativo.setCategoriaInvestimento(dto.categoriaInvestimento());
        ativo.setQuantidade(dto.quantidade());
        ativo.setValorUnitario(dto.valorUnitario());
        ativo.setPrecoAtual(dto.precoAtual());
        ativo.setInstituicao(dto.instituicao());
        ativo.setDataAplicacao(dto.dataAplicacao());
        ativo.setVencimento(dto.vencimento());
        ativo.setDataVencimento(dto.dataVencimento());
        ativo.setRentabilidade(dto.rentabilidade());
        ativo.setPago(dto.pago());

        if (carteira instanceof CarteiraInvestimentoModel) {
            ativo.setCarteiraInvestimento((CarteiraInvestimentoModel) carteira);
        } else if (carteira instanceof CarteiraDividasModel) {
            ativo.setCarteiraDividas((CarteiraDividasModel) carteira);
        }

        return ativo;
    }

    public ContasResponseDTO toResponse(ContasModel ativo) {
        if (ativo == null) {
            return null;
        }

        Long carteiraInvestimentoId = (ativo.getCarteiraInvestimento() != null) ? ativo.getCarteiraInvestimento().getId() : null;
        Long carteiraDividasId = (ativo.getCarteiraDividas() != null) ? ativo.getCarteiraDividas().getId() : null;

        return new ContasResponseDTO(
            ativo.getId(),
            ativo.getNome(),
            ativo.getCategoria(),
            ativo.getCategoriaInvestimento(),
            ativo.getQuantidade(),
            ativo.getValorUnitario(),
            ativo.getPrecoAtual(),
            ativo.getSaldo(),
            ativo.getInstituicao(),
            ativo.getDataAplicacao(),
            ativo.getVencimento(),
            ativo.getDataVencimento(),
            ativo.getRentabilidade(),
            ativo.getPago(),
            carteiraInvestimentoId,
            carteiraDividasId
        );
    }
}