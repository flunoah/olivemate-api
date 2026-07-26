-- 성능 분석에서 확인된 누락 인덱스 추가 (풀스캔 -> 인덱스 스캔)
-- Flyway 미적용 상태이므로 자동 실행되지 않음. 운영 DB에 수동으로 실행할 것:
--   psql "$DB_URL" -f V1__add_missing_indexes.sql
-- CONCURRENTLY: 인덱스 생성 중 테이블 락을 잡지 않음. 트랜잭션 블록(BEGIN/COMMIT) 안에서는
-- 실행할 수 없으므로, 파일을 통째로 하나의 트랜잭션으로 묶는 도구(-1 옵션 등)는 쓰지 말 것.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ledger_crew_granted
    ON point_ledger (crew_id, granted_at);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ledger_tx
    ON point_ledger (tx_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_workday_grant_scan
    ON work_day (point_granted, skipped, work_date);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_crew_active_role
    ON crew (is_active, role);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_schedule_crew_active
    ON crew_schedule (crew_id, is_active);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_schedule_active
    ON crew_schedule (is_active);
