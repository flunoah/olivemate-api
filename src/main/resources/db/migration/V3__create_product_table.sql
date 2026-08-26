-- 수동 실행 필요 (Flyway 미적용):
-- psql "$DB_URL" -f src/main/resources/db/migration/V3__create_product_table.sql
-- CREATE INDEX CONCURRENTLY는 트랜잭션 블록 안에서 실행 불가 (psql -1 등으로 묶지 말 것)

CREATE TABLE product (
    product_id UUID PRIMARY KEY,
    goods_no VARCHAR(50) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    regular_price BIGINT NOT NULL,
    sale_price BIGINT NOT NULL,
    synced_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX CONCURRENTLY idx_product_goods_no ON product(goods_no);
CREATE INDEX CONCURRENTLY idx_product_name ON product(name);
