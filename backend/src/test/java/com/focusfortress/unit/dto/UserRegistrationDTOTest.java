package com.focusfortress.unit.dto;

import com.focusfortress.dto.UserRegistrationDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Registration rules expressed on the DTO: Bean Validation constraints (evaluated with a plain
 * {@link Validator}, no Spring context) and the cross-field helpers used by UserController.
 */
@DisplayName("UserRegistrationDTO registration rules")
class UserRegistrationDTOTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static UserRegistrationDTO valid() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setName("Jane");
        dto.setEmail("jane@focusfortress.test");
        dto.setDateOfBirth(LocalDate.of(1995, 3, 14));
        dto.setGender("Female");
        dto.setPassword("Secret123");
        dto.setConfirmPassword("Secret123");
        dto.setSelectedInterests(Set.of("Meditation", "Physics", "Critical Thinking"));
        return dto;
    }

    private static Set<String> violatedFields(UserRegistrationDTO dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static Set<String> interests(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> "Interest " + i).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("a complete, well-formed registration has no violations")
    void validRegistration() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Nested
    @DisplayName("password policy")
    class Password {

        @ParameterizedTest(name = "\"{0}\" is accepted")
        @ValueSource(strings = {"Secret123", "aA1aaaaa", "Very-Long-Passw0rd-With-Symbols!"})
        void accepted(String password) {
            UserRegistrationDTO dto = valid();
            dto.setPassword(password);

            assertThat(violatedFields(dto)).doesNotContain("password");
        }

        @ParameterizedTest(name = "\"{0}\" is rejected ({1})")
        @CsvSource({
                "Sec1a,         shorter than 8",
                "secret123,     no uppercase",
                "SECRET123,     no lowercase",
                "SecretPass,    no digit",
                "'        ',    blank"
        })
        void rejected(String password, String reason) {
            UserRegistrationDTO dto = valid();
            dto.setPassword(password);

            assertThat(violatedFields(dto)).as(reason).contains("password");
        }
    }

    @Nested
    @DisplayName("selected interests")
    class Interests {

        @ParameterizedTest(name = "{0} interests -> valid")
        @ValueSource(ints = {3, 5, 7})
        void withinRange(int count) {
            UserRegistrationDTO dto = valid();
            dto.setSelectedInterests(interests(count));

            assertThat(violatedFields(dto)).doesNotContain("selectedInterests");
        }

        @ParameterizedTest(name = "{0} interests -> invalid")
        @ValueSource(ints = {0, 2, 8})
        void outOfRange(int count) {
            UserRegistrationDTO dto = valid();
            dto.setSelectedInterests(interests(count));

            assertThat(violatedFields(dto)).contains("selectedInterests");
        }

        @ParameterizedTest
        @NullSource
        void missing(Set<String> none) {
            UserRegistrationDTO dto = valid();
            dto.setSelectedInterests(none);

            assertThat(violatedFields(dto)).contains("selectedInterests");
        }
    }

    @Nested
    @DisplayName("profile fields")
    class Profile {

        @ParameterizedTest(name = "name \"{0}\" -> invalid")
        @ValueSource(strings = {"", "J"})
        void nameTooShort(String name) {
            UserRegistrationDTO dto = valid();
            dto.setName(name);

            assertThat(violatedFields(dto)).contains("name");
        }

        @Test
        @DisplayName("name longer than 50 characters is invalid")
        void nameTooLong() {
            UserRegistrationDTO dto = valid();
            dto.setName("x".repeat(51));

            assertThat(violatedFields(dto)).contains("name");
        }

        @ParameterizedTest(name = "e-mail \"{0}\" -> invalid")
        @ValueSource(strings = {"", "plainaddress", "@no-local-part.com", "jane@@focusfortress.test"})
        void invalidEmail(String email) {
            UserRegistrationDTO dto = valid();
            dto.setEmail(email);

            assertThat(violatedFields(dto)).contains("email");
        }

        @Test
        @DisplayName("date of birth today or in the future is invalid")
        void dateOfBirthMustBePast() {
            UserRegistrationDTO today = valid();
            today.setDateOfBirth(LocalDate.now());
            UserRegistrationDTO future = valid();
            future.setDateOfBirth(LocalDate.now().plusDays(1));

            assertThat(violatedFields(today)).contains("dateOfBirth");
            assertThat(violatedFields(future)).contains("dateOfBirth");
        }

        @Test
        @DisplayName("date of birth is required")
        void dateOfBirthRequired() {
            UserRegistrationDTO dto = valid();
            dto.setDateOfBirth(null);

            assertThat(violatedFields(dto)).contains("dateOfBirth");
        }
    }

    @Nested
    @DisplayName("isPasswordMatching")
    class PasswordMatching {

        @ParameterizedTest(name = "password={0}, confirm={1} -> {2}")
        @CsvSource(value = {
                "Secret123, Secret123, true",
                "Secret123, secret123, false",
                "Secret123, 'Secret123 ', false",
                "Secret123, NULL,      false",
                "NULL,      Secret123, false",
                "NULL,      NULL,      false"
        }, nullValues = "NULL")
        void matching(String password, String confirm, boolean expected) {
            UserRegistrationDTO dto = valid();
            dto.setPassword(password);
            dto.setConfirmPassword(confirm);

            assertThat(dto.isPasswordMatching()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("isGenderValid")
    class Gender {

        @ParameterizedTest(name = "\"{0}\" -> valid")
        @ValueSource(strings = {"Male", "Female", "Prefer not to specify"})
        void allowed(String gender) {
            UserRegistrationDTO dto = valid();
            dto.setGender(gender);

            assertThat(dto.isGenderValid()).isTrue();
        }

        @Test
        @DisplayName("gender is optional")
        void optional() {
            UserRegistrationDTO dto = valid();
            dto.setGender(null);

            assertThat(dto.isGenderValid()).isTrue();
        }

        @ParameterizedTest(name = "\"{0}\" -> invalid")
        @ValueSource(strings = {"", "male", "FEMALE", "Other", " Male"})
        void rejected(String gender) {
            UserRegistrationDTO dto = valid();
            dto.setGender(gender);

            assertThat(dto.isGenderValid()).isFalse();
        }
    }
}
