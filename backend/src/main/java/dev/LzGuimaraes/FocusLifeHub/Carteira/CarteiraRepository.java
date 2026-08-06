package dev.LzGuimaraes.FocusLifeHub.Carteira;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CarteiraRepository<T extends CarteiraModel> extends JpaRepository<T, Long> {
    Page<T> findByUserId(Long userId, Pageable pageable);
}
