package com.focusfortress.service;

import com.focusfortress.dto.SubcategoryDTO;
import com.focusfortress.exception.NotFoundException;
import com.focusfortress.model.Category;
import com.focusfortress.model.Subcategory;
import com.focusfortress.repository.CategoryRepository;
import com.focusfortress.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<SubcategoryDTO> getSubcategoriesForCategory(Long categoryId) {
        return subcategoryRepository.findByCategoryId(categoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubcategoryDTO getSubcategoryById(Long id) {
        Subcategory subcategory = subcategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subcategory not found"));
        return convertToDTO(subcategory);
    }

    public SubcategoryDTO createSubcategory(SubcategoryDTO subcategoryDTO) {
        Category category = categoryRepository.findById(subcategoryDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (subcategoryRepository.existsByNameAndCategoryId(subcategoryDTO.getName(), subcategoryDTO.getCategoryId())) {
            throw new IllegalArgumentException("Subcategory with this name already exists in this category");
        }

        Subcategory subcategory = Subcategory.builder()
                .name(subcategoryDTO.getName())
                .icon(subcategoryDTO.getIcon())
                .category(category)
                .build();

        Subcategory saved = subcategoryRepository.save(subcategory);
        return convertToDTO(saved);
    }

    public SubcategoryDTO updateSubcategory(Long id, SubcategoryDTO subcategoryDTO) {
        Subcategory subcategory = subcategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subcategory not found"));

        if (subcategoryDTO.getName() != null) {
            if (!subcategoryDTO.getName().equals(subcategory.getName()) &&
                    subcategoryRepository.existsByNameAndCategoryId(subcategoryDTO.getName(), subcategory.getCategory().getId())) {
                throw new IllegalArgumentException("Subcategory with this name already exists in this category");
            }
            subcategory.setName(subcategoryDTO.getName());
        }

        if (subcategoryDTO.getIcon() != null) {
            subcategory.setIcon(subcategoryDTO.getIcon());
        }

        if (subcategoryDTO.getCategoryId() != null &&
                !subcategoryDTO.getCategoryId().equals(subcategory.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(subcategoryDTO.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));

            if (subcategoryRepository.existsByNameAndCategoryId(subcategory.getName(), newCategory.getId())) {
                throw new IllegalArgumentException("Subcategory with this name already exists in the target category");
            }

            subcategory.setCategory(newCategory);
        }

        Subcategory updated = subcategoryRepository.save(subcategory);
        return convertToDTO(updated);
    }

    public void deleteSubcategory(Long id) {
        Subcategory subcategory = subcategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subcategory not found"));
        subcategoryRepository.delete(subcategory);
    }

    private SubcategoryDTO convertToDTO(Subcategory subcategory) {
        return new SubcategoryDTO(
                subcategory.getId(),
                subcategory.getName(),
                subcategory.getIcon(),
                subcategory.getCategory().getId()
        );
    }
}