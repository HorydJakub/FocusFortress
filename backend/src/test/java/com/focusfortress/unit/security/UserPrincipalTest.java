package com.focusfortress.unit.security;

import com.focusfortress.model.Role;
import com.focusfortress.model.User;
import com.focusfortress.repository.UserRepository;
import com.focusfortress.security.CustomUserDetailsService;
import com.focusfortress.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPrincipal & CustomUserDetailsService")
class UserPrincipalTest {

    private static final String EMAIL = "jane@focusfortress.test";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private static User userWithRole(Role role) {
        User user = new User("Jane", EMAIL, null, null, "hash", null);
        user.setRole(role);
        return user;
    }

    @ParameterizedTest(name = "{0} -> authority {1}, enabled={2}")
    @CsvSource({
            "UNVERIFIED, ROLE_UNVERIFIED, false",
            "USER,       ROLE_USER,       true",
            "ADMIN,      ROLE_ADMIN,      true"
    })
    @DisplayName("maps the role to a ROLE_-prefixed authority and enables only verified users")
    void roleMapping(Role role, String expectedAuthority, boolean expectedEnabled) {
        UserPrincipal principal = new UserPrincipal(userWithRole(role));

        assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly(expectedAuthority);
        assertThat(principal.isEnabled()).isEqualTo(expectedEnabled);
    }

    @Test
    @DisplayName("a user without a role has no authorities")
    void nullRole() {
        assertThat(new UserPrincipal(userWithRole(null)).getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("exposes e-mail as username and the stored password hash")
    void credentials() {
        UserPrincipal principal = new UserPrincipal(userWithRole(Role.USER));

        assertThat(principal.getUsername()).isEqualTo(EMAIL);
        assertThat(principal.getPassword()).isEqualTo("hash");
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername wraps the stored user in a UserPrincipal")
    void loadsUser() {
        User user = userWithRole(Role.USER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(details).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) details).getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("loadUserByUsername fails for an unknown e-mail")
    void unknownUser() {
        when(userRepository.findByEmail("ghost@focusfortress.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost@focusfortress.test"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@focusfortress.test");
    }
}
