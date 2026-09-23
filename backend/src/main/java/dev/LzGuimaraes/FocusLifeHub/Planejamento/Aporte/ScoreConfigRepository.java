package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoreConfigRepository extends JpaRepository<ScoreConfigModel, Long> {

    Optional<ScoreConfigModel> findByUserId(Long userId);
}
