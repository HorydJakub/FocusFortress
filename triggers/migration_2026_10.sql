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

-- 2. Habits may still point to ownerless categories (the shared interest catalog or old global categories).
--    Give each such user an own copy of the category and subcategory and move the habits to it;
--    the shared catalog itself is left untouched.
INSERT INTO categories (name, icon, owner_id)
SELECT DISTINCT c.name, c.icon, h.user_id
FROM habits h
JOIN categories c ON c.id = h.category_id AND c.owner_id IS NULL
WHERE NOT EXISTS (SELECT 1 FROM categories own WHERE own.owner_id = h.user_id AND own.name = c.name);

INSERT INTO subcategories (name, icon, category_id)
SELECT DISTINCT s.name, s.icon, own.id
FROM habits h
JOIN subcategories s ON s.id = h.subcategory_id
JOIN categories c ON c.id = s.category_id AND c.owner_id IS NULL
JOIN categories own ON own.owner_id = h.user_id AND own.name = c.name
WHERE NOT EXISTS (SELECT 1 FROM subcategories x WHERE x.category_id = own.id AND x.name = s.name);

UPDATE habits h
JOIN subcategories s ON s.id = h.subcategory_id
JOIN categories c ON c.id = s.category_id AND c.owner_id IS NULL
JOIN categories own ON own.owner_id = h.user_id AND own.name = c.name
JOIN subcategories own_s ON own_s.category_id = own.id AND own_s.name = s.name
SET h.category_id = own.id, h.subcategory_id = own_s.id;

-- Habits with a category but without a subcategory
UPDATE habits h
JOIN categories c ON c.id = h.category_id AND c.owner_id IS NULL
JOIN categories own ON own.owner_id = h.user_id AND own.name = c.name
SET h.category_id = own.id;
