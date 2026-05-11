-- ─────────────────────────────────────────────────────────────
-- MediBook — Database Initialization
-- This file runs automatically when MySQL container starts fresh
-- ─────────────────────────────────────────────────────────────

CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS provider_db;
CREATE DATABASE IF NOT EXISTS schedule_db;
CREATE DATABASE IF NOT EXISTS appointment_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS review_db;
CREATE DATABASE IF NOT EXISTS notification_db;
CREATE DATABASE IF NOT EXISTS record_db;

-- Grant all permissions to the app user
GRANT ALL PRIVILEGES ON auth_db.* TO 'medibook_user'@'%';
GRANT ALL PRIVILEGES ON provider_db.* TO 'medibook_user'@'%';
GRANT ALL PRIVILEGES ON schedule_db.* TO 'medibook_user'@'%';
GRANT ALL PRIVILEGES ON appointment_db.* TO 'medibook_user'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'medibook_user'@'%';
GRANT ALL PRIVILEGES ON review_db.* TO 'medibook_user'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'medibook_user'@'%';
GRANT ALL PRIVILEGES ON record_db.* TO 'medibook_user'@'%';

FLUSH PRIVILEGES;