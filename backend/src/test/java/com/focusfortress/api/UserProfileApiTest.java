package com.focusfortress.api;

import com.focusfortress.api.support.ApiTestBase;
import com.focusfortress.api.support.RegistrationPayload;
import com.focusfortress.api.support.TestUser;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

@DisplayName("User profile API  /api/user")
class UserProfileApiTest extends ApiTestBase {

    private TestUser user;

    @BeforeEach
    void setUp() {
        user = auth.registerVerifiedUser(RegistrationPayload.aValidRegistration()
                .with("name", "Jan Kowalski")
                .with("dateOfBirth", "1990-02-03")
                .with("gender", "Male"));
    }

    @Nested
    @DisplayName("GET/PUT /profile")
    class Profile {

        @Test
        @DisplayName("GET returns the registration data and never the password")
        void returnsProfile() {
            as(user).get("/user/profile")
                    .then()
                    .statusCode(200)
                    .body("name", equalTo("Jan Kowalski"))
                    .body("email", equalTo(user.email()))
                    .body("dateOfBirth", equalTo("1990-02-03"))
                    .body("gender", equalTo("Male"))
                    .body("$", not(hasKey("password")));
        }

        @Test
        @DisplayName("PUT updates name, date of birth and gender")
        void updatesProfile() {
            as(user).body(profile("Anna Nowak", "1992-12-24", "Female"))
                    .when().put("/user/profile")
                    .then()
                    .statusCode(200)
                    .body("name", equalTo("Anna Nowak"));

            as(user).get("/user/profile")
                    .then()
                    .body("name", equalTo("Anna Nowak"))
                    .body("dateOfBirth", equalTo("1992-12-24"))
                    .body("gender", equalTo("Female"));
        }

        @Test
        @DisplayName("profile changes are visible only to their owner")
        void profileIsIsolated() {
            TestUser other = auth.registerVerifiedUser();
            as(user).body(profile("Only Mine", "1990-02-03", "Male")).put("/user/profile").then().statusCode(200);

            as(other).get("/user/profile")
                    .then()
                    .body("email", equalTo(other.email()))
                    .body("name", equalTo("Test User"));
        }

        @Test
        @DisplayName("PUT rejects a blank name with 400")
        void rejectsBlankName() {
            as(user).body(profile("", "1990-02-03", "Male"))
                    .when().put("/user/profile")
                    .then()
                    .statusCode(400);
        }

        private Map<String, Object> profile(String name, String dateOfBirth, String gender) {
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("email", user.email());
            body.put("dateOfBirth", dateOfBirth);
            body.put("gender", gender);
            return body;
        }
    }

    @Nested
    @DisplayName("report settings and report e-mail")
    class Reports {

        @Test
        @DisplayName("GET /report-settings returns 404 until settings are saved")
        void noSettingsYet() {
            as(user).get("/user/report-settings").then().statusCode(404);
        }

        @Test
        @DisplayName("POST /report-settings saves settings that GET returns")
        void savesSettings() {
            Map<String, Object> settings = Map.of(
                    "includeActiveHabits", true,
                    "includeCompletedHabits", false,
                    "includeInterests", true,
                    "includeMediaLibrary", false,
                    "includeCounters", true,
                    "automaticReports", true,
                    "reportFrequency", "monthly");

            as(user).body(settings)
                    .when().post("/user/report-settings")
                    .then()
                    .statusCode(200)
                    .body(equalTo("Report settings saved successfully"));

            as(user).get("/user/report-settings")
                    .then()
                    .statusCode(200)
                    .body("includeCompletedHabits", equalTo(false))
                    .body("includeMediaLibrary", equalTo(false))
                    .body("automaticReports", equalTo(true))
                    .body("reportFrequency", equalTo("monthly"));
        }

        @Test
        @DisplayName("POST /generate-report e-mails a progress report to the user")
        void generatesReportEmail() throws Exception {
            as(user).body(Map.of("includeInterests", true))
                    .when().post("/user/generate-report")
                    .then()
                    .statusCode(200)
                    .body(equalTo("Report generated and sent to your email"));

            MimeMessage report = Arrays.stream(GREEN_MAIL.getReceivedMessagesForDomain(user.email()))
                    .filter(this::isProgressReport)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No progress report e-mail sent to " + user.email()));
            assertThat(report.getContent().toString())
                    .contains("Jan Kowalski")
                    .contains("Meditation");
        }

        private boolean isProgressReport(MimeMessage message) {
            try {
                return message.getSubject().contains("Progress Report");
            } catch (Exception e) {
                return false;
            }
        }
    }
}
