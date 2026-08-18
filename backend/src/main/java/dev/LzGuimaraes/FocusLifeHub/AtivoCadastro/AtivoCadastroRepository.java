package dev.LzGuimaraes.FocusLifeHub.AtivoCadastro;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtivoCadastroRepository extends JpaRepository<AtivoCadastroModel, UUID> {
    List<AtivoCadastroModel> findAllByOrderByNomeAsc();
    List<AtivoCadastroModel> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
    java.util.Optional<AtivoCadastroModel> findByNomeIgnoreCase(String nome);
}
