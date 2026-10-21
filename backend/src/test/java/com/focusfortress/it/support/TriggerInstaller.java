package com.focusfortress.it.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Installs the production triggers from {@code triggers/*.sql} into the test container.
 *
 * <p>The files are installed <b>exactly as documented</b> in {@code triggers/trigger_install_instruction.txt}:
 * piped into the {@code mysql} command-line client inside the container, as the application user.
 * Using the CLI (instead of JDBC) means the {@code DELIMITER $$} syntax of the shipped scripts is
 * honoured verbatim - the tests exercise the files that ship, not a hand-edited copy.</p>
 *
 * <p>Must run after Hibernate created the schema (the triggers reference {@code users},
 * {@code habits} and {@code user_media_library}), i.e. after the Spring context started.
 * Idempotent: does nothing when both triggers already exist.</p>
 */
public final class TriggerInstaller {

    public static final String MEDIA_UPDATE_TRIGGER = "before_user_media_update";
    public static final String USER_DELETE_TRIGGER = "before_user_delete";

    private static final Path TRIGGERS_DIR = Path.of("triggers");
    private static final List<String> TRIGGER_FILES = List.of("trigger1.sql", "trigger2.sql");

    private TriggerInstaller() {
    }

    public static synchronized void ensureInstalled(MySQLContainer<?> mysql, JdbcTemplate jdbc) throws Exception {
        if (installedTriggerCount(jdbc) == 2) {
            return;
        }
        for (String file : TRIGGER_FILES) {
            Path script = TRIGGERS_DIR.resolve(file);
            if (!Files.exists(script)) {
                throw new IllegalStateException("Trigger script not found: " + script.toAbsolutePath()
                        + " (integration tests must run with the repository root as working directory)");
            }
            String target = "/tmp/" + file;
            mysql.copyFileToContainer(MountableFile.forHostPath(script), target);

            // drop first so a half-installed state can be repaired
            String trigger = file.equals("trigger1.sql") ? MEDIA_UPDATE_TRIGGER : USER_DELETE_TRIGGER;
            jdbc.execute("DROP TRIGGER IF EXISTS " + trigger);

            Container.ExecResult result = mysql.execInContainer("sh", "-c",
                    "mysql -u" + mysql.getUsername() + " -p" + mysql.getPassword() + " "
                            + mysql.getDatabaseName() + " < " + target);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Installing " + file + " failed: " + result.getStderr());
            }
        }
        int installed = installedTriggerCount(jdbc);
        if (installed != 2) {
            throw new IllegalStateException("Expected 2 triggers after installation, found " + installed);
        }
    }

    private static int installedTriggerCount(JdbcTemplate jdbc) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.TRIGGERS
                WHERE TRIGGER_SCHEMA = DATABASE() AND TRIGGER_NAME IN (?, ?)
                """, Integer.class, MEDIA_UPDATE_TRIGGER, USER_DELETE_TRIGGER);
        return count == null ? 0 : count;
    }
}
