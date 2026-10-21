package com.focusfortress.api.support;

/**
 * A registered, verified and logged-in user.
 *
 * @param email    unique e-mail generated for the test
 * @param password plain-text password used at registration
 * @param token    JWT access token obtained from {@code POST /api/auth/login}
 */
public record TestUser(String email, String password, String token) {
}
