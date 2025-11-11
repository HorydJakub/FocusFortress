package com.focusfortress.controller;

import com.focusfortress.dto.InterestCategoryDTO;
import com.focusfortress.dto.SubcategoryOptionDTO;
import com.focusfortress.service.InterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for fetching available InterestCategories
 * Used during user registration to display interest options
 */
@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    /**
     * GET /api/interests
     * Returns all available interest categories for registration UI
     * Public endpoint (no authentication required)
     */
    @GetMapping
    public ResponseEntity<List<InterestCategoryDTO>> getAllInterests() {
        return ResponseEntity.ok(interestService.getAllAvailableInterests());
    }

    /**
     * GET /api/interests/subcategories
     * Returns all available subcategories (interests) for registration UI
     * Public endpoint (no authentication required)
     */
    @GetMapping("/subcategories")
    public ResponseEntity<List<SubcategoryOptionDTO>> getAllSubcategories() {
        return ResponseEntity.ok(interestService.getAllAvailableSubcategories());
    }
}