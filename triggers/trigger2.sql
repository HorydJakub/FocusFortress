-- Trigger 2: Validation before user deletion
-- Prevents deletion of a user who has active habits

DELIMITER $$

CREATE TRIGGER before_user_delete
BEFORE DELETE ON users
FOR EACH ROW
BEGIN
    DECLARE habit_count INT;

    -- Check if the user has any habits
    SELECT COUNT(*) INTO habit_count
    FROM habits
    WHERE user_id = OLD.id;

    -- If the user has habits, abort the delete operation
    IF habit_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot delete user with active habits. Delete all habits first.';
    END IF;
END$$

DELIMITER ;

