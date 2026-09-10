-- point_ledger.idx_ledger_crew_type_expired 누락분 추가
-- PointLedgerJpaEntity @Table(indexes=...)에는 선언돼 있으나 V1~V6 어디에도 CREATE INDEX가 없어
-- Flyway 미적용 + prod ddl-auto=validate 조합상 운영 DB에는 실제로 없을 가능성이 높음.
-- FIFO 차감(Point.use)과 야간 만료 배치(Point.expireOld)가 매번 이 컬럼 조합으로 정렬·필터링함.
-- Flyway 미적용 상태이므로 자동 실행되지 않음. 운영 DB에 수동으로 실행할 것:
--   psql "$DB_URL" -f V7__add_point_ledger_composite_index.sql
-- CONCURRENTLY: 인덱스 생성 중 테이블 락을 잡지 않음. 트랜잭션 블록(BEGIN/COMMIT) 안에서는
-- 실행할 수 없으므로, 파일을 통째로 하나의 트랜잭션으로 묶는 도구(-1 옵션 등)는 쓰지 말 것.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ledger_crew_type_expired
    ON point_ledger (crew_id, ledger_type, expired_at);
