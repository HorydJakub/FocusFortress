package com.focusfortress.it.infrastructure;

import com.focusfortress.it.MySqlIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Test infrastructure: the suite really runs on MySQL 8.4 with the production triggers")
class DatabaseInfrastructureIT extends MySqlIntegrationTestBase {

    @Test
    @DisplayName("connects to MySQL 8.4 (same major/minor as docker-compose.yml), not H2")
    void runsAgainstMySql84() {
        String version = jdbc.queryForObject("SELECT VERSION()", String.class);
        assertThat(version).startsWith("8.4");
    }

    @Test
    @DisplayName("server allows non-root trigger creation (log_bin_trust_function_creators = 1)")
    void trustFunctionCreatorsIsEnabled() {
        Integer value = jdbc.queryForObject("SELECT @@GLOBAL.log_bin_trust_function_creators", Integer.class);
        assertThat(value).isEqualTo(1);
    }

    @Test
    @DisplayName("both triggers from triggers/*.sql are installed on the Hibernate-generated schema")
    void triggersAreInstalled() {
        var triggers = jdbc.queryForList("""
                SELECT CONCAT(TRIGGER_NAME, ':', ACTION_TIMING, ':', EVENT_MANIPULATION, ':', EVENT_OBJECT_TABLE)
                FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()
                """, String.class);
        assertThat(triggers).containsExactlyInAnyOrder(
                "before_user_media_update:BEFORE:UPDATE:user_media_library",
                "before_user_delete:BEFORE:DELETE:users");
    }
}
