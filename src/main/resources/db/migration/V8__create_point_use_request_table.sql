-- 사용(use) 요청 멱등성 키 저장 테이블.
-- 더블클릭/네트워크 재시도로 같은 idempotency_key가 다시 오면 UNIQUE 위반으로 감지해
-- FIFO 재차감 없이 기존 tx_id의 결과를 그대로 반환한다.
-- 수동 실행 필요 (Flyway 미적용):
-- psql "$DB_URL" -f src/main/resources/db/migration/V8__create_point_use_request_table.sql

CREATE TABLE point_use_request (
    id UUID PRIMARY KEY,
    crew_id UUID NOT NULL,
    idempotency_key UUID NOT NULL,
    tx_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_use_request_crew_idempotency UNIQUE (crew_id, idempotency_key)
);
