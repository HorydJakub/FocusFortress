package com.focusfortress.unit.service;

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
import com.focusfortress.repository.CategoryRepository;
import com.focusfortress.repository.HabitProgressRepository;
import com.focusfortress.repository.HabitRepository;
import com.focusfortress.repository.SubcategoryRepository;
import com.focusfortress.repository.UserRepository;
import com.focusfortress.service.HabitProgressService;
import com.focusfortress.service.HabitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static com.focusfortress.unit.TestData.OTHER_EMAIL;
import static com.focusfortress.unit.TestData.OWNER_EMAIL;
import static com.focusfortress.unit.TestData.category;
import static com.focusfortress.unit.TestData.habit;
import static com.focusfortress.unit.TestData.owner;
import static com.focusfortress.unit.TestData.subcategory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HabitService")
class HabitServiceTest {

    @Mock
    private HabitRepository habitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SubcategoryRepository subcategoryRepository;
    @Mock
    private HabitProgressRepository habitProgressRepository;
    @Mock
    private HabitProgressService habitProgressService;

    @InjectMocks
    private HabitService service;

    private User owner;
    private Category category;
    private Subcategory subcategory;

    @BeforeEach
    void setUp() {
        owner = owner();
        category = category(100L);
        subcategory = subcategory(200L, category);
    }

    private void givenOwnerExists() {
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
    }

    private static HabitDTO dto(String name, Long categoryId, Long subcategoryId, int duration) {
        HabitDTO dto = new HabitDTO();
        dto.setName(name);
        dto.setDescription("Read 20 pages");
        dto.setIcon("B");
        dto.setCategoryId(categoryId);
        dto.setSubcategoryId(subcategoryId);
        dto.setDurationDays(duration);
        return dto;
    }

    @Nested
    @DisplayName("saveHabit")
    class SaveHabit {

        @Test
        @DisplayName("maps every DTO field onto a new, not-done habit owned by the caller")
        void mapsDtoToEntity() {
            givenOwnerExists();
            when(categoryRepository.findByIdAndOwnerId(100L, owner.getId())).thenReturn(Optional.of(category));
            when(subcategoryRepository.findByIdAndCategoryOwnerId(200L, owner.getId())).thenReturn(Optional.of(subcategory));
            when(habitRepository.save(any(Habit.class))).thenAnswer(inv -> inv.getArgument(0));

            Habit saved = service.saveHabit(dto("Reading", 100L, 200L, 30), OWNER_EMAIL);

            assertThat(saved.getName()).isEqualTo("Reading");
            assertThat(saved.getDescription()).isEqualTo("Read 20 pages");
            assertThat(saved.getIcon()).isEqualTo("B");
            assertThat(saved.getDurationDays()).isEqualTo(30);
            assertThat(saved.getCategory()).isSameAs(category);
            assertThat(saved.getSubcategory()).isSameAs(subcategory);
            assertThat(saved.getUser()).isSameAs(owner);
            assertThat(saved.isDone()).isFalse();
        }

