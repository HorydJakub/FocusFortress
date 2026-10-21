package com.focusfortress.service;

import com.focusfortress.dto.SubcategoryDTO;
import com.focusfortress.exception.NotFoundException;
import com.focusfortress.model.Category;
import com.focusfortress.model.Subcategory;
import com.focusfortress.model.User;
import com.focusfortress.repository.CategoryRepository;
import com.focusfortress.repository.SubcategoryRepository;
import com.focusfortress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SubcategoryDTO> getSubcategoriesForCategory(Long categoryId, String email) {
        Category category = findOwnedCategory(categoryId, findUser(email));
        return subcategoryRepository.findByCategoryId(category.getId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubcategoryDTO getSubcategoryById(Long id, String email) {
        return convertToDTO(findOwnedSubcategory(id, findUser(email)));
    }

    public SubcategoryDTO createSubcategory(SubcategoryDTO subcategoryDTO, String email) {
        Category category = findOwnedCategory(subcategoryDTO.getCategoryId(), findUser(email));

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

    public SubcategoryDTO updateSubcategory(Long id, SubcategoryDTO subcategoryDTO, String email) {
        User user = findUser(email);
        Subcategory subcategory = findOwnedSubcategory(id, user);

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
            Category newCategory = findOwnedCategory(subcategoryDTO.getCategoryId(), user);

            if (subcategoryRepository.existsByNameAndCategoryId(subcategory.getName(), newCategory.getId())) {
                throw new IllegalArgumentException("Subcategory with this name already exists in the target category");
            }

            subcategory.setCategory(newCategory);
        }

        Subcategory updated = subcategoryRepository.save(subcategory);
        return convertToDTO(updated);
    }

    public void deleteSubcategory(Long id, String email) {
        subcategoryRepository.delete(findOwnedSubcategory(id, findUser(email)));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private Category findOwnedCategory(Long categoryId, User user) {
        return categoryRepository.findByIdAndOwnerId(categoryId, user.getId())
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private Subcategory findOwnedSubcategory(Long id, User user) {
        return subcategoryRepository.findByIdAndCategoryOwnerId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Subcategory not found"));
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