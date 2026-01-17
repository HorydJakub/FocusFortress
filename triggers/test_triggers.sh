#!/bin/bash

# Script to check and test MySQL triggers in Docker
# Run with: ./triggers/test_triggers.sh (from project root)

echo "======================================"
echo "CHECKING INSTALLED TRIGGERS"
echo "======================================"
docker exec focusfortress-db mysql -u focusfortress -pfocusfortress focusfortress -e "SHOW TRIGGERS\G" 2>&1 | grep -v "Warning"

echo ""
echo "======================================"
echo "TEST 1: before_user_media_update"
echo "======================================"

docker exec focusfortress-db mysql -u focusfortress -pfocusfortress focusfortress << 'EOF' 2>&1 | grep -v "Warning"
INSERT INTO user_media_library (video_id, title, status, user_email, added_at, updated_at)
VALUES ('test1', 'Test', 'TO_WATCH', 'test@test.com', NOW(), NOW());

SELECT 'BEFORE UPDATE:' as '';
SELECT video_id, status, updated_at FROM user_media_library WHERE video_id = 'test1';

DO SLEEP(2);
UPDATE user_media_library SET status = 'WATCHED' WHERE video_id = 'test1';

SELECT 'AFTER UPDATE (updated_at should be newer):' as '';
SELECT video_id, status, updated_at FROM user_media_library WHERE video_id = 'test1';

DELETE FROM user_media_library WHERE video_id = 'test1';
EOF

echo ""
echo "======================================"
echo "TEST 2: before_user_delete"
echo "======================================"
echo "Creating user with habit and trying to delete..."
echo ""

docker exec focusfortress-db mysql -u focusfortress -pfocusfortress focusfortress << 'EOF' 2>&1 | grep -v "Warning"
INSERT INTO users (name, email, password, role, created_at)
VALUES ('Test', 'test@test.com', 'pass', 'USER', NOW());

SET @user_id = LAST_INSERT_ID();

INSERT INTO habits (name, duration_days, user_id, done)
VALUES ('Test Habit', 30, @user_id, false);

DELETE FROM users WHERE id = @user_id;
EOF

if [ $? -ne 0 ]; then
    echo ""
    echo "Trigger is working: User deletion was prevented due to existing habits."
fi

echo ""
echo "Cleaning up..."
docker exec focusfortress-db mysql -u focusfortress -pfocusfortress focusfortress -e "
DELETE FROM habits WHERE user_id = (SELECT id FROM users WHERE email = 'test@test.com');
DELETE FROM users WHERE email = 'test@test.com';
" 2>&1 | grep -v "Warning"

echo ""
echo "======================================"
echo "TESTS COMPLETED"
echo "======================================"

