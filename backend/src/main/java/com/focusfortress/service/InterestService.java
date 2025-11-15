package com.focusfortress.service;

import com.focusfortress.dto.InterestCategoryDTO;
import com.focusfortress.dto.ManageInterestsDTO;
import com.focusfortress.dto.SubcategoryOptionDTO;
import com.focusfortress.dto.UserInterestDTO;
import com.focusfortress.model.*;
import com.focusfortress.repository.CategoryRepository;
import com.focusfortress.repository.SubcategoryRepository;
import com.focusfortress.repository.UserInterestRepository;
import com.focusfortress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class InterestService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;

    @Transactional
    public void createInitialInterestsForUser(User user, Set<String> subcategoryNames) {
        log.info("Creating initial interests for user: {} with subcategories: {}", user.getEmail(), subcategoryNames);

        // Get all subcategory structures from all InterestCategory enums
        Map<String, SubcategoryInfo> subcategoryMap = buildSubcategoryMap();

        // Validate all subcategory names first
        List<String> invalidSubcategories = new ArrayList<>();
        for (String subcategoryName : subcategoryNames) {
            if (!subcategoryMap.containsKey(subcategoryName)) {
                invalidSubcategories.add(subcategoryName);
            }
        }

        if (!invalidSubcategories.isEmpty()) {
            log.error("Invalid subcategories provided: {}", invalidSubcategories);
            throw new IllegalArgumentException("Invalid interest(s): " + String.join(", ", invalidSubcategories) +
                    ". Please select valid subcategories from the available options.");
        }

        for (String subcategoryName : subcategoryNames) {
            SubcategoryInfo info = subcategoryMap.get(subcategoryName);

            // Find or create category
            Category category = categoryRepository.findByName(info.categoryName)
                    .orElseGet(() -> {
                        Category newCategory = Category.builder()
                                .name(info.categoryName)
                                .icon(info.categoryIcon)
                                .build();
                        return categoryRepository.save(newCategory);
                    });

            // Find or create subcategory
            Subcategory subcategory = subcategoryRepository.findByNameAndCategoryId(subcategoryName, category.getId())
                    .orElseGet(() -> {
                        Subcategory newSubcategory = Subcategory.builder()
                                .name(subcategoryName)
                                .icon(info.subcategoryIcon)
                                .category(category)
                                .build();
                        return subcategoryRepository.save(newSubcategory);
                    });

            // Create user interest
            UserInterest userInterest = UserInterest.builder()
                    .user(user)
                    .subcategory(subcategory)
                    .build();
            userInterestRepository.save(userInterest);
        }

        log.info("Successfully created initial interest structure for user");
    }

    private Map<String, SubcategoryInfo> buildSubcategoryMap() {
        Map<String, SubcategoryInfo> map = new HashMap<>();

        for (InterestCategory interestCategory : InterestCategory.values()) {
            for (CategoryStructure categoryStructure : interestCategory.getCategories()) {
                String categoryName = categoryStructure.getName();
                String categoryIcon = categoryStructure.getIcon();

                for (SubcategoryStructure subcategoryStructure : categoryStructure.getSubcategories()) {
                    String subcategoryName = subcategoryStructure.getName();
                    String subcategoryIcon = subcategoryStructure.getIcon();

                    map.put(subcategoryName, new SubcategoryInfo(
                            subcategoryName,
                            subcategoryIcon,
                            categoryName,
                            categoryIcon
                    ));
                }
            }
        }

        return map;
    }

    public List<InterestCategoryDTO> getAllAvailableInterests() {
        return Arrays.stream(InterestCategory.values())
                .map(ic -> new InterestCategoryDTO(
                        ic.name(),
                        ic.getDisplayName(),
                        ic.getCategories()
                ))
                .toList();
    }

    public List<SubcategoryOptionDTO> getAllAvailableSubcategories() {
        List<SubcategoryOptionDTO> options = new ArrayList<>();

        for (InterestCategory interestCategory : InterestCategory.values()) {
            for (CategoryStructure categoryStructure : interestCategory.getCategories()) {
                String categoryName = categoryStructure.getName();
                String categoryIcon = categoryStructure.getIcon();

                for (SubcategoryStructure subcategoryStructure : categoryStructure.getSubcategories()) {
                    options.add(new SubcategoryOptionDTO(
                            subcategoryStructure.getName(),
                            subcategoryStructure.getIcon(),
                            categoryName,
                            categoryIcon
                    ));
                }
            }
        }

        return options;
    }

    @Transactional(readOnly = true)
    public List<UserInterestDTO> getUserInterests(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return userInterestRepository.findByUserIdOrderBySelectedAtDesc(user.getId())
                .stream()
                .map(UserInterestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Set<String> getUserSubcategoryNames(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return userInterestRepository.findByUserIdOrderBySelectedAtDesc(user.getId())
                .stream()
                .map(ui -> ui.getSubcategory().getName())
                .collect(Collectors.toSet());
    }

    @Transactional
    public List<UserInterestDTO> manageUserInterests(String email, ManageInterestsDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Map<String, SubcategoryInfo> subcategoryMap = buildSubcategoryMap();

        // Get current user interests
        Set<String> currentSubcategoryNames = getUserSubcategoryNames(email);

        // Validate all subcategory names first (both add and remove)
        List<String> invalidSubcategories = new ArrayList<>();
        for (String subcategoryName : dto.getAdd()) {
            if (!subcategoryMap.containsKey(subcategoryName)) {
                invalidSubcategories.add(subcategoryName);
            }
        }

        // Check for duplicate additions (interests already present)
        List<String> duplicateAdditions = new ArrayList<>();
        for (String subcategoryName : dto.getAdd()) {
            if (currentSubcategoryNames.contains(subcategoryName)) {
                duplicateAdditions.add(subcategoryName);
            }
        }

        // Check for invalid removals (interests not present)
        List<String> invalidRemovals = new ArrayList<>();
        for (String subcategoryName : dto.getRemove()) {
            if (!currentSubcategoryNames.contains(subcategoryName)) {
                invalidRemovals.add(subcategoryName);
            }
        }

        // Build comprehensive error message
        List<String> errorMessages = new ArrayList<>();
        if (!invalidSubcategories.isEmpty()) {
            errorMessages.add("Invalid subcategory name(s): " + String.join(", ", invalidSubcategories));
        }
        if (!duplicateAdditions.isEmpty()) {
            errorMessages.add("Interest(s) already selected: " + String.join(", ", duplicateAdditions));
        }
        if (!invalidRemovals.isEmpty()) {
            errorMessages.add("Interest(s) not currently selected: " + String.join(", ", invalidRemovals));
        }

        if (!errorMessages.isEmpty()) {
            String fullErrorMessage = String.join(". ", errorMessages);
            log.error("Interest management validation failed for user {}: {}", email, fullErrorMessage);
            throw new IllegalArgumentException(fullErrorMessage);
        }

        // Remove interests (hard delete)
        for (String subcategoryName : dto.getRemove()) {
            Subcategory subcategory = subcategoryRepository.findByName(subcategoryName).orElse(null);
            if (subcategory != null) {
                userInterestRepository.deleteByUserIdAndSubcategoryId(user.getId(), subcategory.getId());
                log.info("Removed interest {} for user {}", subcategoryName, email);
            }
        }

        // Add new interests
        for (String subcategoryName : dto.getAdd()) {
            SubcategoryInfo info = subcategoryMap.get(subcategoryName);
            // Find or create category
            Category category = categoryRepository.findByName(info.categoryName)
                    .orElseGet(() -> {
                        Category newCategory = Category.builder()
                                .name(info.categoryName)
                                .icon(info.categoryIcon)
                                .build();
                        return categoryRepository.save(newCategory);
                    });

            // Find or create subcategory
            Subcategory subcategory = subcategoryRepository.findByNameAndCategoryId(subcategoryName, category.getId())
                    .orElseGet(() -> {
                        Subcategory newSubcategory = Subcategory.builder()
                                .name(subcategoryName)
                                .icon(info.subcategoryIcon)
                                .category(category)
                                .build();
                        return subcategoryRepository.save(newSubcategory);
                    });

            // Create user interest (we already validated it doesn't exist)
            UserInterest newInterest = UserInterest.builder()
                    .user(user)
                    .subcategory(subcategory)
                    .build();
            userInterestRepository.save(newInterest);

            log.info("Added interest {} for user {}", subcategoryName, email);
        }

        return getUserInterests(email);
    }

    @Transactional
    public UserInterestDTO addCustomInterest(String email, String name, String emoji) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Find or create a "Custom" category for user-defined interests
        Category customCategory = categoryRepository.findByName("Custom")
                .orElseGet(() -> {
                    Category newCategory = Category.builder()
                            .name("Custom")
                            .icon("✨")
                            .build();
                    return categoryRepository.save(newCategory);
                });

        // Create the custom subcategory with the provided emoji
        Subcategory customSubcategory = Subcategory.builder()
                .name(name)
                .icon(emoji)
                .category(customCategory)
                .build();
        customSubcategory = subcategoryRepository.save(customSubcategory);

        // Create user interest
        UserInterest userInterest = UserInterest.builder()
                .user(user)
                .subcategory(customSubcategory)
                .build();
        userInterest = userInterestRepository.save(userInterest);

        log.info("Added custom interest '{}' for user {}", name, email);

        return UserInterestDTO.fromEntity(userInterest);
    }

    @Transactional
    public void deleteUserInterest(String email, Long interestId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserInterest userInterest = userInterestRepository.findById(interestId)
                .orElseThrow(() -> new IllegalArgumentException("Interest not found"));

        // Verify the interest belongs to the user
        if (!userInterest.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Interest does not belong to the current user");
        }

        // Check if it's a custom interest (from Custom category)
        boolean isCustom = userInterest.getSubcategory().getCategory().getName().equals("Custom");

        // Delete the user interest
        userInterestRepository.delete(userInterest);

        // If it's a custom interest, also delete the subcategory since it's user-specific
        if (isCustom) {
            Subcategory customSubcategory = userInterest.getSubcategory();
            subcategoryRepository.delete(customSubcategory);
            log.info("Deleted custom interest '{}' and its subcategory for user {}",
                    customSubcategory.getName(), email);
        } else {
            log.info("Removed interest '{}' for user {}",
                    userInterest.getSubcategory().getName(), email);
        }
    }

    private static class SubcategoryInfo {
        String subcategoryName;
        String subcategoryIcon;
        String categoryName;
        String categoryIcon;

        SubcategoryInfo(String subcategoryName, String subcategoryIcon, String categoryName, String categoryIcon) {
            this.subcategoryName = subcategoryName;
            this.subcategoryIcon = subcategoryIcon;
            this.categoryName = categoryName;
            this.categoryIcon = categoryIcon;
        }
    }
}