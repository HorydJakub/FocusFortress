package com.focusfortress.security;

import com.focusfortress.model.AuthProvider;
import com.focusfortress.model.Role;
import com.focusfortress.model.User;
import com.focusfortress.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // IMPORT THIS!
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user details from Google profile
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("picture");

        log.info("Google login success for email: {}", email);

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        boolean needsOnboarding = false;

        if (userOptional.isPresent()) {
            // User exists - update info
            user = userOptional.get();
            if (user.getProvider() == null || user.getProvider() == AuthProvider.LOCAL) {
                user.setProvider(AuthProvider.GOOGLE);
            }
            user.setAvatarUrl(avatarUrl);
            user.setLastLoginAt(LocalDateTime.now());

            // Check if user has selected interests.
            if (user.getInterests() == null || user.getInterests().isEmpty()) {
                needsOnboarding = true;
            }

            userRepository.save(user);
        } else {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setAvatarUrl(avatarUrl);
            user.setProvider(AuthProvider.GOOGLE);
            user.setRole(Role.USER);
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());
            user.setTimezone("Europe/Warsaw");
            userRepository.save(user);

            // New users always need onboarding
            needsOnboarding = true;

            log.info("Created new user from Google: {}", email);
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        // Redirect to Frontend with token and onboarding flag
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("token", token)
                .queryParam("onboarding", needsOnboarding)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}