-- 수동 실행 필요 (Flyway 미적용):
-- psql "$DB_URL" -f src/main/resources/db/migration/V6__add_push_notification_channels.sql

ALTER TABLE push_subscriptions ADD COLUMN notify_point_earned BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE push_subscriptions ADD COLUMN notify_point_expiring BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE push_subscriptions ADD COLUMN notify_admin_adjusted BOOLEAN NOT NULL DEFAULT TRUE;
