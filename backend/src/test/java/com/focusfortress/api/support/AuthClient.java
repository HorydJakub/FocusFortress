package com.focusfortress.api.support;

import com.icegreen.greenmail.util.GreenMail;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.mail.internet.MimeMessage;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Drives the real authentication flow over HTTP: register -> e-mail -> verify -> login.
 * The verification token is read from the e-mail captured by GreenMail, exactly as a user would get it.
 */
public final class AuthClient {

    private static final Pattern TOKEN_IN_LINK =
            Pattern.compile("/api/auth/verify\\?token=([0-9a-fA-F-]{36})");

    private final GreenMail greenMail;

    AuthClient(GreenMail greenMail) {
        this.greenMail = greenMail;
    }

    public Response register(Map<String, Object> body) {
        return RestAssured.given().contentType(ContentType.JSON).body(body).post("/auth/register");
    }

    public Response verify(String token) {
        return RestAssured.given().queryParam("token", token).get("/auth/verify");
    }

    public Response login(String email, String password) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .post("/auth/login");
    }

    /** Registers a user and returns the verification token found in the e-mail sent to them. */
    public String registerAndCaptureToken(RegistrationPayload payload) {
        register(payload.build()).then().statusCode(200);
        return verificationTokenFor(payload.email());
    }

    /** Full happy path; returns a user ready to call protected endpoints. */
    public TestUser registerVerifiedUser(RegistrationPayload payload) {
        String verificationToken = registerAndCaptureToken(payload);
        verify(verificationToken).then().statusCode(200);
        String jwt = login(payload.email(), payload.password())
                .then().statusCode(200).body("accessToken", notNullValue())
                .extract().path("accessToken");
        return new TestUser(payload.email(), payload.password(), jwt);
    }

    public TestUser registerVerifiedUser() {
        return registerVerifiedUser(RegistrationPayload.aValidRegistration());
    }

    /** Extracts the verification token from the latest e-mail delivered to {@code email}. */
    public String verificationTokenFor(String email) {
        MimeMessage message = Arrays.stream(greenMail.getReceivedMessagesForDomain(email))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No e-mail was sent to " + email));
        String body;
        try {
            body = message.getContent().toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read e-mail sent to " + email, e);
        }
        Matcher matcher = TOKEN_IN_LINK.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Verification link not found in e-mail sent to " + email);
        }
        return matcher.group(1);
    }
}
