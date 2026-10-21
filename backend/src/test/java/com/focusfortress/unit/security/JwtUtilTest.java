package com.focusfortress.unit.security;

import com.focusfortress.model.User;
import com.focusfortress.security.JwtUtil;
import com.focusfortress.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("JwtUtil")
class JwtUtilTest {

    /** Same length as the secret shipped in application.properties.example (52 bytes, > 256 bit). */
    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-for-hs256!!";
    private static final String OTHER_SECRET = "a-completely-different-secret-key-also-long-enough!!";
    private static final String EMAIL = "jane@focusfortress.test";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET);

    /** Mirrors JwtUtil#getSigningKey so we can craft tokens the production code accepts. */
    private static Key keyFor(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(keyFor(SECRET)).build().parseClaimsJws(token).getBody();
    }

    private static UserPrincipal principal(String email) {
        User user = new User();
        user.setEmail(email);
        return new UserPrincipal(user);
    }

    @Nested
    @DisplayName("generateToken")
    class Generate {

        @Test
        @DisplayName("produces a compact three-part JWS with the email as subject")
        void subjectIsEmail() {
            String token = jwtUtil.generateToken(EMAIL);

            assertThat(token.split("\\.")).hasSize(3);
            assertThat(parse(token).getSubject()).isEqualTo(EMAIL);
            assertThat(jwtUtil.extractEmail(token)).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("signs with HS256")
        void usesHs256() {
            String header = new String(Base64.getUrlDecoder().decode(jwtUtil.generateToken(EMAIL).split("\\.")[0]),
                    StandardCharsets.UTF_8);

            assertThat(header).contains("\"alg\":\"HS256\"");
        }

        @ParameterizedTest(name = "rememberMe={0} -> lifetime {1}h")
        @CsvSource({"false, 24", "true, 720"})
        @DisplayName("sets expiration according to the remember-me flag")
        void expirationDependsOnRememberMe(boolean rememberMe, long expectedHours) {
            Claims claims = parse(jwtUtil.generateToken(EMAIL, rememberMe));

            long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

            // iat is truncated to whole seconds by the JWT spec, exp is not -> allow 1 s of slack
            assertThat(lifetimeMs).isCloseTo(Duration.ofHours(expectedHours).toMillis(), within(1_000L));
        }

        @Test
        @DisplayName("single-arg overload defaults to a regular (24h) session")
        void defaultOverloadIsNotRememberMe() {
            Claims claims = parse(jwtUtil.generateToken(EMAIL));

            long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
            assertThat(lifetimeMs).isLessThanOrEqualTo(Duration.ofHours(24).toMillis() + 1_000L);
        }

        @ParameterizedTest(name = "secret of {0} chars is rejected")
        @ValueSource(ints = {1, 16, 31})
        @DisplayName("refuses to sign with a secret shorter than 256 bits")
        void weakSecretIsRejected(int length) {
            JwtUtil weak = new JwtUtil("x".repeat(length));

            assertThatThrownBy(() -> weak.generateToken(EMAIL)).isInstanceOf(WeakKeyException.class);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class Validate {

        @Test
        @DisplayName("accepts a fresh token for the matching user")
        void validForOwner() {
            String token = jwtUtil.generateToken(EMAIL);

            assertThat(jwtUtil.validateToken(token, principal(EMAIL))).isTrue();
        }

        @Test
        @DisplayName("rejects a valid token presented for a different user")
        void invalidForOtherUser() {
            String token = jwtUtil.generateToken(EMAIL);

            assertThat(jwtUtil.validateToken(token, principal("someone.else@focusfortress.test"))).isFalse();
        }

        @Test
        @DisplayName("is case-sensitive on the subject")
        void subjectComparisonIsCaseSensitive() {
            String token = jwtUtil.generateToken(EMAIL);

            assertThat(jwtUtil.validateToken(token, principal(EMAIL.toUpperCase()))).isFalse();
        }

        @Test
        @DisplayName("throws ExpiredJwtException for an expired token (handled by JwtAuthenticationFilter)")
        void expiredTokenThrows() {
            Date past = new Date(System.currentTimeMillis() - Duration.ofHours(2).toMillis());
            String expired = Jwts.builder()
                    .setSubject(EMAIL)
                    .setIssuedAt(new Date(past.getTime() - Duration.ofHours(24).toMillis()))
                    .setExpiration(past)
                    .signWith(keyFor(SECRET), SignatureAlgorithm.HS256)
                    .compact();

            assertThatThrownBy(() -> jwtUtil.validateToken(expired, principal(EMAIL)))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("rejects a token signed with a different secret")
        void foreignSignatureRejected() {
            String foreign = new JwtUtil(OTHER_SECRET).generateToken(EMAIL);

            assertThatThrownBy(() -> jwtUtil.validateToken(foreign, principal(EMAIL)))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("rejects a token whose payload was tampered with (privilege escalation attempt)")
        void tamperedPayloadRejected() {
            String[] parts = jwtUtil.generateToken("attacker@focusfortress.test").split("\\.");
            String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ("{\"sub\":\"" + EMAIL + "\",\"iat\":1700000000,\"exp\":4102444800}")
                            .getBytes(StandardCharsets.UTF_8));
            String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

            assertThatThrownBy(() -> jwtUtil.validateToken(tampered, principal(EMAIL)))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("rejects a token whose signature was altered")
        void tamperedSignatureRejected() {
            String[] parts = jwtUtil.generateToken(EMAIL).split("\\.");
            char[] sig = parts[2].toCharArray();
            int middle = sig.length / 2;              // never the last char: its low bits may be padding
            sig[middle] = sig[middle] == 'A' ? 'B' : 'A';
            String tampered = parts[0] + "." + parts[1] + "." + new String(sig);

            assertThatThrownBy(() -> jwtUtil.validateToken(tampered, principal(EMAIL)))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("rejects an unsigned (alg=none) token")
        void unsignedTokenRejected() {
            String unsigned = Jwts.builder()
                    .setSubject(EMAIL)
                    .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                    .compact();

            assertThatThrownBy(() -> jwtUtil.validateToken(unsigned, principal(EMAIL)))
                    .isInstanceOf(JwtException.class);
        }

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {"not-a-jwt", "a.b.c", "header.payload"})
        @DisplayName("rejects malformed tokens")
        void malformedRejected(String garbage) {
            assertThatThrownBy(() -> jwtUtil.extractEmail(garbage)).isInstanceOf(MalformedJwtException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("rejects null/empty tokens")
        void nullOrEmptyRejected(String token) {
            assertThatThrownBy(() -> jwtUtil.extractEmail(token)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
