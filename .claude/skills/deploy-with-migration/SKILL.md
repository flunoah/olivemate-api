---
name: deploy-with-migration
description: DB 스키마를 바꾸고 배포할 때 사용. "컬럼 추가해줘", "테이블 만들어줘", "엔티티에 필드 추가", "마이그레이션 작성해줘", "이거 배포하려면 뭐 해야 해?" 같은 요청에 해당. Flyway 미적용이라 SQL을 수동 실행해야 하고, 순서를 틀리면 prod 앱이 기동 실패한다.
---

# 스키마 변경 배포

## ⚠️ 순서를 반드시 지킬 것

prod는 `ddl-auto=validate`다. 스키마가 엔티티와 다르면 `SchemaManagementException`으로 **기동 자체가 실패**한다.

1. 마이그레이션 SQL 작성
2. 운영 DB에 수동 실행 ← 이걸 빠뜨리면 3번에서 앱이 죽는다
3. 애플리케이션 배포
4. docs/db-schema.md 갱신


## 1. SQL 작성

`src/main/resources/db/migration/V{n}__{설명}.sql`

상단에 수동 실행 안내 주석을 반드시 포함:
```sql
-- Flyway 미적용. 운영 DB에 수동 실행할 것:
--   psql "$DB_URL" -f V{n}__{설명}.sql
```

## 2. 수동 실행

```bash
psql "$DB_URL" -f src/main/resources/db/migration/V{n}__{설명}.sql
```

인덱스 추가 시 `CREATE INDEX CONCURRENTLY` 사용을 검토한다. 단, **트랜잭션 블록 안에서 실행 불가**하므로 `psql -1` 같은 옵션을 쓰면 안 된다.

## 3. 로컬 검증

```bash
export $(grep -v '^#' .env | xargs)
./gradlew build
```

## 4. 문서 갱신

`docs/db-schema.md`의 해당 테이블 표를 수정한다.