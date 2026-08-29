# DB Schema

PostgreSQL 17. `ddl-auto=update`(dev)로 엔티티에서 스키마가 자동 생성되지만, **Flyway 미적용**이라 prod는 이 문서와 `src/main/resources/db/migration/*.sql`이 최종 소스다. 새 마이그레이션은 `V{n}__snake_case_description.sql`로 추가하고, prod 배포 전 `psql "$DB_URL" -f ...`로 수동 실행한다. `CREATE INDEX CONCURRENTLY`는 트랜잭션 블록으로 묶지 말 것(단일 문으로 실행).

## crew

| 컬럼 | 타입 | 비고 |
|---|---|---|
| crew_id | UUID PK | |
| login_id | VARCHAR(50) | UNIQUE |
| password_hash | VARCHAR | BCrypt |
| name | VARCHAR(50) | |
| role | VARCHAR(10) | `CREW`/`STUDENT`/`TEACHER`/`ADMIN` |
| is_active | BOOLEAN | |
| created_at | TIMESTAMP | |

인덱스: `idx_crew_active_role (is_active, role)`

## work_day

| 컬럼 | 타입 | 비고 |
|---|---|---|
| work_day_id | UUID PK | |
| crew_id | UUID | |
| work_date | DATE | |
| point_granted | BOOLEAN | |
| skipped | BOOLEAN | 결근 처리 |
| registered_at | TIMESTAMP | |

제약/인덱스: `uq_crew_work_date (crew_id, work_date)` UNIQUE, `idx_workday_grant_scan (point_granted, skipped, work_date)`

## crew_schedule

| 컬럼 | 타입 | 비고 |
|---|---|---|
| schedule_id | UUID PK | |
| crew_id | UUID | |
| days_of_week | VARCHAR | `"1,2,3"` 형식 문자열, 0=일~6=토 |
| start_date | DATE | |
| end_date | DATE | nullable |
| is_active | BOOLEAN | |
| created_at | TIMESTAMP | |

인덱스: `idx_schedule_crew_active (crew_id, is_active)`, `idx_schedule_active (is_active)`

## point_account

| 컬럼 | 타입 | 비고 |
|---|---|---|
| account_id | UUID PK | |
| crew_id | UUID | UNIQUE |
| balance | BIGINT | |
| version | BIGINT | 낙관적 락 |
| updated_at | TIMESTAMP | |

## point_ledger

| 컬럼 | 타입 | 비고 |
|---|---|---|
| ledger_id | UUID PK | |
| crew_id | UUID | |
| work_day_id | UUID | nullable — `EARN` 건만 값 있음 |
| tx_id | UUID | nullable — `USE` 건 묶음 단위(취소 시 이 값으로 복원) |
| ledger_type | VARCHAR(10) | `EARN`/`USE`/`EXPIRE`/`INIT` |
| amount | BIGINT | |
| remaining | BIGINT | FIFO 차감 대상(EARN/INIT만 갱신) |
| granted_at | TIMESTAMP | |
| expired_at | TIMESTAMP | nullable |
| created_at | TIMESTAMP | |
| description | VARCHAR | `USE` 건에 한해 상품명(자유 텍스트) |
| brand | VARCHAR(100) | nullable — `USE` 건에 한해 상품 자동완성 선택 시 채워짐 (`V4`) |

인덱스: `idx_ledger_crew_type_expired (crew_id, ledger_type, expired_at)`, `idx_ledger_crew_granted (crew_id, granted_at)`, `idx_ledger_tx (tx_id)`

## notifications

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | UUID PK | |
| crew_id | UUID | |
| type | VARCHAR(50) | `NotificationType` enum |
| title | VARCHAR(200) | |
| body | VARCHAR(500) | |
| deep_link | VARCHAR(500) | nullable |
| read | BOOLEAN | |
| sent_at | TIMESTAMP | |

인덱스: `idx_notifications_crew_id (crew_id)`

## push_subscriptions

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | UUID PK | |
| crew_id | UUID | |
| endpoint | VARCHAR(1000) | UNIQUE |
| p256dh | VARCHAR(500) | |
| auth | VARCHAR(500) | |
| registered_at | TIMESTAMP | |
| notify_point_earned | BOOLEAN | 기본 `TRUE` (`V6`) |
| notify_point_expiring | BOOLEAN | 기본 `TRUE` (`V6`) |
| notify_admin_adjusted | BOOLEAN | 기본 `TRUE` (`V6`) |

인덱스: `idx_push_subscriptions_crew_id (crew_id)`

## product (`V3`)

상품 자동완성/브랜드 표시용 카탈로그. 관리자가 엑셀로 업로드해 동기화한다(`goods_no` 기준 upsert).

| 컬럼 | 타입 | 비고 |
|---|---|---|
| product_id | UUID PK | |
| goods_no | VARCHAR(50) | UNIQUE — 엑셀의 상품코드 |
| brand | VARCHAR(100) | |
| name | VARCHAR(200) | |
| regular_price | BIGINT | |
| sale_price | BIGINT | |
| synced_at | TIMESTAMP | 마지막 업로드 반영 시각 |

인덱스: `idx_product_goods_no` UNIQUE, `idx_product_name` (`LIKE` 검색용)

## product_request (`V5`, 미사용)

크루가 상품 검색에서 찾지 못했거나(`NEW`) 매칭된 상품 정보가 틀렸을 때(`CORRECTION`) 보내는 등록·정정 요청. 관리자가 승인하면 `product` 테이블에 반영되는 구조였으나, 포인트 사용 시트를 자유 텍스트 입력으로 되돌리면서(2026-08-29) 관련 애플리케이션 코드를 전부 제거했다. **테이블은 드롭하지 않고 남겨둠** — 드롭 여부는 별도 승인 필요.

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | UUID PK | |
| crew_id | UUID | |
| request_type | VARCHAR(20) | `NEW` / `CORRECTION` |
| product_name | VARCHAR(200) | |
| brand | VARCHAR(100) | nullable |
| price | BIGINT | nullable — 모르면 승인 시 0원으로 임시 등록 |
| note | VARCHAR(500) | nullable |
| linked_product_id | UUID | nullable — `CORRECTION`일 때 대상 `product.product_id` |
| status | VARCHAR(20) | `PENDING` / `APPROVED` / `REJECTED` |
| created_at | TIMESTAMP | |
| reviewed_at | TIMESTAMP | nullable |

인덱스: `idx_product_request_status (status, created_at)`

## 마이그레이션 이력

| 파일 | 내용 |
|---|---|
| V1 | 성능 분석에서 확인된 누락 인덱스 6종 추가 |
| V2 | `notifications`, `push_subscriptions` 테이블 신설 |
| V3 | `product` 테이블 신설 |
| V4 | `point_ledger.brand` 컬럼 추가 |
| V5 | `product_request` 테이블 신설 |
| V6 | `push_subscriptions`에 채널별 알림 토글 컬럼 3종 추가 |
