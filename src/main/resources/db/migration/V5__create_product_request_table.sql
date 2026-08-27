-- 수동 실행 필요 (Flyway 미적용):
-- psql "$DB_URL" -f src/main/resources/db/migration/V5__create_product_request_table.sql
-- CREATE INDEX CONCURRENTLY는 트랜잭션 블록 안에서 실행 불가 (psql -1 등으로 묶지 말 것)

CREATE TABLE product_request (
    id UUID PRIMARY KEY,
    crew_id UUID NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    brand VARCHAR(100),
    price BIGINT,
    note VARCHAR(500),
    linked_product_id UUID,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP
);
CREATE INDEX CONCURRENTLY idx_product_request_status ON product_request(status, created_at);
