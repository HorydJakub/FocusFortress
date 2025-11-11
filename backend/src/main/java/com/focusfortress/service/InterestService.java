package com.focusfortress.service;

import com.focusfortress.dto.InterestCategoryDTO;
import com.focusfortress.dto.ManageInterestsDTO;
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

import java.util.List;
import java.util.Set;
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
    public void createInitialInterestsForUser(User user, Set<InterestCategory> interests) {
        log.info("Creating initial interests for user: {}", user.getEmail());

        for (InterestCategory interest : interests) {
            UserInterest userInterest = UserInterest.builder()
                    .user(user)
                    .interest(interest)
                    .build();

            user.addInterest(userInterest);

            List<CategoryStructure> categoryStructures = interest.getCategories();

            for (CategoryStructure categoryStructure : categoryStructures) {
                String categoryName = categoryStructure.getName();
                String categoryIcon = categoryStructure.getIcon();

                Category category = categoryRepository.findByName(categoryName)
                        .orElseGet(() -> {
                            Category newCategory = Category.builder()
                                    .name(categoryName)
                                    .icon(categoryIcon)
                                    .build();
                            return categoryRepository.save(newCategory);
                        });

                for (SubcategoryStructure subcategoryStructure : categoryStructure.getSubcategories()) {
                    String subcategoryName = subcategoryStructure.getName();
                    String subcategoryIcon = subcategoryStructure.getIcon();

                    if (!subcategoryRepository.existsByNameAndCategoryId(subcategoryName, category.getId())) {
                        Subcategory subcategory = Subcategory.builder()
                                .name(subcategoryName)
                                .icon(subcategoryIcon)
                                .category(category)
                                .build();
                        subcategoryRepository.save(subcategory);
                    }
                }
            }
        }

        log.info("Successfully created initial interest structure for user");
    }

    public List<InterestCategoryDTO> getAllAvailableInterests() {
        return List.of(InterestCategory.values()).stream()
                .map(ic -> new InterestCategoryDTO(
                        ic.name(),
                        ic.getDisplayName(),
                        ic.getIcon(),
                        ic.getCategories()
                ))
                .toList();
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

    @Transactional
    public List<UserInterestDTO> manageUserInterests(String email, ManageInterestsDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Remove interests (hard delete)
        for (String interestKey : dto.getRemove()) {
            InterestCategory interest = InterestCategory.fromString(interestKey);
            userInterestRepository.deleteByUserIdAndInterest(user.getId(), interest);
            log.info("Removed interest {} for user {}", interest, email);
        }

        // Add new interests
        for (String interestKey : dto.getAdd()) {
            InterestCategory interest = InterestCategory.fromString(interestKey);

            if (!userInterestRepository.existsByUserIdAndInterest(user.getId(), interest)) {
                UserInterest newInterest = UserInterest.builder()
                        .user(user)
                        .interest(interest)
                        .build();
                userInterestRepository.save(newInterest);

                log.info("Added interest {} for user {}", interest, email);
            }
        }

        return getUserInterests(email);
    }

    @Transactional(readOnly = true)
    public Set<InterestCategory> getUserInterestCategories(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return userInterestRepository.findInterestCategoriesByUserId(user.getId());
    }
}