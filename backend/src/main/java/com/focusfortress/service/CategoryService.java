package com.focusfortress.service;

import com.focusfortress.dto.CategoryDTO;
import com.focusfortress.exception.NotFoundException;
import com.focusfortress.model.Category;
import com.focusfortress.model.User;
import com.focusfortress.repository.CategoryRepository;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories(String email) {
        return categoryRepository.findByOwnerId(findUser(email).getId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id, String email) {
        return convertToDTO(findOwnedCategory(id, findUser(email)));
    }

    public CategoryDTO createCategory(CategoryDTO categoryDTO, String email) {
        User user = findUser(email);

        if (categoryRepository.existsByOwnerIdAndName(user.getId(), categoryDTO.getName())) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        Category category = Category.builder()
                .name(categoryDTO.getName())
                .icon(categoryDTO.getIcon())
                .owner(user)
                .build();

        Category saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }

    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO, String email) {
        User user = findUser(email);
        Category category = findOwnedCategory(id, user);

        // Check if new name conflicts with existing category (but exclude current category!)
        if (categoryDTO.getName() != null && !categoryDTO.getName().equals(category.getName())) {
            if (categoryRepository.existsByOwnerIdAndName(user.getId(), categoryDTO.getName())) {
                throw new IllegalArgumentException("Category with this name already exists");
            }
            category.setName(categoryDTO.getName());
        }

        if (categoryDTO.getIcon() != null) {
            category.setIcon(categoryDTO.getIcon());
        }

        Category updated = categoryRepository.save(category);
        return convertToDTO(updated);
    }

    public void deleteCategory(Long id, String email) {
        categoryRepository.delete(findOwnedCategory(id, findUser(email)));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // Another user's category is reported as not found, so ids of other users' data don't leak
    private Category findOwnedCategory(Long id, User user) {
        return categoryRepository.findByIdAndOwnerId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private CategoryDTO convertToDTO(Category category) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getIcon()
        );
    }
}