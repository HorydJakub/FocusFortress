package com.focusfortress.it;

import com.focusfortress.it.support.DatabaseCleaner;
import com.focusfortress.it.support.FixtureFactory;
import com.focusfortress.it.support.TriggerInstaller;
import com.focusfortress.service.YouTubeApiClient;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all integration tests that need a real MySQL 8.4.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li><b>Singleton container</b> - started once per JVM in a static block (no {@code @Container}),
 *       so every IT class reuses the same MySQL and the same cached Spring context.
 *       Ryuk removes the container when the JVM exits.</li>
 *   <li><b>Same image as docker-compose.yml</b> ({@code mysql:8.4}), started with
 *       {@code --log-bin-trust-function-creators=1} - the containerised equivalent of step 1 of
 *       {@code triggers/trigger_install_instruction.txt}, so the non-root app user may create triggers.</li>
 *   <li><b>One Spring context</b> - all configuration lives here; subclasses must not add their own
 *       {@code @MockitoBean}/properties, otherwise Spring builds a second context.</li>
 *   <li><b>No {@code @Transactional} on tests</b> - triggers fire on real statements and rollback tests
 *       need real commits. Isolation is achieved by truncating all tables before each test.</li>
 *   <li><b>GreenMail</b> on a dynamic port (no clash with other suites running in parallel).</li>
 *   <li><b>YouTube</b> is replaced by a Mockito mock - no network calls.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
public abstract class MySqlIntegrationTestBase {

    protected static final String DB_NAME = "focusfortress";
    protected static final String DB_USER = "focusfortress";
    protected static final String DB_PASSWORD = "focusfortress";

    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                    .withDatabaseName(DB_NAME)
                    .withUsername(DB_USER)
                    .withPassword(DB_PASSWORD)
                    .withCommand("--log-bin-trust-function-creators=1");

    protected static final GreenMail GREEN_MAIL =
            new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));

    static {
        MYSQL.start();
        GREEN_MAIL.start();
        Runtime.getRuntime().addShutdownHook(new Thread(GREEN_MAIL::stop));
    }

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.mail.port", () -> GREEN_MAIL.getSmtp().getPort());
    }

    @MockitoBean
    protected YouTubeApiClient youTubeApiClient;

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private ApplicationContext applicationContext;

    protected FixtureFactory data;

    @BeforeEach
    void resetInfrastructure() throws Exception {
        data = new FixtureFactory(applicationContext);
        TriggerInstaller.ensureInstalled(MYSQL, jdbc);
        DatabaseCleaner.truncateAllTables(jdbc);
        GREEN_MAIL.purgeEmailFromAllMailboxes();
        RestAssured.port = port;
        RestAssured.basePath = "";
    }
}
