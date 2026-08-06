package dev.LzGuimaraes.FocusLifeHub.Contas;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

import org.springframework.data.domain.Page;

@Repository
public interface ContasRepository extends JpaRepository<ContasModel, Long> {
    Page<ContasModel> findByCarteiraInvestimento_UserId(Long userId, Pageable pageable);
    Page<ContasModel> findByCarteiraDividas_UserId(Long userId, Pageable pageable);
    Page<ContasModel> findByCarteiraInvestimento_UserIdOrCarteiraDividas_UserId(Long userId, Long userId2, Pageable pageable);
    List<ContasModel> findByCarteiraInvestimentoId(Long id);
    List<ContasModel> findByCarteiraDividasId(Long id);
    List<ContasModel> findByCarteiraInvestimento_UserIdAndPagoFalseAndDataVencimento(Long userId, LocalDate dataVencimento);
    List<ContasModel> findByCarteiraDividas_UserIdAndPagoFalseAndDataVencimento(Long userId, LocalDate dataVencimento);
    List<ContasModel> findByPagoFalseAndDataVencimento(LocalDate dataVencimento);
}