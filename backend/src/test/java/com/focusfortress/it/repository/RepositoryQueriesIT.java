package com.focusfortress.it.repository;

import com.focusfortress.it.MySqlIntegrationTestBase;
import com.focusfortress.model.Category;
import com.focusfortress.model.Habit;
import com.focusfortress.model.HabitProgress;
import com.focusfortress.model.MediaStatus;
import com.focusfortress.model.Subcategory;
import com.focusfortress.model.User;
import com.focusfortress.model.UserInterest;
import com.focusfortress.model.UserMediaItem;
import com.focusfortress.repository.CategoryRepository;
import com.focusfortress.repository.CounterRepository;
import com.focusfortress.repository.HabitProgressRepository;
import com.focusfortress.repository.HabitRepository;
import com.focusfortress.repository.SubcategoryRepository;
import com.focusfortress.repository.UserInterestRepository;
import com.focusfortress.repository.UserMediaItemRepository;
import com.focusfortress.repository.UserRepository;
import com.focusfortress.service.InterestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Derived and {@code @Query} repository methods executed as real MySQL SQL - including the generated
 * ORDER BY, the composite unique constraints and derived deletes.
 */
@DisplayName("Repository queries on MySQL")
class RepositoryQueriesIT extends MySqlIntegrationTestBase {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HabitRepository habitRepository;
    @Autowired
    private HabitProgressRepository habitProgressRepository;
    @Autowired
    private UserInterestRepository userInterestRepository;
    @Autowired
    private UserMediaItemRepository mediaRepository;
    @Autowired
    private CounterRepository counterRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SubcategoryRepository subcategoryRepository;
    @Autowired
    private InterestService interestService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Nested
    @DisplayName("UserRepository")
    class Users {

        @Test
        @DisplayName("findByVerificationToken finds exactly the owner of the token")
        void findByVerificationToken() {
            User pending = data.verifiedUser();
            pending.setVerificationToken("3f1c2b9e-0000-4000-8000-000000000001");
            userRepository.save(pending);
            data.verifiedUser();

            assertThat(userRepository.findByVerificationToken("3f1c2b9e-0000-4000-8000-000000000001"))
                    .get().extracting(User::getId).isEqualTo(pending.getId());
            assertThat(userRepository.findByVerificationToken("does-not-exist")).isEmpty();
        }

