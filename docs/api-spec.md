# API Spec

CrewCheck 백엔드 REST API 명세. Base URL: `/api/v1`

---

## ⚠️ 응답 포맷이 두 가지로 혼재되어 있다

| 패턴 | 사용 컨트롤러 | 응답 형태 |
|---|---|---|
| **직접 반환** | `PointController`, `AttendanceController`, `ScheduleController`, `AuthController`, `AdminController` | `{ "balance": 13100, ... }` |
| **`ApiResponse<T>` 래핑** | `NotificationController`, `PushSubscriptionController` | `{ "success": true, "data": {...}, "message": null }` |

기존 컨트롤러 대부분은 결과 객체를 그대로 반환하고, 알림 관련 컨트롤러만 `ApiResponse.ok(...)`로 감싼다.

**프론트 대응**: `res.data ?? res` 패턴으로 언래핑한다.

```typescript
const res2 = await res.json();
const data = res2?.data ?? res2;
```

> **정리 필요**: 신규 엔드포인트는 어느 쪽을 따를지 결정하고 통일할 것. 기존 API를 `ApiResponse`로 바꾸면 프론트 전 페이지를 수정해야 하므로, 당분간은 위 방어 패턴을 유지한다.

또한 **에러 응답은 항상 `ErrorResponse` 형태**로 일관된다.

```json
{ "code": "INSUFFICIENT_POINT", "message": "잔액 부족. balance=1000, requested=3000" }
```

---

## 인증

### 헤더

| 헤더 | 대상 | 비고 |
|---|---|---|
| `Authorization: Bearer {accessToken}` | 인증 필요한 모든 요청 | `authFetch`가 자동 첨부 |
| `X-Refresh-Token: {refreshToken}` | `/auth/refresh` 전용 | |
| `X-Admin-Key: {key}` | `/admin/grant-points-all`, `/expire-points-all`, `/generate-workdays` | 배치 수동 실행용 |

### 두 가지 관리자 인증 방식이 공존한다

`AdminController`는 엔드포인트마다 검증 방식이 다르다.

| 엔드포인트 | 검증 | 실패 시 |
|---|---|---|
| `/admin/grant-points-all`, `/expire-points-all`, `/generate-workdays` | `X-Admin-Key` 헤더 값 비교 | 403 `"Forbidden"` (평문) |
| `/admin/crews`, `/admin/workdays` | `SecurityUtils.validateAdmin()` (JWT의 `ROLE_ADMIN`) | 403 `ACCESS_DENIED` |

전자는 외부 크론/스크립트에서 호출하는 용도, 후자는 어드민 UI용이다.

### 권한 검증 규칙

- `validateSelfOrAdmin(crewId)`: 본인이거나 `ROLE_ADMIN`인 경우만 허용. 대부분의 크루 API에 적용.
- `validateAdmin()`: `ROLE_ADMIN`만 허용.
- `@PreAuthorize("hasRole('ADMIN')")`: `/points/grant/manual`에만 사용.

---

## Auth

### POST /api/v1/auth/register

회원가입. 인증 불필요.

```json
{ "loginId": "crew01", "password": "...", "name": "홍길동", "role": "CREW" }
```

