package com.focusfortress.repository;

import com.focusfortress.model.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    List<Subcategory> findByCategoryId(Long categoryId);
    boolean existsByNameAndCategoryId(String name, Long categoryId);
}