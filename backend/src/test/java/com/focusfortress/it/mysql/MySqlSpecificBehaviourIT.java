package com.focusfortress.it.mysql;

import com.focusfortress.dto.HabitDTO;
import com.focusfortress.dto.UserRegistrationDTO;
import com.focusfortress.it.MySqlIntegrationTestBase;
import com.focusfortress.model.Habit;
import com.focusfortress.model.User;
import com.focusfortress.repository.HabitRepository;
import com.focusfortress.repository.UserRepository;
import com.focusfortress.service.HabitService;
import com.focusfortress.service.UserService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

/**
 * Behaviour that only shows up on a real MySQL server - an H2 based test suite would report the opposite
 * (or nothing) for every test in this class.
 */
@DisplayName("MySQL-specific behaviour")
class MySqlSpecificBehaviourIT extends MySqlIntegrationTestBase {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitService habitService;

    @Autowired
    private HabitRepository habitRepository;

    @Nested
    @DisplayName("default collation utf8mb4_0900_ai_ci (case- and accent-insensitive)")
    class Collation {

        @Test
        @DisplayName("schema really uses the case/accent-insensitive collation")
        void columnsUseAiCiCollation() {
            String collation = jdbc.queryForObject("""
                    SELECT COLLATION_NAME FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email'
                    """, String.class);
            assertThat(collation).isEqualTo("utf8mb4_0900_ai_ci");
        }

        @Test
        @DisplayName("e-mail differing only in letter case is treated as already registered")
        void emailUniquenessIsCaseInsensitive() {
            userService.registerUser(registration("Jan.Kowalski@FocusFortress.test"));

            assertThatThrownBy(() -> userService.registerUser(registration("jan.kowalski@focusfortress.test")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Email already exists!");
            assertThat(userRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("UNIQUE(email) constraint itself rejects a case variant (last line of defence)")
        void uniqueConstraintIsCaseInsensitive() {
            data.verifiedUser("mixed.Case@focusfortress.test");

            assertThatThrownBy(() -> data.verifiedUser("MIXED.case@FOCUSFORTRESS.test"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("login works with any letter case and the JWT carries the stored e-mail")
        void loginIsCaseInsensitive() {
            data.verifiedUser("case.login@focusfortress.test");

            String jwt = given().contentType(ContentType.JSON)
                    .body(Map.of("email", "CASE.LOGIN@focusfortress.TEST", "password", "Secret123"))
                    .when().post("/api/auth/login")
                    .then().statusCode(200)
                    .body("accessToken", not(emptyOrNullString()))
                    .extract().path("accessToken");

            given().auth().oauth2(jwt)
                    .when().get("/api/user/profile")
                    .then().statusCode(200)
                    .body("email", org.hamcrest.Matchers.equalTo("case.login@focusfortress.test"));
        }

        @Test
        @DisplayName("duplicate-habit check ignores case and accents ('Café' == 'CAFE')")
        void habitNameDuplicateCheckIgnoresCaseAndAccents() {
            User user = data.verifiedUser();
            habitService.saveHabit(habitDto("Café with a book", 7), user.getEmail());

            assertThatThrownBy(() -> habitService.saveHabit(habitDto("CAFE WITH A BOOK", 7), user.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Habit with this name already exists for the user");
        }
    }

    @Nested
    @DisplayName("utf8mb4 and strict SQL mode")
    class CharsetAndStrictMode {

        @Test
        @DisplayName("STRICT_TRANS_TABLES is active, as on a default MySQL 8.4 install")
        void strictModeIsActive() {
            String sqlMode = jdbc.queryForObject("SELECT @@SESSION.sql_mode", String.class);
            assertThat(sqlMode).contains("STRICT_TRANS_TABLES");
        }

        @Test
        @DisplayName("multi-code-point emoji (ZWJ sequence) survives a round-trip in a 5-char icon column")
        void emojiRoundTrip() {
            User user = data.verifiedUser();
            String meditatingMan = "🧘‍♂️"; // 🧘‍♂️ - 4 code points, 5 UTF-16 units

            Habit saved = habitService.saveHabit(habitDtoWithIcon("Meditate", meditatingMan), user.getEmail());

            String stored = jdbc.queryForObject("SELECT icon FROM habits WHERE id = ?", String.class, saved.getId());
            assertThat(stored).isEqualTo(meditatingMan);
            assertThat(habitRepository.findById(saved.getId()).orElseThrow().getIcon()).isEqualTo(meditatingMan);
        }

        @Test
        @DisplayName("VARCHAR(5) counts code points: a 7-code-point family emoji is rejected, not truncated")
        void tooLongEmojiIsRejected() {
            User user = data.verifiedUser();
            String family = "👨‍👩‍👧‍👦"; // 👨‍👩‍👧‍👦

            assertThatThrownBy(() -> habitService.saveHabit(habitDtoWithIcon("Family dinner", family), user.getEmail()))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("Data too long for column 'icon'");
            assertThat(habitRepository.count()).isZero();
        }

        @Test
        @DisplayName("enum columns are native MySQL ENUMs - an unknown status is rejected by the database")
        void unknownEnumValueIsRejected() {
            String columnType = jdbc.queryForObject("""
                    SELECT COLUMN_TYPE FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_media_library' AND COLUMN_NAME = 'status'
                    """, String.class);
            assertThat(columnType)
                    .startsWith("enum(")
                    .contains("'WATCH_LATER'", "'CURRENTLY_WATCHING'", "'FINISHED'")
                    .doesNotContain("'TO_WATCH'", "'WATCHED'");

            // exactly the INSERT used by triggers/test_triggers.sh (status 'TO_WATCH' does not exist)
            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO user_media_library (video_id, title, status, user_email, added_at, updated_at)
                    VALUES ('test1', 'Test', 'TO_WATCH', 'test@test.com', NOW(), NOW())
                    """))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("Data truncated for column 'status'");
        }
    }

    // ------------------------------------------------------------------ helpers

    private static UserRegistrationDTO registration(String email) {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setName("Jan Kowalski");
        dto.setEmail(email);
        dto.setDateOfBirth(LocalDate.of(1990, 1, 1));
        dto.setGender("Male");
        dto.setPassword("Secret123");
        dto.setConfirmPassword("Secret123");
        dto.setSelectedInterests(Set.of("Meditation", "Stoicism", "Physics"));
        return dto;
    }

    private static HabitDTO habitDto(String name, int durationDays) {
        HabitDTO dto = new HabitDTO();
        dto.setName(name);
        dto.setDurationDays(durationDays);
        return dto;
    }

    private static HabitDTO habitDtoWithIcon(String name, String icon) {
        HabitDTO dto = habitDto(name, 21);
        dto.setIcon(icon);
        return dto;
    }
}
