package com.focusfortress.api;

import com.focusfortress.api.support.ApiTestBase;
import com.focusfortress.api.support.RegistrationPayload;
import com.focusfortress.api.support.TestUser;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.focusfortress.api.support.RegistrationPayload.aValidRegistration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

@DisplayName("Auth API  /api/auth")
class AuthApiTest extends ApiTestBase {

    @Nested
    @DisplayName("POST /register")
    class Register {

        @Test
        @DisplayName("creates an account and e-mails a verification link")
        void registersAndSendsVerificationEmail() {
            RegistrationPayload payload = aValidRegistration();

            auth.register(payload.build())
                    .then()
                    .statusCode(200)
                    .body(equalTo("Account created successfully! Please verify your email."));

            assertThat(auth.verificationTokenFor(payload.email())).isNotBlank();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.focusfortress.api.AuthApiTest#invalidRegistrations")
        @DisplayName("rejects invalid payload with 400 and a field error")
        void rejectsInvalidPayload(String scenario, String field, Object value) {
            auth.register(aValidRegistration().with(field, value).build())
                    .then()
                    .statusCode(400)
                    .body("status", equalTo(400))
                    .body("errors", hasKey(field));
        }

        @Test
        @DisplayName("rejects payload with missing required fields")
        void rejectsMissingFields() {
            auth.register(Map.of())
                    .then()
                    .statusCode(400)
                    .body("errors.keySet()", hasItems(
                            "name", "email", "password", "confirmPassword", "selectedInterests"));
        }

        @Test
        @DisplayName("rejects non-matching password confirmation")
        void rejectsPasswordMismatch() {
            auth.register(aValidRegistration().with("confirmPassword", "Different123").build())
                    .then()
                    .statusCode(400)
                    .body(equalTo("Passwords do not match!"));
        }

        @Test
        @DisplayName("rejects gender outside the allowed list")
        void rejectsUnknownGender() {
            auth.register(aValidRegistration().with("gender", "Robot").build())
                    .then()
                    .statusCode(400)
                    .body(equalTo("Invalid gender value!"));
        }

        @Test
        @DisplayName("rejects interests that are not in the catalog")
        void rejectsUnknownInterest() {
            auth.register(aValidRegistration()
                            .with("selectedInterests", List.of("Meditation", "Physics", "Underwater Basket Weaving"))
                            .build())
                    .then()
                    .statusCode(400)
                    .body(containsString("Underwater Basket Weaving"));
        }

        @Test
        @DisplayName("rejects an e-mail that is already registered")
        void rejectsDuplicateEmail() {
            RegistrationPayload first = aValidRegistration();
            auth.register(first.build()).then().statusCode(200);

            auth.register(aValidRegistration().with("email", first.email()).build())
                    .then()
                    .statusCode(400)
                    .body("message", equalTo("Email already exists!"));
        }
    }

    static Stream<Arguments> invalidRegistrations() {
        return Stream.of(
                Arguments.of("name too short", "name", "A"),
                Arguments.of("blank name", "name", "   "),
                Arguments.of("malformed e-mail", "email", "not-an-email"),
                Arguments.of("date of birth in the future", "dateOfBirth", LocalDate.now().plusDays(1).toString()),
                Arguments.of("password too short", "password", "Ab1"),
                Arguments.of("password without digit", "password", "NoDigitsHere"),
                Arguments.of("password without uppercase", "password", "lowercase123"),
                Arguments.of("only two interests", "selectedInterests", List.of("Meditation", "Physics")),
                Arguments.of("eight interests", "selectedInterests", List.of(
                        "Meditation", "Physics", "Reading", "Biology", "Chemistry", "Focus", "Music", "Hiking"))
        );
    }

    @Nested
    @DisplayName("GET /verify")
    class Verify {

        @Test
        @DisplayName("activates the account for a valid token")
        void verifiesAccount() {
            String token = auth.registerAndCaptureToken(aValidRegistration());

            auth.verify(token)
                    .then()
                    .statusCode(200)
                    .body(equalTo("Account verified successfully!"));
        }

