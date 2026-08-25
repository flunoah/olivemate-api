# DB Schema

PostgreSQL 17. 테이블명·컬럼명은 **snake_case 단수형**을 따른다 (`point_ledger`, `work_day`).

---

## ⚠️ 마이그레이션 운영 방식 (반드시 숙지)

**Flyway가 적용되어 있지 않다.** `db/migration/*.sql`은 자동 실행되지 않으며, 운영 DB에 **수동으로 실행**해야 한다.

```bash
psql "$DB_URL" -f src/main/resources/db/migration/V1__add_missing_indexes.sql
```

| 환경 | `ddl-auto` | 스키마 생성 방식 |
|---|---|---|
| `dev` | `update` | Hibernate가 엔티티를 보고 자동 생성·변경 |
| `prod` | `validate` | 자동 생성 안 함. 스키마가 엔티티와 다르면 **기동 실패** |

### 배포 순서 (엔티티 변경 시)

```
1. 마이그레이션 SQL 작성 (db/migration/V{n}__*.sql)
2. 운영 DB에 psql로 수동 실행     ← 이 단계를 빠뜨리면 배포 후 앱이 죽는다
3. 애플리케이션 배포
```

`prod`가 `validate`이므로, 새 테이블·컬럼이 있는 코드를 먼저 배포하면 `SchemaManagementException`으로 기동 자체가 실패한다.

### CONCURRENTLY 주의

`V1`의 인덱스는 `CREATE INDEX CONCURRENTLY`를 쓴다. 테이블 락을 피하기 위함이며, **트랜잭션 블록 안에서 실행할 수 없다.** `psql -1` 같이 파일 전체를 한 트랜잭션으로 묶는 옵션을 쓰면 실패한다.

---

## 테이블 목록

| 테이블 | 역할 | PK |
|---|---|---|
| `crew` | 크루 계정 | `crew_id` |
| `crew_schedule` | 소정 근무 요일 설정 (이력형) | `schedule_id` |
| `work_day` | 근무일 등록 기록 | `work_day_id` |
| `point_account` | 크루별 포인트 잔액 (집계) | `account_id` |
| `point_ledger` | 포인트 원장 (append-only) | `ledger_id` |
| `notification` | 인앱 알림 이력 | `id` |
| `push_subscription` | Web Push 구독 정보 | `id` |
| `product` | 올리브영 자사몰 상품 카탈로그 (크루용 캐시) | `product_id` |

모든 PK는 `UUID`이며 애플리케이션에서 생성한다 (DB 시퀀스 미사용).
`crew_id`로 논리적 연결은 하지만 **물리적 FK 제약은 걸지 않는다.**

---

## crew

크루 계정. 인증 주체.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `crew_id` | UUID | PK | |
| `login_id` | varchar(50) | NOT NULL, **UNIQUE** | 로그인 ID |
| `password_hash` | varchar | NOT NULL | BCrypt 해시 |
| `name` | varchar(50) | NOT NULL | 표시 이름 |
| `role` | varchar(10) | NOT NULL | `CREW` / `STUDENT` / `TEACHER` / `ADMIN` |
| `is_active` | boolean | NOT NULL | 비활성 계정 제외용 |
| `created_at` | timestamp | 자동 (`@CreatedDate`) | |

**인덱스**: `idx_crew_active_role (is_active, role)` — 활성 크루 전체 조회(배치)에서 사용

**주의**: `role`에 `STUDENT`, `TEACHER`가 포함되어 있으나 현재 크루 포인트 도메인에서는 사용하지 않는다. 권한 판단은 `ADMIN` 여부(`ROLE_ADMIN`)만 본다.

---

## crew_schedule

크루의 소정 근무 요일. **변경 시 기존 행을 수정하지 않고 비활성화 후 새 행을 추가**하는 이력형 구조.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `schedule_id` | UUID | PK | |
| `crew_id` | UUID | NOT NULL | |
| `days_of_week` | varchar | NOT NULL | **`"1,2,3"` CSV 문자열** |
| `start_date` | date | NOT NULL | 적용 시작일 |
| `end_date` | date | NULL 허용 | 종료일 (미종료면 NULL) |
| `is_active` | boolean | NOT NULL | 현재 유효한 스케줄 여부 |
| `created_at` | timestamp | 자동 | |

