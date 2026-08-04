package dev.LzGuimaraes.FocusLifeHub.Contas;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

import org.springframework.data.domain.Page;

@Repository
public interface ContasRepository extends JpaRepository<ContasModel, Long> {
    Page<ContasModel> findByFinancas_UserId(Long userId, Pageable pageable);
    List<ContasModel> findByFinancasId(Long financasId);
    List<ContasModel> findByFinancas_UserIdAndPagoFalseAndDataVencimento(Long userId, LocalDate dataVencimento);
    List<ContasModel> findByPagoFalseAndDataVencimento(LocalDate dataVencimento);
}