        @Test
        @DisplayName("token is single-use")
        void tokenCannotBeReused() {
            String token = auth.registerAndCaptureToken(aValidRegistration());
            auth.verify(token).then().statusCode(200);

            auth.verify(token)
                    .then()
                    .statusCode(400)
                    .body("message", equalTo("Invalid verification token"));
        }

        @Test
        @DisplayName("rejects an unknown token")
        void rejectsUnknownToken() {
            auth.verify("00000000-0000-0000-0000-000000000000")
                    .then()
                    .statusCode(400)
                    .body("message", equalTo("Invalid verification token"));
        }
    }

    @Nested
    @DisplayName("POST /login")
    class Login {

        @Test
        @DisplayName("returns a Bearer JWT for a verified user")
        void returnsJwt() {
            RegistrationPayload payload = aValidRegistration();
            auth.verify(auth.registerAndCaptureToken(payload)).then().statusCode(200);

            String token = auth.login(payload.email(), payload.password())
                    .then()
                    .statusCode(200)
                    .body("tokenType", equalTo("Bearer"))
                    .body("accessToken", not(emptyOrNullString()))
                    .extract().path("accessToken");

            assertThat(jwtClaim(token, "sub")).isEqualTo(payload.email());
        }

        @Test
        @DisplayName("issued JWT grants access to protected endpoints")
        void jwtGrantsAccess() {
            TestUser user = auth.registerVerifiedUser();

            as(user).get("/user/profile")
                    .then()
                    .statusCode(200)
                    .body("email", equalTo(user.email()));
        }

        @Test
        @DisplayName("'remember me' extends token lifetime from 24 hours to 30 days")
        void rememberMeExtendsTokenLifetime() {
            RegistrationPayload payload = aValidRegistration();
            auth.verify(auth.registerAndCaptureToken(payload)).then().statusCode(200);

            String regular = auth.login(payload.email(), payload.password()).path("accessToken");
            String remembered = anonymous()
                    .body(Map.of("email", payload.email(), "password", payload.password(), "rememberMe", true))
                    .post("/auth/login")
                    .then().statusCode(200)
                    .extract().path("accessToken");

            // iat/exp are second-precision and taken a few microseconds apart, hence the 1 s tolerance
            assertThat(lifetimeOf(regular).toSeconds())
                    .isBetween(Duration.ofHours(24).toSeconds(), Duration.ofHours(24).toSeconds() + 1);
            assertThat(lifetimeOf(remembered).toSeconds())
                    .isBetween(Duration.ofDays(30).toSeconds(), Duration.ofDays(30).toSeconds() + 1);
        }

        @Test
        @DisplayName("rejects a wrong password with 401")
        void rejectsWrongPassword() {
            TestUser user = auth.registerVerifiedUser();

            auth.login(user.email(), "WrongPassword1")
                    .then()
                    .statusCode(401)
                    .body("message", equalTo("Invalid email or password"));
        }

        @Test
        @DisplayName("rejects an unknown e-mail with the same 401 (no user enumeration)")
        void rejectsUnknownEmail() {
            auth.login(RegistrationPayload.uniqueEmail(), "Secret123")
                    .then()
                    .statusCode(401)
                    .body("message", equalTo("Invalid email or password"));
        }

        @Test
        @DisplayName("rejects an unverified account with 403")
        void rejectsUnverifiedUser() {
            RegistrationPayload payload = aValidRegistration();
            auth.register(payload.build()).then().statusCode(200);

            auth.login(payload.email(), payload.password())
                    .then()
                    .statusCode(403)
                    .body("message", containsString("verify your email"));
        }
    }

    private static String jwtClaim(String jwt, String claim) {
        String payloadJson = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
        return JsonPath.from(payloadJson).getString(claim);
    }

    private static Duration lifetimeOf(String jwt) {
        long issuedAt = Long.parseLong(jwtClaim(jwt, "iat"));
        long expiresAt = Long.parseLong(jwtClaim(jwt, "exp"));
        return Duration.ofSeconds(expiresAt - issuedAt);
    }
}