**인덱스**
- `idx_schedule_crew_active (crew_id, is_active)` — 특정 크루의 현재 스케줄 조회
- `idx_schedule_active (is_active)` — 전체 활성 스케줄 스캔 (주간 근무일 자동 생성 배치)

### ⚠️ `days_of_week` 요일 번호 규약

**서버는 1=월 ~ 7=일**, **JavaScript `Date.getDay()`는 0=일 ~ 6=토**로 서로 다르다.
프론트에서 반드시 변환해야 한다.

```typescript
// mate-front: 서버 → JS
const serverDayToJsDay = (d: number): number => (d === 7 ? 0 : d);
```

CSV 문자열로 저장하는 구조라 **요일 조건으로 SQL 검색이 불가능**하다. 애플리케이션에서 파싱해 필터링한다.

---

## work_day

근무일 등록 기록. 포인트 적립의 트리거.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `work_day_id` | UUID | PK | |
| `crew_id` | UUID | NOT NULL | |
| `work_date` | date | NOT NULL | 실제 근무한 날짜 |
| `point_granted` | boolean | NOT NULL, 기본 `false` | 적립 완료 여부 |
| `skipped` | boolean | NOT NULL, 기본 `false` | 결근 처리 여부 |
| `registered_at` | timestamp | 자동 | 등록 시각 |

**유니크 제약**: `uq_crew_work_date (crew_id, work_date)`
→ 같은 크루가 같은 날짜를 중복 등록하면 DB에서 차단. API는 409 `CONFLICT`("이미 등록된 근무일입니다")로 응답.

**인덱스**: `idx_workday_grant_scan (point_granted, skipped, work_date)`
→ 매일 01:00 적립 배치의 `findAllNotGranted(today)` 조회용. 이 인덱스가 없으면 풀스캔이 발생한다.

### 상태 전이

```
등록          point_granted=false, skipped=false
  ↓ 배치 실행
적립 완료      point_granted=true      (markPointGranted())
  
결근 처리      skipped=true            (markSkipped()) → 배치 대상에서 제외
```

---

## point_account

크루별 포인트 잔액. `point_ledger`의 집계값을 캐싱하는 성격.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `account_id` | UUID | PK | |
| `crew_id` | UUID | NOT NULL, **UNIQUE** | 크루당 1계좌 |
| `balance` | bigint | NOT NULL | 현재 사용 가능 잔액 |
| `version` | bigint | `@Version` | **낙관적 락** |
| `updated_at` | timestamp | 자동 (`@LastModifiedDate`) | |

### ⚠️ 낙관적 락 (`@Version`)

동시에 같은 크루의 포인트를 사용하려 하면(더블 클릭, 여러 탭 등), 나중 트랜잭션이 `OptimisticLockingFailureException`으로 실패한다. 이것이 **잔액 음수 방지의 핵심 방어선**이다.

- 프론트는 사용 버튼에 `disabled` 처리를 병행해 중복 요청을 줄인다.
- 이 예외는 현재 `GlobalExceptionHandler`의 catch-all(500)로 떨어진다. 사용자에게 "잠시 후 다시 시도해주세요"로 안내하려면 전용 핸들러 추가를 검토할 것.

---

## point_ledger

포인트 원장. **append-only 감사 로그**이며 시스템의 단일 진실 공급원(source of truth)이다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `ledger_id` | UUID | PK | |
| `crew_id` | UUID | NOT NULL | |
| `work_day_id` | UUID | NULL 허용 | `EARN`일 때만 존재 |
| `tx_id` | UUID | NULL 허용 | `USE` 1건이 여러 행으로 쪼개질 때 묶는 키 |
| `ledger_type` | varchar(10) | NOT NULL | `EARN` / `USE` / `EXPIRE` / `INIT` |
| `amount` | bigint | NOT NULL | 거래 금액 (항상 양수) |
| `remaining` | bigint | NOT NULL | **잔여 차감 가능액** (적립 원장만 의미 있음) |
| `granted_at` | timestamp | NOT NULL | 적립일(EARN) / 사용일(USE) |
| `expired_at` | timestamp | NULL 허용 | 만료 예정일. NULL이면 무기한 |
| `created_at` | timestamp | 자동, `updatable=false` | 실제 행 생성 시각 |
| `description` | varchar | NULL 허용 | 제품명 등 메모 |
| `brand` | varchar(100) | NULL 허용 | `USE` 원장에서, 자동완성으로 상품을 선택한 경우 그 상품의 브랜드를 비정규화 복사. 자유 입력이면 NULL. `history` 페이지의 "이번 달 브랜드별 소비" 집계에 쓰임(`product` 테이블과 물리적 FK 없음, 값만 복사) |

