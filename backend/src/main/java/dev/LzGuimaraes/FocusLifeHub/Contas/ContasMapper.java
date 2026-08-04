package dev.LzGuimaraes.FocusLifeHub.Contas;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Financas.FinancasModel;

@Component
public class ContasMapper {

    public ContasModel toModel(ContasRequestDTO dto, FinancasModel financas) {
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
        ativo.setFinancas(financas);

        return ativo;
    }

    public ContasResponseDTO toResponse(ContasModel ativo) {
        if (ativo == null) {
            return null;
        }

        Long financasId = (ativo.getFinancas() != null) ? ativo.getFinancas().getId() : null;

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
            financasId
        );
    }
}