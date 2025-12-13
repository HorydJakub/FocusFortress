package com.focusfortress.repository;

import com.focusfortress.model.Role;
import com.focusfortress.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationToken(String token);
    boolean existsByEmail(String email);
}
