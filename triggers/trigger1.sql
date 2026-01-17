-- Trigger 1: Automatically updates the updated_at field in user_media_library table
-- on each record modification
DELIMITER $$
CREATE TRIGGER before_user_media_update
BEFORE UPDATE ON user_media_library
FOR EACH ROW
BEGIN
    SET NEW.updated_at = NOW();
END$$
DELIMITER ;
