package com.focusfortress.unit.service;

import com.focusfortress.dto.UserRegistrationDTO;
import com.focusfortress.model.AuthProvider;
import com.focusfortress.model.Role;
import com.focusfortress.model.User;
import com.focusfortress.repository.UserRepository;
import com.focusfortress.service.EmailService;
import com.focusfortress.service.InterestService;
import com.focusfortress.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    private static final String EMAIL = "new.user@focusfortress.test";
    private static final Set<String> INTERESTS = Set.of("Meditation", "Physics", "Critical Thinking");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private InterestService interestService;

    @InjectMocks
    private UserService service;

    private static UserRegistrationDTO registration() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setName("Jane");
        dto.setEmail(EMAIL);
        dto.setDateOfBirth(LocalDate.of(1995, 3, 14));
        dto.setGender("Female");
        dto.setPassword("Secret123");
        dto.setConfirmPassword("Secret123");
        dto.setSelectedInterests(INTERESTS);
        return dto;
    }

    @Nested
    @DisplayName("registerUser")
    class Register {

        @Test
        @DisplayName("stores an UNVERIFIED local user with an encoded password and default timezone")
        void persistsUser() {
            when(passwordEncoder.encode("Secret123")).thenReturn("{bcrypt}hash");

            service.registerUser(registration());

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            User user = saved.getValue();
            assertThat(user.getName()).isEqualTo("Jane");
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 3, 14));
            assertThat(user.getGender()).isEqualTo("Female");
            assertThat(user.getPassword()).isEqualTo("{bcrypt}hash").isNotEqualTo("Secret123");
            assertThat(user.getRole()).isEqualTo(Role.UNVERIFIED);
            assertThat(user.isVerified()).isFalse();
            assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
            assertThat(user.getTimezone()).isEqualTo("Europe/Warsaw");
        }

        @Test
        @DisplayName("issues a random UUID verification token and e-mails exactly that token")
        void sendsVerificationToken() {
            when(passwordEncoder.encode(anyString())).thenReturn("hash");

            User user = service.registerUser(registration());

            String token = user.getVerificationToken();
            assertThatCode(() -> UUID.fromString(token)).doesNotThrowAnyException();
            verify(emailService).sendVerificationEmail(EMAIL, token);
        }

        @Test
        @DisplayName("generates a different token for every registration")
        void tokensAreUnique() {
            when(passwordEncoder.encode(anyString())).thenReturn("hash");

            String first = service.registerUser(registration()).getVerificationToken();
            String second = service.registerUser(registration()).getVerificationToken();

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("saves the user before creating interests and sending the e-mail")
        void orderOfSideEffects() {
            when(passwordEncoder.encode(anyString())).thenReturn("hash");

            User user = service.registerUser(registration());

            InOrder order = inOrder(userRepository, interestService, emailService);
            order.verify(userRepository).save(user);
            order.verify(interestService).createInitialInterestsForUser(user, INTERESTS);
            order.verify(emailService).sendVerificationEmail(EMAIL, user.getVerificationToken());
        }

        @Test
        @DisplayName("rejects an e-mail that is already registered without side effects")
        void duplicateEmail() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> service.registerUser(registration()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Email already exists!");
            verify(userRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder, interestService, emailService);
        }
    }

    @Nested
    @DisplayName("verifyUser")
    class Verify {

        @Test
        @DisplayName("promotes the user to USER and consumes the token")
        void verifies() {
            User user = new User("Jane", EMAIL, null, null, "hash", "token-123");
            when(userRepository.findByVerificationToken("token-123")).thenReturn(Optional.of(user));

            service.verifyUser("token-123");

            assertThat(user.getRole()).isEqualTo(Role.USER);
            assertThat(user.isVerified()).isTrue();
            assertThat(user.getVerificationToken()).isNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("rejects an unknown or already used token")
        void invalidToken() {
            when(userRepository.findByVerificationToken("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verifyUser("nope"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Invalid verification token");
            verify(userRepository, never()).save(any());
        }
    }
}
