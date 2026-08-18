---
name: add-domain-feature
description: CrewCheck 백엔드에 새 기능(엔티티+API)을 추가할 때 사용. "쿠폰 기능 추가해줘", "출근 메모 API 만들어줘", "새 테이블이랑 CRUD 만들어줘", "리뷰 도메인 추가" 같은 요청에 해당. 레이어드 DDD 구조상 파일 8~10개를 정해진 순서로 만들어야 하며, 하나라도 빠지면 컴파일 에러가 연쇄 발생한다.
---

# 신규 도메인 기능 추가

## 생성 순서 (역방향 의존 순)

1. `domain/{도메인}/model/{Entity}.java` — 순수 자바. `create()` + `reconstruct()` 둘 다 정의
2. `domain/{도메인}/repository/{Entity}Repository.java` — 인터페이스
3. `infrastructure/{도메인}/persistence/{Entity}JpaEntity.java` — `@NoArgsConstructor(PROTECTED)` + `@Builder`
4. `infrastructure/{도메인}/persistence/{Entity}JpaRepository.java` — Spring Data
5. `infrastructure/{도메인}/persistence/{Entity}Mapper.java` — `@Component`, `toDomain()`은 반드시 `reconstruct()` 호출
6. `infrastructure/{도메인}/persistence/{Entity}RepositoryImpl.java` — `@Repository` + `@RequiredArgsConstructor`
7. `application/{도메인}/{Entity}Service.java`
8. `presentation/{도메인}/{Entity}Controller.java` + Request/Response DTO
9. `resources/db/migration/V{n}__create_{table}.sql`

## 체크리스트

- [ ] 도메인 모델에 `reconstruct()` 있는가 (없으면 DB 복원 시 id가 새로 생성됨)
- [ ] Repository **인터페이스와 구현체 양쪽** 모두 메서드를 추가했는가
- [ ] Mapper가 `@Component`인가
- [ ] 테이블명이 **단수형**인가 (`point_ledger`, `work_day` 컨벤션)
- [ ] 마이그레이션 SQL을 작성했는가 (Flyway 미적용 — 수동 실행 필요)
- [ ] 응답 포맷을 기존 컨트롤러와 맞췄는가 (`ApiResponse` 래핑 여부 확인)

## 검증

```bash
export $(grep -v '^#' .env | xargs)
./gradlew compileJava
```

## 레이어 두께 판단

- 금전·복잡한 규칙 → 도메인 모델 + Mapper + 이벤트 (풀 스택)
- 단순 CRUD → 도메인 모델 + Mapper (이벤트 생략 가능)
