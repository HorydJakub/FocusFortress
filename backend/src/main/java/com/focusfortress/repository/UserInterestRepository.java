package com.focusfortress.repository;

import com.focusfortress.model.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    List<UserInterest> findByUserIdOrderBySelectedAtDesc(Long userId);

    boolean existsByUserIdAndSubcategoryId(Long userId, Long subcategoryId);

    void deleteByUserIdAndSubcategoryId(Long userId, Long subcategoryId);

    @Query("SELECT COUNT(ui) FROM UserInterest ui WHERE ui.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}