        @Test
        @DisplayName("allows a habit without category and subcategory")
        void uncategorised() {
            givenOwnerExists();
            when(habitRepository.save(any(Habit.class))).thenAnswer(inv -> inv.getArgument(0));

            Habit saved = service.saveHabit(dto("Walk", null, null, 7), OWNER_EMAIL);

            assertThat(saved.getCategory()).isNull();
            assertThat(saved.getSubcategory()).isNull();
            verify(categoryRepository, never()).findById(anyLong());
            verify(subcategoryRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("rejects a duplicate habit name for the same user")
        void duplicateName() {
            givenOwnerExists();
            when(habitRepository.existsByUserIdAndName(owner.getId(), "Reading")).thenReturn(true);

            assertThatThrownBy(() -> service.saveHabit(dto("Reading", null, null, 7), OWNER_EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
            verify(habitRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails with NotFoundException for an unknown category")
        void unknownCategory() {
            givenOwnerExists();
            when(categoryRepository.findByIdAndOwnerId(100L, owner.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.saveHabit(dto("Reading", 100L, null, 7), OWNER_EMAIL))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Category not found");
        }

        @Test
        @DisplayName("fails with NotFoundException for an unknown subcategory")
        void unknownSubcategory() {
            givenOwnerExists();
            when(subcategoryRepository.findByIdAndCategoryOwnerId(200L, owner.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.saveHabit(dto("Reading", null, 200L, 7), OWNER_EMAIL))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Subcategory not found");
        }

        @Test
        @DisplayName("fails for an unknown user")
        void unknownUser() {
            when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.saveHabit(dto("Reading", null, null, 7), OWNER_EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("rejects a subcategory that belongs to a different category")
        void mismatchedSubcategory() {
            givenOwnerExists();
            Category otherCategory = category(999L);
            when(categoryRepository.findByIdAndOwnerId(999L, owner.getId())).thenReturn(Optional.of(otherCategory));
            when(subcategoryRepository.findByIdAndCategoryOwnerId(200L, owner.getId())).thenReturn(Optional.of(subcategory));

            assertThatThrownBy(() -> service.saveHabit(dto("Reading", 999L, 200L, 7), OWNER_EMAIL))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(habitRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateHabit")
    class UpdateHabit {

        private Habit existing;

        @BeforeEach
        void setUp() {
            existing = habit(10L, owner, 21);
            existing.setCategory(category);
            existing.setSubcategory(subcategory);
        }

        private void givenExistingHabit() {
            givenOwnerExists();
            when(habitRepository.findByIdAndUserId(10L, owner.getId())).thenReturn(Optional.of(existing));
        }

        @Test
        @DisplayName("updates editable fields of a habit that has not been started")
        void updatesFields() {
            givenExistingHabit();
            Category newCategory = category(101L);
            Subcategory newSubcategory = subcategory(201L, newCategory);
            when(habitProgressService.getCurrentStreak(10L, OWNER_EMAIL)).thenReturn(0);
            when(categoryRepository.findByIdAndOwnerId(101L, owner.getId())).thenReturn(Optional.of(newCategory));
            when(subcategoryRepository.findByIdAndCategoryOwnerId(201L, owner.getId())).thenReturn(Optional.of(newSubcategory));
            when(habitRepository.save(existing)).thenReturn(existing);

            Habit updated = service.updateHabit(10L, dto("Renamed", 101L, 201L, 60), OWNER_EMAIL);

            assertThat(updated.getName()).isEqualTo("Renamed");
            assertThat(updated.getDurationDays()).isEqualTo(60);
            assertThat(updated.getCategory()).isSameAs(newCategory);
            assertThat(updated.getSubcategory()).isSameAs(newSubcategory);
        }

        @Test
        @DisplayName("allows renaming a started habit when the duration is unchanged")
        void renameStartedHabit() {
            givenExistingHabit();
            when(habitProgressService.getCurrentStreak(10L, OWNER_EMAIL)).thenReturn(5);
            when(categoryRepository.findByIdAndOwnerId(100L, owner.getId())).thenReturn(Optional.of(category));
            when(subcategoryRepository.findByIdAndCategoryOwnerId(200L, owner.getId())).thenReturn(Optional.of(subcategory));
            when(habitRepository.save(existing)).thenReturn(existing);

            Habit updated = service.updateHabit(10L, dto("Renamed", 100L, 200L, 21), OWNER_EMAIL);

            assertThat(updated.getName()).isEqualTo("Renamed");
        }

        @Test
        @DisplayName("blocks a duration change once the streak has started")
        void durationLockedAfterStart() {
            givenExistingHabit();
            when(habitProgressService.getCurrentStreak(10L, OWNER_EMAIL)).thenReturn(1);

            assertThatThrownBy(() -> service.updateHabit(10L, dto("Reading", 100L, 200L, 30), OWNER_EMAIL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot change duration");
            verify(habitRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses to edit a completed habit")
        void completedHabitIsReadOnly() {
            existing.setDone(true);
            givenExistingHabit();

            assertThatThrownBy(() -> service.updateHabit(10L, dto("Reading", 100L, 200L, 21), OWNER_EMAIL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot edit a completed habit");
            verify(habitProgressService, never()).getCurrentStreak(anyLong(), any());
        }

        @Test
        @DisplayName("rejects a subcategory from a different category")
        void mismatchedSubcategory() {
            givenExistingHabit();
            Category otherCategory = category(999L);
            when(habitProgressService.getCurrentStreak(10L, OWNER_EMAIL)).thenReturn(0);
            when(categoryRepository.findByIdAndOwnerId(999L, owner.getId())).thenReturn(Optional.of(otherCategory));
            when(subcategoryRepository.findByIdAndCategoryOwnerId(200L, owner.getId())).thenReturn(Optional.of(subcategory));

            assertThatThrownBy(() -> service.updateHabit(10L, dto("Reading", 999L, 200L, 21), OWNER_EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("returns ResourceNotFoundException when the habit is not owned by the caller")
        void notOwned() {
            givenOwnerExists();
            when(habitRepository.findByIdAndUserId(10L, owner.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateHabit(10L, dto("Reading", 100L, 200L, 21), OWNER_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Habit not found");
        }

        @Test
        @DisplayName("rejects renaming to a name already used by another habit of the user")
        void duplicateNameOnRename() {
            givenExistingHabit();
            lenient().when(habitRepository.existsByUserIdAndName(owner.getId(), "Taken")).thenReturn(true);
            lenient().when(habitProgressService.getCurrentStreak(10L, OWNER_EMAIL)).thenReturn(0);
            lenient().when(categoryRepository.findByIdAndOwnerId(100L, owner.getId())).thenReturn(Optional.of(category));
            lenient().when(subcategoryRepository.findByIdAndCategoryOwnerId(200L, owner.getId())).thenReturn(Optional.of(subcategory));
            lenient().when(habitRepository.save(existing)).thenReturn(existing);

            assertThatThrownBy(() -> service.updateHabit(10L, dto("Taken", 100L, 200L, 21), OWNER_EMAIL))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(habitRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows editing a habit that has no category")
        void editUncategorisedHabit() {
            existing.setCategory(null);
            existing.setSubcategory(null);
            givenExistingHabit();
            when(habitProgressService.getCurrentStreak(10L, OWNER_EMAIL)).thenReturn(0);
            when(habitRepository.save(existing)).thenReturn(existing);

            Habit updated = service.updateHabit(10L, dto("Renamed", null, null, 21), OWNER_EMAIL);

            assertThat(updated.getName()).isEqualTo("Renamed");
            assertThat(updated.getCategory()).isNull();
        }
    }

    @Nested
    @DisplayName("deleteHabit / getHabitById")
    class DeleteAndGet {

        @Test
        @DisplayName("deletes progress history before deleting the habit itself")
        void deletesProgressFirst() {
            Habit habit = habit(10L, owner, 21);
            when(habitRepository.findById(10L)).thenReturn(Optional.of(habit));

            service.deleteHabit(10L, OWNER_EMAIL);

            InOrder order = inOrder(habitProgressRepository, habitRepository);
            order.verify(habitProgressRepository).deleteByHabitId(10L);
            order.verify(habitRepository).delete(habit);
        }

        @Test
        @DisplayName("forbids deleting another user's habit and leaves data untouched")
        void deleteForeign() {
            when(habitRepository.findById(10L)).thenReturn(Optional.of(habit(10L, owner, 21)));

            assertThatThrownBy(() -> service.deleteHabit(10L, OTHER_EMAIL)).isInstanceOf(ForbiddenException.class);
            verify(habitProgressRepository, never()).deleteByHabitId(anyLong());
            verify(habitRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws NotFoundException when deleting an unknown habit")
        void deleteUnknown() {
            when(habitRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteHabit(10L, OWNER_EMAIL)).isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("returns the habit to its owner and forbids everybody else")
        void getById() {
            Habit habit = habit(10L, owner, 21);
            when(habitRepository.findById(10L)).thenReturn(Optional.of(habit));

            assertThat(service.getHabitById(10L, OWNER_EMAIL)).isSameAs(habit);
            assertThatThrownBy(() -> service.getHabitById(10L, OTHER_EMAIL)).isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("getHabitsTree")
    class HabitsTree {

        @Test
        @DisplayName("groups habits by category/subcategory and enriches them with the current streak")
        void buildsTree() {
            givenOwnerExists();
            Subcategory emptySubcategory = subcategory(201L, category);
            Habit reading = habit(10L, owner, 21);
            reading.setCategory(category);
            reading.setSubcategory(subcategory);
            Habit uncategorised = habit(11L, owner, 7);

            when(habitRepository.findByUserId(owner.getId())).thenReturn(List.of(reading, uncategorised));
            when(categoryRepository.findByOwnerId(owner.getId())).thenReturn(List.of(category));
            when(subcategoryRepository.findByCategoryId(100L)).thenReturn(List.of(subcategory, emptySubcategory));
            when(habitProgressService.getCurrentStreak(10L, OWNER_EMAIL)).thenReturn(4);

            List<CategoryTreeDTO> tree = service.getHabitsTree(OWNER_EMAIL);

            assertThat(tree).singleElement().satisfies(cat -> {
                assertThat(cat.getId()).isEqualTo(100L);
                assertThat(cat.getSubcategories()).extracting(SubcategoryTreeDTO::getId).containsExactly(200L, 201L);
            });
            SubcategoryTreeDTO withHabit = tree.get(0).getSubcategories().get(0);
            assertThat(withHabit.getHabits()).singleElement().satisfies(h -> {
                assertThat(h.getId()).isEqualTo(10L);
                assertThat(h.getCategoryId()).isEqualTo(100L);
                assertThat(h.getSubcategoryId()).isEqualTo(200L);
                assertThat(h.getDurationDays()).isEqualTo(21);
                assertThat(h.getCurrentStreak()).isEqualTo(4);
                assertThat(h.isDone()).isFalse();
            });
            assertThat(tree.get(0).getSubcategories().get(1).getHabits()).isEmpty();
            // uncategorised habits are not part of the tree, so no streak lookup for them
            verify(habitProgressService, never()).getCurrentStreak(11L, OWNER_EMAIL);
        }
    }

    @Test
    @DisplayName("getUserHabits resolves the user id from the e-mail")
    void getUserHabits() {
        givenOwnerExists();
        List<Habit> habits = List.of(habit(10L, owner, 21));
        when(habitRepository.findByUserId(owner.getId())).thenReturn(habits);

        assertThat(service.getUserHabits(OWNER_EMAIL)).isSameAs(habits);

        ArgumentCaptor<Long> userId = ArgumentCaptor.forClass(Long.class);
        verify(habitRepository).findByUserId(userId.capture());
        assertThat(userId.getValue()).isEqualTo(owner.getId());
    }
}