**인덱스**
- `idx_ledger_crew_type_expired (crew_id, ledger_type, expired_at)` — FIFO 차감 대상 조회, 만료 예정 합계
- `idx_ledger_crew_granted (crew_id, granted_at)` — 내역 조회
- `idx_ledger_tx (tx_id)` — 사용 취소 시 동일 거래 묶음 조회

### `amount` vs `remaining`

```
EARN 4,000P 적립       → amount=4000, remaining=4000
3,000P 사용 (FIFO 차감) → 위 행의 remaining=1000으로 UPDATE
                        + USE 행 신규 INSERT (amount=3000, remaining=0)
```

- `amount`는 **불변**. `remaining`만 변경된다.
- `USE`/`EXPIRE` 행의 `remaining`은 항상 0이며 의미 없음.
- **`remaining` 외 어떤 컬럼도 UPDATE 하지 않는다.** 이력 삭제도 금지 (예외: 당일 사용 취소 시 `deleteLedgersByTxId`).

### `granted_at`의 이중 의미

| ledger_type | `granted_at` 의미 |
|---|---|
| `EARN` / `INIT` | 포인트가 지급된 날 |
| `USE` | 포인트를 사용한 날 |
| `EXPIRE` | 소멸 처리된 시각 |

내역 조회 시 날짜 그룹핑은 `granted_at` 기준, 취소 가능 여부 판단은 `created_at` 기준(당일 여부)이다.

### `tx_id`로 묶이는 이유

3,000P를 사용할 때 잔여 1,000P + 2,000P 두 원장에서 차감되면 `USE` 행이 **2개** 생긴다. 이 둘은 같은 `tx_id`를 가지며, 사용 취소 시 `tx_id` 단위로 함께 되돌린다.

### FIFO 차감 대상 조건

```java
ledger_type IN ('EARN', 'INIT')
  AND remaining > 0
  AND (expired_at IS NULL OR expired_at > now)
ORDER BY COALESCE(expired_at, '9999-12-31')  -- 만료일 없는 건 맨 뒤로
```

---

## notification

인앱 알림 이력. Web Push 발송과 별개로 저장된다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | UUID | PK | |
| `crew_id` | UUID | NOT NULL | |
| `type` | varchar(50) | NOT NULL | `POINT_EARNED` / `POINT_EXPIRING` |
| `title` | varchar(200) | NOT NULL | |
| `body` | varchar(500) | NOT NULL | |
| `deep_link` | varchar(500) | NULL 허용 | 예: `/history?date=2026-08-11` |
| `read` | boolean | NOT NULL, 기본 `false` | |
| `sent_at` | timestamp | NOT NULL | |

**인덱스**: `idx_notification_crew_id (crew_id)`

> **네이밍 확인 필요**: `V2__create_notification_tables.sql`에는 복수형(`notifications`)으로 작성되어 있으나, 다른 테이블은 모두 단수형이다. 일관성을 위해 단수형으로 통일할지 결정할 것. 변경 시 `@Table(name = ...)`도 함께 수정하고 dev DB의 기존 테이블을 정리해야 한다.

---

## push_subscription

Web Push 구독 정보. 크루 1명이 여러 기기/브라우저를 가질 수 있어 **1:N**이다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | UUID | PK | |
| `crew_id` | UUID | NOT NULL | |
| `endpoint` | varchar(1000) | NOT NULL, **UNIQUE** | 브라우저 벤더가 발급한 푸시 수신 URL |
| `p256dh` | varchar(500) | NOT NULL | 암호화 공개키 |
| `auth` | varchar(500) | NOT NULL | 인증 시크릿 |
| `registered_at` | timestamp | NOT NULL | |

