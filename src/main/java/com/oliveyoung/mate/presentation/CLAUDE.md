# Presentation Layer

## 규칙
- Repository를 직접 호출하지 않는다. 반드시 `application`의 Service 경유.
- try-catch 금지. 도메인 예외를 그대로 던지고 `GlobalExceptionHandler`가 변환.
- 응답은 `ApiResponse.ok(data)`로 감싼다. (`success()` 아님)

## 인증
- 크루 ID: `CrewId.of(SecurityUtils.authenticatedCrewId())`
- 권한: `SecurityUtils.validateAdmin()` / `validateSelfOrAdmin(crewId)`
- 관리자 API는 `X-Admin-Key` 헤더 요구

## 에러 코드 매핑
| 예외 | 상태 | code |
|---|---|---|
| IllegalArgumentException | 400 | BAD_REQUEST |
| IllegalStateException | 409 | CONFLICT |
| DataIntegrityViolationException | 409 | CONFLICT |
| AccessDeniedException | 403 | ACCESS_DENIED |
| InsufficientPointException | 422 | INSUFFICIENT_POINT |ㅎ