**응답** `200` — `TokenResult`
```json
{ "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

### POST /api/v1/auth/login

```json
{ "loginId": "crew01", "password": "..." }
```

**응답** `200` — `TokenResult`

### POST /api/v1/auth/refresh

**헤더**: `X-Refresh-Token: {refreshToken}`
**Body**: 없음
**응답** `200` — `TokenResult` (새 accessToken + refreshToken)

---

## Points

### GET /api/v1/points/balance/{crewId}

잔액 및 집계 조회. `validateSelfOrAdmin`

**응답** `200` — `PointBalanceResult`
```json
{
  "balance": 13100,
  "expiringIn7Days": 4000,
  "expiringIn30Days": 8000,
  "monthlyEarned": 16000,
  "monthlyUsed": 14900,
  "monthlyExpiring": 1100
}
```

- `monthly*`는 **이번 달 1일 ~ 말일** 기준.
- `expiringIn*`는 **지금부터 N일 후**까지 만료 예정 합계.

### POST /api/v1/points/use/{crewId}

FIFO 차감. `validateSelfOrAdmin`

```json
{
  "amount": 3000,
  "description": "닥터자르트 시카페어 크림",
  "usedAt": "2026-08-10"
}
```

- `usedAt`이 `null`이면 현재 시각(KST) 사용.

**응답** `200` — `UsePointResult`
```json
{ "usedAmount": 3000, "balance": 10100 }
```

> **프론트 주의**: `DashboardPage`가 `data.ledgerId ?? data.id`로 취소용 ID를 찾지만, **`UsePointResult`에는 해당 필드가 없다.** 현재 되돌리기(Undo) 토스트가 뜨지 않고 일반 성공 토스트로 폴백된다. 되돌리기를 살리려면 `UsePointResult`에 `txId` 추가가 필요하다.

**에러**
| 상황 | 상태 | code |
|---|---|---|
| 잔액 부족 | 422 | `INSUFFICIENT_POINT` |
| 계좌 없음 | 404 | `POINT_ACCOUNT_NOT_FOUND` |
| 동시 사용 충돌 | 500 | `INTERNAL_ERROR` (낙관적 락, 전용 핸들러 미구현) |

### GET /api/v1/points/history/{crewId}

전체 원장 내역. `validateSelfOrAdmin`

**응답** `200` — `List<LedgerHistoryResult>`
```json
[{
  "ledgerType": "EARN",
  "amount": 4000,
  "remaining": 4000,
  "grantedAt": "2026-08-11T00:00:00",
  "expiredAt": "2026-08-31T00:00:00",
  "createdAt": "2026-08-11T01:00:03",
  "description": null
}]
```

`createdAt` 내림차순 정렬. `ledgerType`은 `INIT` / `EARN` / `USE` / `EXPIRE`.

### POST /api/v1/points/initialize/{crewId}

초기 포인트 등록. **크루당 최초 1회만.** `validateSelfOrAdmin`

```json
{ "amount": 173000 }
```

**응답** `200` — `"초기 포인트 등록 완료!"` (평문)
**에러**: 이미 등록됨 → `409 CONFLICT`

### POST /api/v1/points/cancel

당일 사용 건 취소. `validateSelfOrAdmin`

```json
{ "ledgerId": "uuid", "crewId": "uuid" }
```

**응답** `200` — `"포인트 사용이 취소됐습니다."` (평문)

**에러**
| 상황 | 상태 | code |
|---|---|---|
| 당일 건 아님 | 409 | `CONFLICT` ("당일 사용 건만 취소 가능합니다.") |
| USE 타입 아님 / 내역 없음 | 400 | `BAD_REQUEST` |
| 타인 내역 | 403 | `ACCESS_DENIED` |

> 하나의 사용이 여러 원장으로 쪼개진 경우, 같은 `txId`를 가진 행 전체가 함께 취소된다. 프론트는 `mergedIds`를 순회하며 각각 호출하고 있으나, 첫 호출에서 `txId` 단위로 전부 처리되므로 이후 호출은 실패한다.

### POST /api/v1/points/grant/manual

소급 적립. **`@PreAuthorize("hasRole('ADMIN')")`**

```json
{ "crewId": "uuid", "workDate": "2026-08-05" }
```

**응답** `200` — `"포인트 소급 적립 완료!"` (평문)

**에러**
| 상황 | 상태 |
|---|---|
| 근무일 없음 | 400 `BAD_REQUEST` |
| 이미 지급됨 / 결근 처리됨 | 409 `CONFLICT` |

---

## Attendance

### POST /api/v1/attendance/register

근무일 등록. `validateSelfOrAdmin`

```json
{ "crewId": "uuid", "workDate": "2026-08-12" }
```

**응답** `200` — `"근무일 등록 완료. 포인트는 내일 지급됩니다."` (평문)
**에러**: 중복 등록 → `409` (DB `uq_crew_work_date` 제약)

### GET /api/v1/attendance/week/{crewId}

이번 주 근무 현황. `validateSelfOrAdmin`

**응답** `200` — `List<AttendanceService.WorkDayStatus>`
```json
[{ "date": "2026-08-12", "skipped": false }]
```

> 프론트는 구 형식(문자열 배열 `["2026-08-12"]`)과 신 형식을 모두 처리하도록 방어 코드가 있다.

### PUT /api/v1/attendance/reinstate

결근 취소(복원). `validateSelfOrAdmin`

**쿼리**: `?crewId={uuid}&workDate=2026-08-12`
**응답** `200` — `"근무일 복원 완료."` (평문)

### DELETE /api/v1/attendance/cancel

결근 처리. `validateSelfOrAdmin`

**쿼리**: `?crewId={uuid}&workDate=2026-08-12`
**응답** `200` — `"결근 처리됐어요."` (평문)

호출자 ID가 `log.warn`으로 감사 로깅된다.

---

## Schedule

### POST /api/v1/schedule

소정 근무 요일 저장. `validateSelfOrAdmin`

```json
{
  "crewId": "uuid",
  "daysOfWeek": [3, 4, 5],
  "startDate": "2026-04-08",
  "endDate": null
}
```

**⚠️ 요일 번호는 1=월 ~ 7=일** (JS `Date.getDay()`의 0=일~6=토와 다름)

```typescript
// 프론트 변환 필수
const serverDayToJsDay = (d: number): number => (d === 7 ? 0 : d);
```

**응답** `200` — `"근무 요일이 저장됐어요!"` (평문)

기존 스케줄은 비활성화되고 새 행이 추가되는 **이력형** 구조.

### GET /api/v1/schedule/me/{crewId}

**응답** `200` — `ScheduleResult`
```json
{ "daysOfWeek": [3, 4, 5], "startDate": "2026-04-08" }
```

**`404`** — 스케줄 미설정 (`ResponseEntity.notFound()`, body 없음)

---

## Notifications

`ApiResponse<T>` 래핑 사용.

### GET /api/v1/notifications

**쿼리**: `?unreadOnly=true` (기본 `false`)

**응답** `200`
```json
{
  "success": true,
  "data": [{
    "id": "uuid",
    "type": "POINT_EARNED",
    "title": "포인트가 적립됐어요 🎉",
    "body": "어제 근무하신 4,000P가 적립됐어요.",
    "deepLink": "/points/history?date=2026-08-11",
    "read": false,
    "sentAt": "2026-08-11T01:00:05"
  }],
  "message": null
}
```

> **미사용**: 프론트에 인앱 알림 리스트 UI가 없어 현재 호출되지 않는다. Push 발송이 실패하면 사용자가 알림을 확인할 방법이 없다.

### PATCH /api/v1/notifications/{id}/read

**응답** `200` — `ApiResponse<Void>`

---

## Push Subscriptions

`ApiResponse<T>` 래핑 사용.

### GET /api/v1/push/vapid-public-key

구독 생성에 필요한 VAPID 공개키. 공개해도 무방한 값.

**응답** `200`
```json
{ "success": true, "data": "BGrlhl_iNNKsw...", "message": null }
```

### POST /api/v1/push/subscribe

```json
{
  "endpoint": "https://fcm.googleapis.com/fcm/send/...",
  "p256dh": "BN4...",
  "auth": "5I3..."
}
```

**응답** `200` — `ApiResponse<Void>`

**멱등**: 동일 `endpoint`가 이미 있으면 아무것도 하지 않고 성공 반환. 중복 클릭 시에도 에러가 나지 않는다.

### DELETE /api/v1/push/subscribe

**쿼리**: `?endpoint={url}` (URL 인코딩 필수)
**응답** `200` — `ApiResponse<Void>`

---

## Admin

### 배치 수동 실행 (`X-Admin-Key` 인증)

| 메서드 | 경로 | 동작 |
|---|---|---|
| POST | `/api/v1/admin/grant-points-all` | 미지급 근무일 일괄 적립 |
| POST | `/api/v1/admin/expire-points-all` | 만료 도래 포인트 일괄 소멸 |
| POST | `/api/v1/admin/generate-workdays` | 다음 주 근무일 자동 생성 |

**헤더**: `X-Admin-Key: {ADMIN_SECRET_KEY}`
**응답** `200` — 평문 완료 메시지 / `403` — `"Forbidden"` (평문, `ErrorResponse` 형태 아님)

> 이 세 엔드포인트는 스케줄러와 동일한 작업을 즉시 실행한다. **알림 기능 테스트 시 `grant-points-all`을 호출하면 배치를 기다리지 않고 Web Push를 검증할 수 있다.**

### GET /api/v1/admin/crews

활성 `CREW` 목록. `validateAdmin()`

**응답** `200`
```json
[{ "crewId": "uuid", "name": "홍길동", "loginId": "crew01", "role": "CREW" }]
```

### GET /api/v1/admin/workdays

**쿼리**: `?crewId={uuid}&from=2026-08-01&to=2026-08-31`

**응답** `200`
```json
[{ "workDate": "2026-08-12", "pointGranted": true, "skipped": false }]
```

---

## Health

### GET /api/v1/health

`HealthController`. 인증 불필요. 배포 헬스체크용.

---

## 에러 코드 전체

| code | 상태 | 발생 상황 |
|---|---|---|
| `INVALID_INPUT` | 400 | Bean Validation 실패 (필드명 포함 메시지) |
| `BAD_REQUEST` | 400 | `IllegalArgumentException` |
| `ACCESS_DENIED` | 403 | 타인 리소스 접근, 관리자 권한 없음 |
| `POINT_ACCOUNT_NOT_FOUND` | 404 | 포인트 계좌 미생성 |
| `NOT_FOUND` | 404 | 존재하지 않는 경로 |
| `CONFLICT` | 409 | 중복 등록, 당일 취소 불가, 이미 초기화됨 |
| `INSUFFICIENT_POINT` | 422 | 잔액 부족 |
| `INTERNAL_ERROR` | 500 | 그 외 (Telegram 알림 발송됨) |

### 프론트 에러 메시지 매핑

```typescript
function apiErrorMessage(status: number): string {
  if (status === 401) return "아이디 또는 비밀번호를 확인해주세요";
  if (status === 403) return "접근 권한이 없습니다";
  if (status === 409) return "이미 등록된 근무일입니다";
  if (status >= 500) return "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요";
  return "오류가 발생했습니다.";
}
```

> 409는 "중복 등록" 외에도 "당일 취소 불가", "이미 초기화됨" 등에 쓰이므로, 상황에 따라 잘못된 안내가 나갈 수 있다. `ErrorResponse.code`로 분기하는 것이 정확하다.

---

## 알려진 불일치 (정리 대상)

| 항목 | 현황 | 영향 |
|---|---|---|
| 응답 포맷 | `ApiResponse` 래핑 여부가 컨트롤러마다 다름 | 프론트가 `res.data ?? res`로 방어 |
| 성공 응답 타입 | 일부는 평문 문자열(`"등록 완료!"`) 반환 | JSON 파싱 시 주의 필요 |
| `UsePointResult` | `ledgerId`/`txId` 없음 | 프론트 Undo 기능 동작 불가 |
| 관리자 인증 | `X-Admin-Key`와 `ROLE_ADMIN` 혼용 | 신규 엔드포인트 추가 시 혼란 |
| 취소 API | `txId` 단위 처리인데 프론트는 `ledgerId`별 반복 호출 | 두 번째 호출부터 실패 |
| 알림 조회 API | 구현됐으나 프론트 미사용 | Push 실패 시 알림 유실 |