-- ShedLock 분산락 스키마 (공식 권장 DDL).
-- 롤링 배포 중 신/구 인스턴스가 짧게 겹치는 순간 같은 @Scheduled 크론이 동시에 돌아
-- 포인트가 이중 지급되는 것을 방지한다 (PointGrantScheduler 등).
-- 수동 실행 필요 (Flyway 미적용):
-- psql "$DB_URL" -f src/main/resources/db/migration/V9__create_shedlock_table.sql

CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