        @Test
        @DisplayName("createdAt is filled by @CreationTimestamp and never updated afterwards")
        void createdAtIsImmutable() {
            User user = data.verifiedUser();
            LocalDateTime createdAt = jdbc.queryForObject(
                    "SELECT created_at FROM users WHERE id = ?", LocalDateTime.class, user.getId());

            user.setName("Renamed");
            userRepository.save(user);

            assertThat(jdbc.queryForObject("SELECT created_at FROM users WHERE id = ?", LocalDateTime.class, user.getId()))
                    .isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("UserInterestRepository")
    class Interests {

        @Test
        @DisplayName("@Query countByUserId counts only the given user's interests")
        void countByUserId() {
            User alice = data.verifiedUser();
            User bob = data.verifiedUser();
            interestService.createInitialInterestsForUser(alice, Set.of("Meditation", "Stoicism", "Physics", "Sleep"));
            interestService.createInitialInterestsForUser(bob, Set.of("Meditation", "Stoicism", "Physics"));

            assertThat(userInterestRepository.countByUserId(alice.getId())).isEqualTo(4);
            assertThat(userInterestRepository.countByUserId(bob.getId())).isEqualTo(3);
            assertThat(userInterestRepository.countByUserId(-1L)).isZero();
            assertThat(categoryRepository.count())
                    .as("categories are shared, not duplicated per user")
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("findByUserIdOrderBySelectedAtDesc returns newest first")
        void orderedBySelectedAtDesc() {
            User user = data.verifiedUser();
            interestService.createInitialInterestsForUser(user, Set.of("Meditation", "Physics", "Sleep"));
            setSelectedAt(user, "Physics", "2024-01-01 10:00:00");
            setSelectedAt(user, "Sleep", "2024-03-01 10:00:00");
            setSelectedAt(user, "Meditation", "2024-02-01 10:00:00");

            List<String> names = transactionTemplate.execute(status ->
                    userInterestRepository.findByUserIdOrderBySelectedAtDesc(user.getId()).stream()
                            .map(UserInterest::getSubcategory).map(Subcategory::getName).toList());

            assertThat(names).containsExactly("Sleep", "Meditation", "Physics");
        }

        @Test
        @DisplayName("UNIQUE(user_id, subcategory_id): the same interest cannot be stored twice")
        void interestIsUniquePerUser() {
            User user = data.verifiedUser();
            interestService.createInitialInterestsForUser(user, Set.of("Meditation", "Physics", "Sleep"));
            Subcategory meditation = catalogSubcategory("Meditation").orElseThrow();

            assertThatThrownBy(() -> userInterestRepository.saveAndFlush(
                    UserInterest.builder().user(user).subcategory(meditation).build()))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("derived deleteByUserIdAndSubcategoryId removes one row inside a transaction - and fails without one")
        void derivedDelete() {
            User user = data.verifiedUser();
            interestService.createInitialInterestsForUser(user, Set.of("Meditation", "Physics", "Sleep"));
            Long physicsId = catalogSubcategory("Physics").orElseThrow().getId();

            assertThatThrownBy(() -> userInterestRepository.deleteByUserIdAndSubcategoryId(user.getId(), physicsId))
                    .as("derived delete outside a transaction")
                    .isInstanceOf(InvalidDataAccessApiUsageException.class);

            transactionTemplate.executeWithoutResult(status ->
                    userInterestRepository.deleteByUserIdAndSubcategoryId(user.getId(), physicsId));

            assertThat(userInterestRepository.countByUserId(user.getId())).isEqualTo(2);
            assertThat(userInterestRepository.existsByUserIdAndSubcategoryId(user.getId(), physicsId)).isFalse();
        }

        private void setSelectedAt(User user, String subcategory, String timestamp) {
            jdbc.update("""
                    UPDATE user_interests ui JOIN subcategories s ON s.id = ui.subcategory_id
                    SET ui.selected_at = ? WHERE ui.user_id = ? AND s.name = ?
                    """, timestamp, user.getId(), subcategory);
        }
    }

    @Nested
    @DisplayName("HabitRepository / HabitProgressRepository")
    class Habits {

        @Test
        @DisplayName("category / subcategory filters return only matching habits of the given user")
        void categoryAndSubcategoryFilters() {
            User user = data.verifiedUser();
            User other = data.verifiedUser();
            Category health = categoryRepository.save(Category.builder().name("Health").icon("H").build());
            Category mind = categoryRepository.save(Category.builder().name("Mind").icon("M").build());
            Subcategory sleep = subcategoryRepository.save(Subcategory.builder().name("Sleep").icon("S").category(health).build());
            Subcategory food = subcategoryRepository.save(Subcategory.builder().name("Food").icon("F").category(health).build());
            Subcategory focus = subcategoryRepository.save(Subcategory.builder().name("Focus").icon("F").category(mind).build());

            Habit bedtime = categorised(user, "Bed by 23:00", health, sleep);
            Habit veggies = categorised(user, "Veggies", health, food);
            categorised(user, "Deep work", mind, focus);
            categorised(other, "Other's bedtime", health, sleep);

            assertThat(habitRepository.findByUserIdAndCategoryId(user.getId(), health.getId()))
                    .extracting(Habit::getId).containsExactlyInAnyOrder(bedtime.getId(), veggies.getId());
            assertThat(habitRepository.findByUserIdAndSubcategoryId(user.getId(), sleep.getId()))
                    .extracting(Habit::getId).containsExactly(bedtime.getId());
            assertThat(habitRepository.findByUserIdAndCategoryIdAndSubcategoryId(user.getId(), health.getId(), food.getId()))
                    .extracting(Habit::getId).containsExactly(veggies.getId());
            assertThat(habitRepository.findByUserIdAndCategoryIdAndSubcategoryId(user.getId(), mind.getId(), sleep.getId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("findByIdAndUserId does not return another user's habit")
        void findByIdAndUserIdEnforcesOwnership() {
            User owner = data.verifiedUser();
            User other = data.verifiedUser();
            Habit habit = data.habit(owner, "Mine", 10);

            assertThat(habitRepository.findByIdAndUserId(habit.getId(), owner.getId())).isPresent();
            assertThat(habitRepository.findByIdAndUserId(habit.getId(), other.getId())).isEmpty();
        }

        @Test
        @DisplayName("findByHabitOrderByDateDesc sorts by date regardless of insertion order")
        void progressOrderedByDateDesc() {
            User user = data.verifiedUser();
            Habit habit = data.habit(user, "Ordered", 30);
            data.progress(habit, LocalDate.of(2025, 3, 2));
            data.progress(habit, LocalDate.of(2025, 3, 5));
            data.progress(habit, LocalDate.of(2025, 3, 1));

            assertThat(habitProgressRepository.findByHabitOrderByDateDesc(habit))
                    .extracting(HabitProgress::getDate)
                    .containsExactly(LocalDate.of(2025, 3, 5), LocalDate.of(2025, 3, 2), LocalDate.of(2025, 3, 1));
            assertThat(habitProgressRepository.findByHabitAndDate(habit, LocalDate.of(2025, 3, 2))).isPresent();
            assertThat(habitProgressRepository.findByHabitAndDate(habit, LocalDate.of(2025, 3, 3))).isEmpty();
        }

        @Test
        @DisplayName("deleteByHabitId removes only that habit's progress")
        void deleteByHabitId() {
            User user = data.verifiedUser();
            Habit keep = data.habit(user, "Keep", 30);
            Habit drop = data.habit(user, "Drop", 30);
            data.progressForPreviousDays(keep, 2);
            data.progressForPreviousDays(drop, 3);

            transactionTemplate.executeWithoutResult(status -> habitProgressRepository.deleteByHabitId(drop.getId()));

            assertThat(habitProgressRepository.findByHabitOrderByDateDesc(keep)).hasSize(2);
            assertThat(habitProgressRepository.findByHabitOrderByDateDesc(drop)).isEmpty();
        }

        @Test
        @DisplayName("FK habit_progress -> habits: a habit with history cannot be deleted directly")
        void progressForeignKeyProtectsHistory() {
            User user = data.verifiedUser();
            Habit habit = data.habit(user, "With history", 30);
            data.progressForPreviousDays(habit, 1);

            assertThatThrownBy(() -> habitRepository.deleteById(habit.getId()))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        private Habit categorised(User user, String name, Category category, Subcategory subcategory) {
            return habitRepository.save(Habit.builder().name(name).durationDays(30).user(user)
                    .category(category).subcategory(subcategory).done(false).build());
        }
    }

    @Nested
    @DisplayName("UserMediaItemRepository / CounterRepository")
    class MediaAndCounters {

        @Test
        @DisplayName("media library queries are scoped by user e-mail and ordered newest first")
        void mediaQueries() {
            UserMediaItem older = data.mediaItem("a@focusfortress.test", "vid-old", MediaStatus.WATCH_LATER);
            UserMediaItem newer = data.mediaItem("a@focusfortress.test", "vid-new", MediaStatus.FINISHED);
            UserMediaItem foreign = data.mediaItem("b@focusfortress.test", "vid-old", MediaStatus.WATCH_LATER);
            jdbc.update("UPDATE user_media_library SET added_at = '2024-01-01 00:00:00' WHERE id = ?", older.getId());

            assertThat(mediaRepository.findByUserEmailOrderByAddedAtDesc("a@focusfortress.test"))
                    .extracting(UserMediaItem::getId).containsExactly(newer.getId(), older.getId());
            assertThat(mediaRepository.findByUserEmailAndStatus("a@focusfortress.test", MediaStatus.FINISHED))
                    .extracting(UserMediaItem::getId).containsExactly(newer.getId());
            assertThat(mediaRepository.findByIdAndUserEmail(foreign.getId(), "a@focusfortress.test")).isEmpty();
            assertThat(mediaRepository.existsByUserEmailAndVideoId("b@focusfortress.test", "vid-old")).isTrue();
            assertThat(mediaRepository.existsByUserEmailAndVideoId("b@focusfortress.test", "vid-new")).isFalse();
        }

        @Test
        @DisplayName("uk_counters_user_name: counter names are unique per user, but not globally")
        void counterNameUniquePerUser() {
            User alice = data.verifiedUser();
            User bob = data.verifiedUser();
            data.counter(alice, "No smoking", LocalDateTime.now().minusDays(1));
            data.counter(bob, "No smoking", LocalDateTime.now().minusDays(1));

            assertThat(counterRepository.existsByUserIdAndName(alice.getId(), "No smoking")).isTrue();
            assertThat(counterRepository.findByUserId(bob.getId())).hasSize(1);
            assertThatThrownBy(() -> data.counter(alice, "No smoking", LocalDateTime.now()))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uk_counters_user_name");
        }
    }

    private java.util.Optional<Subcategory> catalogSubcategory(String name) {
        String categoryName = name.equals("Meditation") ? "Mind & Body" : "Natural Sciences";
        Long categoryId = categoryRepository.findFirstByNameAndOwnerIsNullOrderByIdAsc(categoryName).orElseThrow().getId();
        return subcategoryRepository.findByNameAndCategoryId(name, categoryId);
    }
}
