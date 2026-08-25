-- 수동 실행 필요 (Flyway 미적용):
-- psql "$DB_URL" -f src/main/resources/db/migration/V4__add_brand_to_point_ledger.sql

ALTER TABLE point_ledger ADD COLUMN brand VARCHAR(100);
