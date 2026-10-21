package com.focusfortress.repository;

import com.focusfortress.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByOwnerId(Long ownerId);
    Optional<Category> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByOwnerIdAndName(Long ownerId, String name);

    // Shared interest catalog (categories without an owner). NULL owners don't collide in the
    // unique index, so concurrent registrations may create a duplicate - always use the oldest one.
    Optional<Category> findFirstByNameAndOwnerIsNullOrderByIdAsc(String name);
}
