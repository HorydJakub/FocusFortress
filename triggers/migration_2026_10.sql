-- Migration for databases created before the 2026-10 test-suite fixes. Safe to run more than once.
-- 1. Start the backend once with the new code (Hibernate adds categories.owner_id).
-- 2. Run this script:
--      docker exec -i focusfortress-db mysql -u root -prootpassword focusfortress < triggers/migration_2026_10.sql
-- 3. Restart the backend, so Hibernate can add UNIQUE(habit_id, date) to habit_progress.
-- 4. Reinstall trigger2 (it now counts only active habits), see trigger_install_instruction.txt.

-- 0. Remove duplicate check-ins (same habit, same day) left by the old race condition
DELETE hp FROM habit_progress hp
JOIN habit_progress keep
  ON keep.habit_id = hp.habit_id AND keep.date = hp.date AND keep.id < hp.id;

-- 1. Drop the old global UNIQUE(name) index on categories
SET @old_index = (
    SELECT s.index_name
    FROM information_schema.statistics s
    WHERE s.table_schema = DATABASE()
      AND s.table_name = 'categories'
      AND s.non_unique = 0
      AND s.index_name <> 'PRIMARY'
    GROUP BY s.index_name
    HAVING COUNT(*) = 1 AND MAX(s.column_name) = 'name'
    LIMIT 1
);
SET @sql = IF(@old_index IS NULL, 'SELECT ''no old index to drop''',
              CONCAT('ALTER TABLE categories DROP INDEX `', @old_index, '`'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Give existing habit categories an owner: the user whose habits use them.
--    Categories without habits stay ownerless (shared interest catalog).
UPDATE categories c
JOIN (
    SELECT category_id, MIN(user_id) AS user_id
    FROM habits
    WHERE category_id IS NOT NULL
    GROUP BY category_id
) h ON h.category_id = c.id
SET c.owner_id = h.user_id
WHERE c.owner_id IS NULL;
