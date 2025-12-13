package com.focusfortress.service;

import com.focusfortress.model.Role;
import com.focusfortress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupUnverifiedUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        userRepository.deleteByRoleAndCreatedAtBefore(Role.UNVERIFIED, cutoff);
    }
}