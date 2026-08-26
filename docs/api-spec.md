# API Spec

CrewCheck 백엔드 REST API 명세.

> **2026-08-26 최종 컷오버**: 크루/관리자용 UI는 전부 Thymeleaf+htmx 서버 렌더링 페이지(`/login`, `/dashboard`, `/history`, `/mypage`, `/notifications`, `/admin/**`)로 이관 완료됐고, 이 페이지들은 세션 기반 인증(`SecurityConfig.webFilterChain`)을 쓴다. mate-front(Next.js)가 소비하던 JWT 기반 `/api/v1/auth`, `/api/v1/points`, `/api/v1/attendance`, `/api/v1/schedule`, `/api/v1/notifications` REST 엔드포인트와 `JwtAuthFilter`/`JwtProvider`, CORS 설정은 전부 제거됐다. 아래는 컷오버 이후에도 남아있는 API 표면 전체다.

---

## Push Subscriptions

`ApiResponse<T>` 래핑 사용(`{ "success": true, "data": {...}, "message": null }`). `/mypage` 페이지의 세션 인증으로 보호되며(`anyRequest().authenticated()`), CSRF 토큰이 필요하다 — `fragments/layout.html`의 `_csrf`/`_csrf_header` meta 태그를 헤더에 실어 보낸다 (`static/js/push.js` 참고).

### GET /push/vapid-public-key

구독 생성에 필요한 VAPID 공개키. 공개해도 무방한 값.

**응답** `200`
```json
{ "success": true, "data": "BGrlhl_iNNKsw...", "message": null }
```

### POST /push/subscribe

```json
{
  "endpoint": "https://fcm.googleapis.com/fcm/send/...",
  "p256dh": "BN4...",
  "auth": "5I3..."
}
```

**응답** `200` — `ApiResponse<Void>`

**멱등**: 동일 `endpoint`가 이미 있으면 아무것도 하지 않고 성공 반환.

### DELETE /push/subscribe

**쿼리**: `?endpoint={url}` (URL 인코딩 필수)
**응답** `200` — `ApiResponse<Void>`

---

## Admin — 배치 수동 실행 (`X-Admin-Key` 인증)

세션 인증이 아니라 `X-Admin-Key` 헤더 값 비교로만 검증하는 운영 도구. 실제 적립/만료/근무일 생성은 `@Scheduled` 잡(`PointGrantScheduler`, `PointExpiryScheduler`, `DailyWorkDayScheduler`)이 매일 자동 수행하며, 아래 엔드포인트는 배치 실패 시 curl 등으로 수동 재실행하는 용도다. `SecurityConfig.webFilterChain`에서 `/api/v1/admin/**`을 permitAll + CSRF 예외로 두어 세션/토큰 없이 호출 가능하다.

| 메서드 | 경로 | 동작 |
|---|---|---|
| POST | `/api/v1/admin/grant-points-all` | 미지급 근무일 일괄 적립 |
| POST | `/api/v1/admin/expire-points-all` | 만료 도래 포인트 일괄 소멸 |
| POST | `/api/v1/admin/generate-workdays` | 오늘 근무일 자동 생성 |

**헤더**: `X-Admin-Key: {ADMIN_SECRET_KEY}`
**응답** `200` — 평문 완료 메시지 / `403` — `"Forbidden"` (평문)

---

## Health

### GET /health

`HealthController`. 인증 불필요. 배포 헬스체크용.

---

## 알려진 결함 (정리 대상)

| 항목 | 현황 |
|---|---|
| Web Push 알림 아이콘 | `service-worker.js`가 참조하는 `/icons/icon-192.png`, `/icons/badge-72.png` 파일이 없다 (mate-front 시절부터 있던 기존 결함, 컷오버 시 그대로 포팅) |
