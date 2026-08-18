# Infrastructure Layer

## 네이밍
- `~JpaEntity` / `~JpaRepository` (Spring Data 인터페이스) / `~Mapper` / `~RepositoryImpl`

## JPA 엔티티
- `@NoArgsConstructor(access = PROTECTED)` + 생성자에 `@Builder`
- setter 금지. 의도가 드러나는 메서드로 (`updateBalance()`, `markAsRead()`)

## Mapper
- `toDomain()`은 반드시 도메인의 `reconstruct()`를 호출할 것
- `@Component`로 등록, `RepositoryImpl`에 주입

## RepositoryImpl
- `@Repository` + `@RequiredArgsConstructor`
- `domain`의 인터페이스를 구현 (의존성 역전)