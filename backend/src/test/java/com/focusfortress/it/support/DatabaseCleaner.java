package com.focusfortress.it.support;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Statement;
import java.util.List;

/**
 * Empties every application table before a test.
 *
 * <p>TRUNCATE (not DELETE) is used on purpose: it does not fire {@code before_user_delete},
 * so cleanup never trips the trigger under test. All statements run on one connection because
 * {@code FOREIGN_KEY_CHECKS} is a session variable.</p>
 */
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void truncateAllTables(JdbcTemplate jdbc) {
        List<String> tables = jdbc.queryForList("""
                SELECT TABLE_NAME FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
                """, String.class);

        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String table : tables) {
                    statement.execute("TRUNCATE TABLE `" + table + "`");
                }
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            return null;
        });
    }
}
