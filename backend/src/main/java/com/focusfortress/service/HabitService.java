package com.focusfortress.service;

import com.focusfortress.dto.CategoryTreeDTO;
import com.focusfortress.dto.HabitDTO;
import com.focusfortress.dto.SubcategoryTreeDTO;
import com.focusfortress.exception.ForbiddenException;
import com.focusfortress.exception.NotFoundException;
import com.focusfortress.exception.ResourceNotFoundException;
import com.focusfortress.model.Category;
import com.focusfortress.model.Habit;
import com.focusfortress.model.Subcategory;
import com.focusfortress.model.User;
import com.focusfortress.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
public class HabitService {

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final HabitProgressRepository habitProgressRepository;
    private final HabitProgressService habitProgressService;

    @Transactional(readOnly = true)
    public List<Habit> getUserHabitsByCategory(String email, Long categoryId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return habitRepository.findByUserIdAndCategoryId(user.getId(), categoryId);
    }

    @Transactional(readOnly = true)
    public List<Habit> getUserHabitsBySubcategory(String email, Long subcategoryId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return habitRepository.findByUserIdAndSubcategoryId(user.getId(), subcategoryId);
    }

    @Transactional(readOnly = true)
    public List<Habit> getUserHabitsByCategoryAndSubcategory(String email, Long categoryId, Long subcategoryId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return habitRepository.findByUserIdAndCategoryIdAndSubcategoryId(user.getId(), categoryId, subcategoryId);
    }

    public Habit saveHabit(HabitDTO habitDTO, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (habitRepository.existsByUserIdAndName(user.getId(), habitDTO.getName())) {
            throw new IllegalArgumentException("Habit with this name already exists for the user");
        }

        Category category = null;
        if (habitDTO.getCategoryId() != null) {
            category = categoryRepository.findById(habitDTO.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
        }

        Subcategory subcategory = null;
        if (habitDTO.getSubcategoryId() != null) {
            subcategory = subcategoryRepository.findById(habitDTO.getSubcategoryId())
                    .orElseThrow(() -> new NotFoundException("Subcategory not found"));
        }

        Habit habit = Habit.builder()
                .name(habitDTO.getName())
                .description(habitDTO.getDescription())
                .category(category)
                .subcategory(subcategory)
                .icon(habitDTO.getIcon())
                .durationDays(habitDTO.getDurationDays())
                .user(user)
                .done(false)
                .build();

        return habitRepository.save(habit);
    }

    public Habit updateHabit(Long habitId, HabitDTO habitDTO, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Habit habit = habitRepository.findByIdAndUserId(habitId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found"));

        // Don't allow editing completed habits
        if (habit.isDone()) {
            throw new IllegalStateException("Cannot edit a completed habit");
        }

        // Block duration change after starting
        int currentStreak = habitProgressService.getCurrentStreak(habitId, email);
        if (currentStreak > 0 && habit.getDurationDays() != habitDTO.getDurationDays()) {
            throw new IllegalStateException("Cannot change duration after starting the habit. Delete and recreate to restart.");
        }

        Category category = categoryRepository.findById(habitDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Subcategory subcategory = subcategoryRepository.findById(habitDTO.getSubcategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found"));

        if (!subcategory.getCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException("Subcategory does not belong to the selected category");
        }

        habit.setName(habitDTO.getName());
        habit.setDescription(habitDTO.getDescription());
        habit.setIcon(habitDTO.getIcon());
        habit.setDurationDays(habitDTO.getDurationDays());
        habit.setCategory(category);
        habit.setSubcategory(subcategory);

        return habitRepository.save(habit);
    }

    @Transactional(readOnly = true)
    public List<Habit> getUserHabits(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return habitRepository.findByUserId(user.getId());
    }

    public void deleteHabit(Long habitId, String email) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));

        if (!habit.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Access denied");
        }

        habitProgressRepository.deleteByHabitId(habitId);
        habitRepository.delete(habit);
    }

    @Transactional(readOnly = true)
    public Habit getHabitById(Long habitId, String email) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new NotFoundException("Habit not found"));

        if (!habit.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Access denied");
        }

        return habit;
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeDTO> getHabitsTree(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Habit> userHabits = habitRepository.findByUserId(user.getId());
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .map(category -> buildCategoryTree(category, userHabits, email))
                .collect(Collectors.toList());
    }

    private CategoryTreeDTO buildCategoryTree(Category category, List<Habit> userHabits, String email) {
        List<Subcategory> subcategories = subcategoryRepository.findByCategoryId(category.getId());
        List<SubcategoryTreeDTO> subcategoryTrees = subcategories.stream()
                .map(subcategory -> buildSubcategoryTree(subcategory, userHabits, email))
                .collect(Collectors.toList());

        return new CategoryTreeDTO(
                category.getId(),
                category.getName(),
                category.getIcon(),
                subcategoryTrees
        );
    }

    private SubcategoryTreeDTO buildSubcategoryTree(Subcategory subcategory, List<Habit> userHabits, String email) {
        List<HabitDTO> habits = userHabits.stream()
                .filter(habit -> habit.getSubcategory() != null &&
                        habit.getSubcategory().getId().equals(subcategory.getId()))
                .map(habit -> {
                    HabitDTO dto = convertToDTO(habit);
                    // Count current streak for each habit
                    dto.setCurrentStreak(
                            habitProgressService.getCurrentStreak(habit.getId(), email)
                    );
                    return dto;
                })
                .collect(Collectors.toList());

        return new SubcategoryTreeDTO(
                subcategory.getId(),
                subcategory.getName(),
                subcategory.getIcon(),
                habits
        );
    }

    private HabitDTO convertToDTO(Habit habit) {
        HabitDTO dto = new HabitDTO();
        dto.setId(habit.getId());
        dto.setName(habit.getName());
        dto.setDescription(habit.getDescription());
        dto.setCategoryId(habit.getCategory() != null ? habit.getCategory().getId() : null);
        dto.setSubcategoryId(habit.getSubcategory() != null ? habit.getSubcategory().getId() : null);
        dto.setIcon(habit.getIcon());
        dto.setDurationDays(habit.getDurationDays());
        dto.setDone(habit.isDone());
        return dto;
    }
}