package com.focusfortress.repository;

import com.focusfortress.model.InterestCategory;
import com.focusfortress.model.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    List<UserInterest> findByUserIdOrderBySelectedAtDesc(Long userId);

    Optional<UserInterest> findByUserIdAndInterest(Long userId, InterestCategory interest);

    boolean existsByUserIdAndInterest(Long userId, InterestCategory interest);

    @Query("SELECT ui.interest FROM UserInterest ui WHERE ui.user.id = :userId")
    Set<InterestCategory> findInterestCategoriesByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndInterest(Long userId, InterestCategory interest);
}