package com.focusfortress.api;

import com.focusfortress.api.support.ApiTestBase;
import com.focusfortress.api.support.TestUser;
import com.focusfortress.security.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static com.focusfortress.api.support.RegistrationPayload.uniqueEmail;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

@DisplayName("Security  - authentication of API calls")
class SecurityApiTest extends ApiTestBase {

    private static final String FOREIGN_SECRET = "some-other-secret-that-is-definitely-long-enough-for-hs256";

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Every way a caller can fail to present a valid identity. */
    enum InvalidCredentials {
        NO_AUTHORIZATION_HEADER,
        GARBAGE_BEARER_TOKEN,
        TOKEN_SIGNED_WITH_FOREIGN_KEY,
        EXPIRED_TOKEN,
        VALID_SIGNATURE_UNKNOWN_USER,
        BASIC_INSTEAD_OF_BEARER
    }

    @Nested
    @DisplayName("without a valid JWT")
    class WithoutValidJwt {

        @ParameterizedTest(name = "{0}")
        @EnumSource(InvalidCredentials.class)
        @DisplayName("request is never served with 2xx")
        void requestIsNotServed(InvalidCredentials credentials) {
            requestWith(credentials).get("/habits")
                    .then()
                    .statusCode(not(successful()));
        }

        @ParameterizedTest(name = "GET {0}")
        @ValueSource(strings = {"/habits", "/habits/tree", "/counters", "/categories",
                "/user/profile", "/user/interests", "/media/library", "/media/tiles"})
        @DisplayName("every protected endpoint refuses an anonymous caller")
        void everyProtectedEndpointRefusesAnonymous(String path) {
            anonymous().get(path)
                    .then()
                    .statusCode(not(successful()));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(InvalidCredentials.class)
        @DisplayName("API answers 401 Unauthorized")
        void respondsWith401(InvalidCredentials credentials) {
            requestWith(credentials).get("/habits")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("with a valid JWT")
    class WithValidJwt {

        @ParameterizedTest(name = "GET {0}")
        @ValueSource(strings = {"/habits", "/habits/tree", "/counters", "/categories",
                "/user/profile", "/user/interests", "/media/library", "/media/tiles"})
        @DisplayName("protected endpoint answers 200")
        void protectedEndpointServed(String path) {
            as(auth.registerVerifiedUser()).get(path).then().statusCode(200);
        }
    }

    @Nested
    @DisplayName("public endpoints")
    class PublicEndpoints {

        @ParameterizedTest(name = "GET {0}")
        @ValueSource(strings = {"/interests", "/interests/subcategories"})
        @DisplayName("are reachable without a token")
        void reachableAnonymously(String path) {
            anonymous().get(path).then().statusCode(200);
        }

        @Test
        @DisplayName("ignore a garbage token instead of failing")
        void garbageTokenIgnored() {
            withToken("not-a-jwt").get("/interests").then().statusCode(200);
        }
    }

    // ---- helpers ------------------------------------------------------------------------------

    private RequestSpecification requestWith(InvalidCredentials credentials) {
        return switch (credentials) {
            case NO_AUTHORIZATION_HEADER -> anonymous();
            case GARBAGE_BEARER_TOKEN -> withToken("not-a-jwt");
            case TOKEN_SIGNED_WITH_FOREIGN_KEY -> {
                TestUser user = auth.registerVerifiedUser();
                yield withToken(signedToken(user.email(), FOREIGN_SECRET, Duration.ofHours(1)));
            }
            case EXPIRED_TOKEN -> {
                TestUser user = auth.registerVerifiedUser();
                yield withToken(signedToken(user.email(), jwtSecret, Duration.ofMinutes(-1)));
            }
            case VALID_SIGNATURE_UNKNOWN_USER -> withToken(jwtUtil.generateToken(uniqueEmail()));
            case BASIC_INSTEAD_OF_BEARER -> {
                TestUser user = auth.registerVerifiedUser();
                yield anonymous().auth().preemptive().basic(user.email(), user.password());
            }
        };
    }

    private static String signedToken(String subject, String secret, Duration validFor) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(now - Duration.ofHours(2).toMillis()))
                .setExpiration(new Date(now + validFor.toMillis()))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private static org.hamcrest.Matcher<Integer> successful() {
        return allOf(greaterThanOrEqualTo(200), lessThan(300));
    }
}
