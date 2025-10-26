package com.focusfortress.controller;

import com.focusfortress.dto.SubcategoryDTO;
import com.focusfortress.service.SubcategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/subcategories")
public class SubcategoryController {

    private final SubcategoryService subcategoryService;

    @GetMapping("/{id}")
    public ResponseEntity<SubcategoryDTO> getSubcategoryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(subcategoryService.getSubcategoryById(id));
    }

    @PostMapping
    public ResponseEntity<SubcategoryDTO> createSubcategory(@Valid @RequestBody SubcategoryDTO subcategoryDTO) {
        SubcategoryDTO created = subcategoryService.createSubcategory(subcategoryDTO);
        URI location = URI.create("/api/subcategories/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubcategoryDTO> updateSubcategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody SubcategoryDTO subcategoryDTO) {
        SubcategoryDTO updated = subcategoryService.updateSubcategory(id, subcategoryDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubcategory(@PathVariable("id") Long id) {
        subcategoryService.deleteSubcategory(id);
        return ResponseEntity.noContent().build();
    }
}