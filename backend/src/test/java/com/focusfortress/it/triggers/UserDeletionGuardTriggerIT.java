package com.focusfortress.it.triggers;

import com.focusfortress.it.MySqlIntegrationTestBase;
import com.focusfortress.model.Habit;
import com.focusfortress.model.User;
import com.focusfortress.repository.UserInterestRepository;
import com.focusfortress.repository.UserRepository;
import com.focusfortress.service.HabitService;
import com.focusfortress.service.InterestService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * {@code triggers/trigger2.sql} - {@code before_user_delete}: refuses to delete a user that still has habits,
 * by raising {@code SIGNAL SQLSTATE '45000'}.
 *
 * <p>Important subtlety: {@code habits.user_id} is also a foreign key, so the delete would fail
 * <i>even without the trigger</i> (SQLSTATE 23000, error 1451). "Delete failed" therefore proves nothing.
 * Every negative test asserts the exact trigger signature - SQLSTATE {@value #TRIGGER_SQLSTATE},
 * vendor code {@value #ER_SIGNAL_EXCEPTION} and the trigger's message - so it can only pass because of the trigger.</p>
 */
@DisplayName("Trigger before_user_delete (trigger2.sql)")
class UserDeletionGuardTriggerIT extends MySqlIntegrationTestBase {

    private static final String TRIGGER_SQLSTATE = "45000";
    private static final int ER_SIGNAL_EXCEPTION = 1644;
    private static final String TRIGGER_MESSAGE = "Cannot delete user with active habits. Delete all habits first.";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInterestRepository userInterestRepository;

    @Autowired
    private HabitService habitService;

    @Autowired
    private InterestService interestService;

    @Test
    @DisplayName("plain SQL DELETE of a user with a habit is rejected with SQLSTATE 45000 and the trigger message")
    void sqlDeleteIsRejectedBySignal() {
        User user = data.verifiedUser();
        data.habit(user, "Morning run", 30);

        Throwable thrown = catchThrowable(() -> jdbc.update("DELETE FROM users WHERE id = ?", user.getId()));

        assertIsTriggerSignal(thrown);
        assertThat(userRepository.existsById(user.getId())).isTrue();
    }

    @Test
    @DisplayName("JPA delete is rejected too and the whole transaction (incl. cascaded interest deletes) rolls back")
    void jpaDeleteIsRejectedAndCascadeIsRolledBack() {
        User user = data.verifiedUser();
        interestService.createInitialInterestsForUser(user, Set.of("Meditation", "Stoicism", "Physics"));
        data.habit(user, "Read 20 pages", 21);
        assertThat(userInterestRepository.countByUserId(user.getId())).isEqualTo(3);

        // Hibernate first deletes the user's interests (CascadeType.ALL + orphanRemoval), then the user row.
        // The trigger fires on the second statement - the first one must be rolled back with it.
        Throwable thrown = catchThrowable(() -> userRepository.deleteById(user.getId()));

        assertThat(thrown).isInstanceOf(DataAccessException.class);
        assertIsTriggerSignal(thrown);
        assertThat(userRepository.existsById(user.getId())).isTrue();
        assertThat(userInterestRepository.countByUserId(user.getId()))
                .as("cascaded interest deletes must be rolled back together with the failed user delete")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("the guard counts every habit: a user with several habits is blocked as well")
    void severalHabitsBlockDeletion() {
        User user = data.verifiedUser();
        data.habit(user, "Meditate", 10);
        data.habit(user, "Journal", 10);
        data.habit(user, "Cold shower", 10);

        assertIsTriggerSignal(catchThrowable(() -> jdbc.update("DELETE FROM users WHERE id = ?", user.getId())));
    }

    @Test
    @DisplayName("a user without habits can be deleted")
    void userWithoutHabitsCanBeDeleted() {
        User user = data.verifiedUser();

        userRepository.deleteById(user.getId());

        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    @Test
    @DisplayName("the guard is per user: other users' habits do not block deletion")
    void otherUsersHabitsDoNotBlock() {
        User owner = data.verifiedUser();
        data.habit(owner, "Morning run", 30);
        User other = data.verifiedUser();

        int deleted = jdbc.update("DELETE FROM users WHERE id = ?", other.getId());

        assertThat(deleted).isEqualTo(1);
        assertThat(userRepository.existsById(owner.getId())).isTrue();
    }

    @Test
    @DisplayName("documented workflow: delete habits via HabitService first, then the user can be deleted")
    void deletingHabitsFirstUnblocksUserDeletion() {
        User user = data.verifiedUser();
        Habit habit = data.habit(user, "Morning run", 30);
        data.progressForPreviousDays(habit, 3);

        habitService.deleteHabit(habit.getId(), user.getEmail());
        userRepository.deleteById(user.getId());

        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM habit_progress", Integer.class)).isZero();
    }

    @Test
    @DisplayName("bulk DELETE is atomic: one guarded user aborts the whole statement")
    void bulkDeleteIsAtomic() {
        User free = data.verifiedUser();
        User guarded = data.verifiedUser();
        data.habit(guarded, "Morning run", 30);

        Throwable thrown = catchThrowable(() ->
                jdbc.update("DELETE FROM users WHERE id IN (?, ?)", free.getId(), guarded.getId()));

        assertIsTriggerSignal(thrown);
        assertThat(userRepository.existsById(free.getId()))
                .as("InnoDB rolls back the whole statement, not only the failing row")
                .isTrue();
    }

    @Test
    @Disabled("BUG: trigger2 message, comment and trigger_install_instruction.txt all say 'active habits', but the "
            + "trigger counts ALL habits (no 'done = false' filter). A user whose habits are all completed is "
            + "rejected with the misleading 'Cannot delete user with active habits'. Expected: the trigger stays "
            + "silent (the delete may then still fail on the habits FK, SQLSTATE 23000 - a separate concern).")
    @DisplayName("a user with only COMPLETED habits is not rejected as having 'active habits'")
    void completedHabitsAreNotActiveHabits() {
        User user = data.verifiedUser();
        data.completedHabit(user, "Finished 30-day challenge");

        Throwable thrown = catchThrowable(() -> jdbc.update("DELETE FROM users WHERE id = ?", user.getId()));

        SQLException root = rootSqlException(thrown);
        assertThat(root == null ? null : root.getSQLState()).isNotEqualTo(TRIGGER_SQLSTATE);
    }

    private static void assertIsTriggerSignal(Throwable thrown) {
        assertThat(thrown).as("delete should have been rejected").isNotNull();
        SQLException root = rootSqlException(thrown);
        assertThat((Throwable) root).as("root cause should be a SQLException").isNotNull();
        assertThat(root.getSQLState()).as("SQLSTATE raised by SIGNAL").isEqualTo(TRIGGER_SQLSTATE);
        assertThat(root.getErrorCode()).as("MySQL ER_SIGNAL_EXCEPTION").isEqualTo(ER_SIGNAL_EXCEPTION);
        assertThat(root.getMessage()).isEqualTo(TRIGGER_MESSAGE);
    }

    private static SQLException rootSqlException(Throwable thrown) {
        if (thrown == null) {
            return null;
        }
        Throwable root = NestedExceptionUtils.getMostSpecificCause(thrown);
        if (root instanceof SQLException sql) {
            return sql;
        }
        Throwable cause = thrown;
        while (cause != null && !(cause instanceof SQLException)) {
            cause = cause.getCause();
        }
        return (SQLException) cause;
    }
}
