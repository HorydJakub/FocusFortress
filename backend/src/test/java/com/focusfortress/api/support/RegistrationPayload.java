package com.focusfortress.api.support;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fluent builder for the {@code POST /api/auth/register} body.
 * Starts from a valid payload; tests override only what they care about.
 */
public final class RegistrationPayload {

    public static final String DEFAULT_PASSWORD = "Secret123";
    public static final List<String> DEFAULT_INTERESTS = List.of("Meditation", "Physics", "Reading");

    private final Map<String, Object> body = new LinkedHashMap<>();

    private RegistrationPayload() {
        body.put("name", "Test User");
        body.put("email", uniqueEmail());
        body.put("dateOfBirth", LocalDate.of(1995, 5, 17).toString());
        body.put("gender", "Prefer not to specify");
        body.put("password", DEFAULT_PASSWORD);
        body.put("confirmPassword", DEFAULT_PASSWORD);
        body.put("selectedInterests", DEFAULT_INTERESTS);
    }

    public static RegistrationPayload aValidRegistration() {
        return new RegistrationPayload();
    }

    public static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@focusfortress.test";
    }

    public RegistrationPayload with(String field, Object value) {
        body.put(field, value);
        return this;
    }

    public RegistrationPayload without(String field) {
        body.remove(field);
        return this;
    }

    public RegistrationPayload withPassword(String password) {
        return with("password", password).with("confirmPassword", password);
    }

    public String email() {
        return (String) body.get("email");
    }

    public String password() {
        return (String) body.get("password");
    }

    public Map<String, Object> build() {
        return new LinkedHashMap<>(body);
    }
}
