package com.focusfortress.controller;

import com.focusfortress.dto.CategoryDTO;
import com.focusfortress.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories(Principal principal) {
        return ResponseEntity.ok(categoryService.getAllCategories(principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable("id") Long id, Principal principal) {
        return ResponseEntity.ok(categoryService.getCategoryById(id, principal.getName()));
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO, Principal principal) {
        CategoryDTO created = categoryService.createCategory(categoryDTO, principal.getName());
        URI location = URI.create("/api/categories/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody CategoryDTO categoryDTO,
            Principal principal) {
        CategoryDTO updated = categoryService.updateCategory(id, categoryDTO, principal.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") Long id, Principal principal) {
        categoryService.deleteCategory(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}