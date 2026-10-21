package com.focusfortress.api.support;

import com.focusfortress.service.YouTubeApiClient;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.RestAssuredConfig.config;

/**
 * Base class for black-box API tests.
 *
 * <p>Boots the full application on a random port against in-memory H2 (profile {@code test}).
 * All subclasses share one cached Spring context, so the expensive bits live here:
 * <ul>
 *     <li>a single GreenMail SMTP server on a dynamic port (captures verification e-mails),</li>
 *     <li>a Mockito mock of {@link YouTubeApiClient} so no test ever touches the network.</li>
 * </ul>
 *
 * <p>Tests isolate themselves with unique data (random e-mails / names) instead of
 * transactional rollback, which would not reach the server threads anyway.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class ApiTestBase {

    protected static final GreenMail GREEN_MAIL = startGreenMail();

    @MockitoBean
    protected YouTubeApiClient youTubeApiClient;

    @LocalServerPort
    private int port;

    protected AuthClient auth;

    private static GreenMail startGreenMail() {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP.dynamicPort());
        greenMail.start();
        Runtime.getRuntime().addShutdownHook(new Thread(greenMail::stop));
        return greenMail;
    }

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.port", () -> GREEN_MAIL.getSmtp().getPort());
    }

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
        // never follow redirects - an OAuth2 redirect must be visible to the test, not chased to Google
        RestAssured.config = config().redirect(redirectConfig().followRedirects(false));
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        auth = new AuthClient(GREEN_MAIL);
    }

    /** Request without credentials. */
    protected RequestSpecification anonymous() {
        return RestAssured.given().contentType(ContentType.JSON);
    }

    /** Request carrying {@code Authorization: Bearer <token>}. */
    protected RequestSpecification as(TestUser user) {
        return withToken(user.token());
    }

    protected RequestSpecification withToken(String token) {
        return anonymous().header("Authorization", "Bearer " + token);
    }

    // ---- shared fixtures (created through the API, never through repositories) ----

    protected long createCategory(TestUser user) {
        return createId(user, "/categories", Payloads.category(Payloads.unique("Category")));
    }

    protected long createSubcategory(TestUser user, long categoryId) {
        return createId(user, "/subcategories", Payloads.subcategory(Payloads.unique("Subcategory"), categoryId));
    }

    protected long createHabit(TestUser user, Map<String, Object> body) {
        return createId(user, "/habits", body);
    }

    protected long createId(TestUser user, String path, Map<String, Object> body) {
        return as(user).body(body)
                .when().post(path)
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }
}