**인덱스**: `idx_push_subscription_crew_id (crew_id)`

### `endpoint` UNIQUE의 의미

같은 브라우저에서 재구독을 시도하면 동일한 `endpoint`가 오므로, `PushSubscriptionService.subscribe()`가 **`findByEndpoint`로 선확인 후 있으면 무시**하는 멱등 처리를 한다. 이 방어가 없으면 사용자가 "알림 켜기"를 두 번 누를 때 `DataIntegrityViolationException`이 발생한다.

발송 실패(410 Gone 등) 시 해당 행을 삭제해 죽은 구독을 정리한다.

---

## product

올리브영 자사몰 상품 카탈로그. 크롤링은 앱 바깥에서 별도로 이뤄지고 그 결과가 엑셀 파일로 전달되며, 어드민이 `POST /api/v1/admin/products/upload`로 업로드하면 `ProductSyncService`가 `goods_no` 기준 upsert로 이 테이블에 반영한다 (크루/포인트 도메인과 논리적 연결 없음, 독립 참조 테이블. `point_ledger.brand`로 소비 패턴 집계 시에만 값이 복사되어 쓰임).

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `product_id` | UUID | PK | |
| `goods_no` | varchar(50) | NOT NULL, **UNIQUE** | 상품코드. 엑셀 재업로드 시 upsert 판별 키 |
| `brand` | varchar(100) | NOT NULL | 브랜드명 |
| `name` | varchar(200) | NOT NULL | 상품명 |
| `regular_price` | bigint | NOT NULL | 정상가(원) |
| `sale_price` | bigint | NOT NULL | 판매가(원) — 포인트 사용 시 사용액 자동 입력값으로 쓰임 |
| `synced_at` | timestamp | NOT NULL | 마지막 엑셀 업로드 반영 시각 |

**인덱스**
- `idx_product_goods_no` — upsert 시 `goods_no` 조회
- `idx_product_name` — 자동완성 `ILIKE '%keyword%'` 검색. 카탈로그가 커지면 `pg_trgm` 도입 검토

**주의**: `point_ledger`와 물리적 FK 연결 없음. 크루가 자동완성으로 상품을 선택하면 `point_ledger.brand`에 브랜드명만 비정규화 복사되고, `product_id` 단위 추적이나 카테고리 집계는 하지 않는다 (`TODO.md` 참고).

---

## 테이블 관계도

```
crew (1) ──────┬── (1) point_account          [crew_id UNIQUE]
               ├── (N) point_ledger
               ├── (N) work_day               [uq: crew_id + work_date]
               ├── (N) crew_schedule          [is_active=true인 것이 현재 유효]
               ├── (N) notification
               └── (N) push_subscription      [endpoint UNIQUE]

work_day (1) ── (0..1) point_ledger           [work_day_id, EARN 타입만]
point_ledger ── tx_id로 USE 행들이 묶임
```

물리적 FK 제약은 걸지 않으므로, **참조 무결성은 애플리케이션이 보장**한다.

---

## 감사(Audit) 필드

`@EntityListeners(AuditingEntityListener.class)` + `MateApplication`의 `@EnableJpaAuditing`으로 자동 채워진다.

| 어노테이션 | 컬럼 | 적용 테이블 |
|---|---|---|
| `@CreatedDate` | `created_at` / `registered_at` | `crew`, `crew_schedule`, `work_day`, `point_ledger` |
| `@LastModifiedDate` | `updated_at` | `point_account` |

`@EnableJpaAuditing`이 빠지면 이 필드들이 **null로 저장**되므로 제거하지 말 것.

---

## 스키마 변경 체크리스트

- [ ] 엔티티 수정 (`@Column`, `@Index`, `@UniqueConstraint`)
- [ ] `db/migration/V{n}__*.sql` 작성
- [ ] SQL 상단에 수동 실행 안내 주석 추가 (Flyway 미적용 상태이므로)
- [ ] 인덱스 추가 시 `CREATE INDEX CONCURRENTLY` 사용 검토
- [ ] **운영 DB에 SQL 수동 실행**
- [ ] 애플리케이션 배포
- [ ] 이 문서 갱신