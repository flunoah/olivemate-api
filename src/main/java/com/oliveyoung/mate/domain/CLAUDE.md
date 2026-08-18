# Domain Layer

## 절대 규칙
- 이 패키지에 Spring·JPA 어노테이션을 넣지 않는다. 순수 자바만.
- 금액은 `Money` VO로만. `long` 직접 연산 금지.
- 비즈니스 규칙은 Service가 아닌 이 레이어의 객체 안에 둔다.

## 생성 vs 복원
- 신규: `create()`, `pointEarned()` 등 의도가 드러나는 이름
- DB 복원: `reconstruct()` — 안 쓰면 id/remaining이 새 값으로 덮어써짐

## 도메인 이벤트
- 애그리거트가 직접 `domainEvents.add(...)`
- `pullDomainEvents()`는 조회 후 clear (중복 발행 방지)

## Point 애그리거트 변경 추적
- `newLedgers`: 신규 원장 → batch INSERT 대상
- `dirtyLedgerIds`: remaining 변경분 → targeted UPDATE 대상