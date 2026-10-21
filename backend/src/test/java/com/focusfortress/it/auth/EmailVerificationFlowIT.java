package com.focusfortress.it.auth;

import com.focusfortress.it.MySqlIntegrationTestBase;
import com.focusfortress.model.Role;
import com.focusfortress.model.User;
import com.focusfortress.repository.UserInterestRepository;
import com.focusfortress.repository.UserRepository;
import io.restassured.http.ContentType;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

/**
 * End-to-end account lifecycle over real HTTP (REST Assured -> embedded Tomcat -> MySQL),
 * with the outgoing e-mail captured by GreenMail:
 * <pre>register -> verification e-mail -> token from the link -> verify -> login -> authenticated call</pre>
 */
@DisplayName("Registration and e-mail verification flow")
class EmailVerificationFlowIT extends MySqlIntegrationTestBase {

    private static final Pattern VERIFY_LINK =
            Pattern.compile("href=\"(http://[^\"]+/api/auth/verify\\?token=([0-9a-f\\-]{36}))\"");
    private static final List<String> INTERESTS = List.of("Meditation", "Physics", "Programming");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInterestRepository userInterestRepository;

    @Test
    @DisplayName("happy path: register -> e-mail with link -> verify -> login -> JWT accepted")
    void fullVerificationFlow() throws Exception {
        String email = "anna.nowak@focusfortress.test";

        register(email, INTERESTS)
                .statusCode(200)
                .body(containsString("Please verify your email"));

        // --- persisted state right after registration ---
        User pending = userRepository.findByEmail(email).orElseThrow();
        assertThat(pending.getRole()).isEqualTo(Role.UNVERIFIED);
        assertThat(pending.getVerificationToken()).isNotBlank();
        assertThat(pending.getPassword()).as("password must be stored hashed").startsWith("$2");
        assertThat(userInterestRepository.countByUserId(pending.getId())).isEqualTo(3);

        // --- the e-mail ---
        MimeMessage mail = singleReceivedMail();
        assertThat(mail.getAllRecipients()).extracting(Object::toString).containsExactly(email);
        assertThat(mail.getSubject()).isEqualTo("Verify your FocusFortress account");
        String token = extractToken(mail);
        assertThat(token).isEqualTo(pending.getVerificationToken());

        // --- unverified account cannot log in ---
        login(email).statusCode(403).body("message", containsString("verify your email"));

        // --- click the link ---
        given().queryParam("token", token)
                .when().get("/api/auth/verify")
                .then().statusCode(200).body(equalTo("Account verified successfully!"));

        User verified = userRepository.findByEmail(email).orElseThrow();
        assertThat(verified.getRole()).isEqualTo(Role.USER);
        assertThat(verified.getVerificationToken()).as("token is single-use and must be cleared").isNull();

        // --- login and use the JWT ---
        String jwt = login(email).statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("accessToken", not(emptyOrNullString()))
                .extract().path("accessToken");

        given().auth().oauth2(jwt)
                .when().get("/api/user/profile")
                .then().statusCode(200)
                .body("email", equalTo(email))
                .body("name", equalTo("Anna Nowak"));

        assertThat(userRepository.findByEmail(email).orElseThrow().getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("verification token cannot be reused")
    void tokenIsSingleUse() throws Exception {
        register("reuse@focusfortress.test", INTERESTS).statusCode(200);
        String token = extractToken(singleReceivedMail());

        given().queryParam("token", token).get("/api/auth/verify").then().statusCode(200);

        given().queryParam("token", token)
                .when().get("/api/auth/verify")
                .then().statusCode(400)
                .body("message", equalTo("Invalid verification token"));
    }

    @Test
    @DisplayName("unknown verification token is rejected and nobody gets verified")
    void unknownTokenIsRejected() {
        register("unknown-token@focusfortress.test", INTERESTS).statusCode(200);

        given().queryParam("token", "00000000-0000-0000-0000-000000000000")
                .when().get("/api/auth/verify")
                .then().statusCode(400);

        assertThat(userRepository.findByEmail("unknown-token@focusfortress.test").orElseThrow().getRole())
                .isEqualTo(Role.UNVERIFIED);
    }

    @Test
    @DisplayName("transaction: invalid interest rolls back the already-inserted user and no e-mail is sent")
    void invalidInterestRollsBackRegistration() {
        // UserService.registerUser saves the user (IDENTITY -> INSERT is executed immediately) and only then
        // validates the interests. The INSERT must be rolled back by the @Transactional boundary.
        register("rollback@focusfortress.test", List.of("Meditation", "Physics", "Underwater Basket Weaving"))
                .statusCode(400)
                .body(containsString("Invalid interest(s): Underwater Basket Weaving"));

        assertThat(userRepository.existsByEmail("rollback@focusfortress.test")).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_interests", Integer.class)).isZero();
        assertThat(GREEN_MAIL.getReceivedMessages()).isEmpty();
    }

    @Test
    @DisplayName("each registration gets its own e-mail with its own token")
    void tokensAreUniquePerUser() throws Exception {
        register("first@focusfortress.test", INTERESTS).statusCode(200);
        register("second@focusfortress.test", INTERESTS).statusCode(200);

        assertThat(GREEN_MAIL.waitForIncomingEmail(5_000, 2)).isTrue();
        MimeMessage[] mails = GREEN_MAIL.getReceivedMessages();
        assertThat(extractToken(mails[0])).isNotEqualTo(extractToken(mails[1]));
    }

    // ------------------------------------------------------------------ helpers

    private io.restassured.response.ValidatableResponse register(String email, List<String> interests) {
        Map<String, Object> body = Map.of(
                "name", "Anna Nowak",
                "email", email,
                "dateOfBirth", "1994-03-12",
                "gender", "Female",
                "password", "Secret123",
                "confirmPassword", "Secret123",
                "selectedInterests", interests);
        return given().contentType(ContentType.JSON).body(body)
                .when().post("/api/auth/register")
                .then();
    }

    private io.restassured.response.ValidatableResponse login(String email) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "Secret123"))
                .when().post("/api/auth/login")
                .then();
    }

    private static MimeMessage singleReceivedMail() {
        assertThat(GREEN_MAIL.waitForIncomingEmail(5_000, 1)).as("verification e-mail received").isTrue();
        MimeMessage[] messages = GREEN_MAIL.getReceivedMessages();
        assertThat(messages).hasSize(1);
        return messages[0];
    }

    /** Reads the decoded HTML body (not the raw MIME source, which may be quoted-printable encoded). */
    private static String extractToken(MimeMessage message) throws Exception {
        String html = message.getContent().toString();
        Matcher matcher = VERIFY_LINK.matcher(html);
        assertThat(matcher.find()).as("verification link present in e-mail body").isTrue();
        return matcher.group(2);
    }
}
