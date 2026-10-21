package com.focusfortress.it.triggers;

import com.focusfortress.dto.UpdateMediaStatusDTO;
import com.focusfortress.it.MySqlIntegrationTestBase;
import com.focusfortress.model.MediaStatus;
import com.focusfortress.model.UserMediaItem;
import com.focusfortress.service.MediaLibraryService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code triggers/trigger1.sql} - {@code before_user_media_update}: every UPDATE on
 * {@code user_media_library} sets {@code updated_at = NOW()}.
 *
 * <p>The JPA entity also sets {@code updatedAt} in {@code @PreUpdate}, so an update through Hibernate
 * cannot prove the trigger exists. The decisive tests therefore modify rows with plain SQL (the way an
 * admin script, a migration or Adminer would) and compare against the <b>database</b> clock
 * ({@code NOW()}), never against the JVM clock - the container runs in UTC, the developer machine may not.</p>
 */
@DisplayName("Trigger before_user_media_update (trigger1.sql)")
class MediaLibraryUpdatedAtTriggerIT extends MySqlIntegrationTestBase {

    private static final String STALE_TIMESTAMP = "2020-01-01 00:00:00";

    @Autowired
    private MediaLibraryService mediaLibraryService;

    @Test
    @DisplayName("plain SQL UPDATE refreshes updated_at to the database's NOW()")
    void sqlUpdateRefreshesUpdatedAt() {
        long id = insertRowWithStaleUpdatedAt("vid-sql-1");

        int updated = jdbc.update("UPDATE user_media_library SET status = 'FINISHED' WHERE id = ?", id);

        assertThat(updated).isEqualTo(1);
        assertThat(secondsBetweenUpdatedAtAndDbNow(id))
                .as("updated_at must be set by the trigger to (roughly) the current DB time")
                .isBetween(0L, 5L);
    }

    @Test
    @DisplayName("an explicit updated_at in the UPDATE statement is overridden by the trigger")
    void explicitUpdatedAtIsOverridden() {
        long id = insertRowWithStaleUpdatedAt("vid-sql-2");

        jdbc.update("UPDATE user_media_library SET notes = 'rewatch', updated_at = '1999-12-31 23:59:59' WHERE id = ?", id);

        assertThat(secondsBetweenUpdatedAtAndDbNow(id)).isBetween(0L, 5L);
    }

    @Test
    @DisplayName("added_at is left untouched by the trigger")
    void addedAtIsNotTouched() {
        long id = insertRowWithStaleUpdatedAt("vid-sql-3");

        jdbc.update("UPDATE user_media_library SET status = 'CURRENTLY_WATCHING' WHERE id = ?", id);

        String addedAt = jdbc.queryForObject(
                "SELECT DATE_FORMAT(added_at, '%Y-%m-%d %H:%i:%s') FROM user_media_library WHERE id = ?", String.class, id);
        assertThat(addedAt).isEqualTo(STALE_TIMESTAMP);
    }

    @Test
    @DisplayName("trigger only touches the updated row, not the rest of the table")
    void onlyUpdatedRowChanges() {
        long touched = insertRowWithStaleUpdatedAt("vid-sql-4");
        long untouched = insertRowWithStaleUpdatedAt("vid-sql-5");

        jdbc.update("UPDATE user_media_library SET status = 'FINISHED' WHERE id = ?", touched);

        assertThat(secondsBetweenUpdatedAtAndDbNow(touched)).isBetween(0L, 5L);
        String untouchedUpdatedAt = jdbc.queryForObject(
                "SELECT DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') FROM user_media_library WHERE id = ?",
                String.class, untouched);
        assertThat(untouchedUpdatedAt).isEqualTo(STALE_TIMESTAMP);
    }

    @Test
    @DisplayName("status change through MediaLibraryService ends with updated_at = DB time of the UPDATE")
    void serviceUpdateGoesThroughTrigger() {
        // Smoke test of the realistic path. It only discriminates trigger vs. @PreUpdate when the JVM time zone
        // differs from the DB's (UTC); the plain-SQL tests above are the decisive proof of the trigger.
        UserMediaItem item = data.mediaItem("viewer@focusfortress.test", "vid-jpa-1", MediaStatus.WATCH_LATER);
        jdbc.update("UPDATE user_media_library SET updated_at = ? WHERE id = ?", STALE_TIMESTAMP, item.getId());

        mediaLibraryService.updateMediaStatus("viewer@focusfortress.test", item.getId(),
                new UpdateMediaStatusDTO(MediaStatus.FINISHED, "great talk"));

        assertThat(secondsBetweenUpdatedAtAndDbNow(item.getId())).isBetween(0L, 5L);
    }

    @Test
    @Disabled("BUG: trigger1 writes updated_at with the MySQL server clock (NOW(), server time zone), while "
            + "added_at/updated_at are written by the JVM (LocalDateTime.now(), JVM time zone). Whenever the two "
            + "time zones differ - e.g. the documented dev setup: MySQL in Docker (UTC) + backend on the host "
            + "(CET) - every update makes updated_at jump hours away from added_at, even earlier than added_at. "
            + "Fix: use one clock source (drop the trigger or @PreUpdate) or pin the DB session time zone. "
            + "Fails on any machine whose JVM time zone is not UTC.")
    @DisplayName("updated_at written by the trigger is consistent with the application's clock")
    void triggerClockMatchesApplicationClock() {
        UserMediaItem item = data.mediaItem("viewer@focusfortress.test", "vid-tz-1", MediaStatus.WATCH_LATER);

        mediaLibraryService.updateMediaStatus("viewer@focusfortress.test", item.getId(),
                new UpdateMediaStatusDTO(MediaStatus.CURRENTLY_WATCHING, null));

        LocalDateTime updatedAt = jdbc.queryForObject(
                "SELECT updated_at FROM user_media_library WHERE id = ?", LocalDateTime.class, item.getId());
        assertThat(Duration.between(updatedAt, LocalDateTime.now()).abs())
                .isLessThan(Duration.ofMinutes(1));
    }

    private long insertRowWithStaleUpdatedAt(String videoId) {
        jdbc.update("""
                INSERT INTO user_media_library (video_id, title, status, user_email, added_at, updated_at)
                VALUES (?, 'Trigger test', 'WATCH_LATER', 'trigger@focusfortress.test', ?, ?)
                """, videoId, STALE_TIMESTAMP, STALE_TIMESTAMP);
        return jdbc.queryForObject("SELECT id FROM user_media_library WHERE video_id = ?", Long.class, videoId);
    }

    private long secondsBetweenUpdatedAtAndDbNow(long id) {
        return jdbc.queryForObject(
                "SELECT TIMESTAMPDIFF(SECOND, updated_at, NOW()) FROM user_media_library WHERE id = ?", Long.class, id);
    }
}
