package com.focusfortress.repository;

import com.focusfortress.model.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByCategoryId(Long categoryId);
    boolean existsByNameAndCategoryId(String name, Long categoryId);
    Optional<Subcategory> findByNameAndCategoryId(String name, Long categoryId);
    Optional<Subcategory> findByIdAndCategoryOwnerId(Long id, Long ownerId);

    // Shared interest catalog (subcategories of categories without an owner)
    Optional<Subcategory> findByNameAndCategoryOwnerIsNull(String name);
}