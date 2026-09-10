# Presentation Layer

## 규칙
- Repository를 직접 호출하지 않는다. 반드시 `application`의 Service 경유.
- try-catch 금지. 도메인 예외를 그대로 던지고 `GlobalExceptionHandler`가 변환.
- 응답은 `ApiResponse.ok(data)`로 감싼다. (`success()` 아님)

## 인증
- 크루 ID: `CrewId.of(SecurityUtils.authenticatedCrewId())`
- 권한: `SecurityUtils.validateAdmin()` / `validateSelfOrAdmin(crewId)`
- 관리자 API는 `X-Admin-Key` 헤더 요구
- 필터 단(CSRF 실패, 컨트롤러 진입 전 권한 부족)은 `GlobalExceptionHandler`가 못 보는 영역 — `SecurityConfig`의 `accessDeniedHandler`가 처리. 세션 기반 CSRF라 세션 만료 시 토큰도 무효화되므로 CSRF 실패는 `/login?error=expired`로 안내

## 에러 코드 매핑
| 예외 | 상태 | code |
|---|---|---|
| IllegalArgumentException | 400 | BAD_REQUEST |
| IllegalStateException | 409 | CONFLICT |
| DataIntegrityViolationException | 409 | CONFLICT |
| AccessDeniedException | 403 | ACCESS_DENIED |
| InsufficientPointException | 422 | INSUFFICIENT_POINT |ㅎ