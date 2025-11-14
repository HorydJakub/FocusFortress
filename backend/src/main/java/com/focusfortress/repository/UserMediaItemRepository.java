package com.focusfortress.repository;

import com.focusfortress.model.MediaStatus;
import com.focusfortress.model.UserMediaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMediaItemRepository extends JpaRepository<UserMediaItem, Long> {

    List<UserMediaItem> findByUserEmailOrderByAddedAtDesc(String userEmail);

    List<UserMediaItem> findByUserEmailAndStatus(String userEmail, MediaStatus status);

    Optional<UserMediaItem> findByIdAndUserEmail(Long id, String userEmail);

    boolean existsByUserEmailAndVideoId(String userEmail, String videoId);
}