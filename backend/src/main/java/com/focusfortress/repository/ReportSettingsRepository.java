package com.focusfortress.repository;

import com.focusfortress.model.ReportSettings;
import com.focusfortress.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportSettingsRepository extends JpaRepository<ReportSettings, Long> {
    Optional<ReportSettings> findByUser(User user);
    Optional<ReportSettings> findByUserId(Long userId);
}

