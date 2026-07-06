-- application.yml 각 모듈이 참조하는 데이터베이스 4개를 생성하고
-- 애플리케이션 계정(wkdwlsgh9622)에 전체 권한을 부여한다.
CREATE DATABASE IF NOT EXISTS memo_auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS memo_task_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS memo_chat_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS memo_notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON memo_auth_db.* TO 'wkdwlsgh9622'@'%';
GRANT ALL PRIVILEGES ON memo_task_db.* TO 'wkdwlsgh9622'@'%';
GRANT ALL PRIVILEGES ON memo_chat_db.* TO 'wkdwlsgh9622'@'%';
GRANT ALL PRIVILEGES ON memo_notification_db.* TO 'wkdwlsgh9622'@'%';

FLUSH PRIVILEGES;
