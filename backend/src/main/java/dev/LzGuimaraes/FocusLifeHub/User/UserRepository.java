package dev.LzGuimaraes.FocusLifeHub.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
    Optional<UserDetails> findUserByEmail(String username);
    boolean existsByEmail(String email);
}
