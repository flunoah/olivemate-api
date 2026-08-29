# 🫒 MATE — 올리브영 자소 포인트 관리 시스템 (CrewCheck)

올리브영 매장 크루(직원)의 근무 등록과 자소(자기소개서) 포인트를 관리하는 풀스택 웹 애플리케이션입니다.
백엔드(Spring Boot)와 프론트엔드(Thymeleaf + htmx)가 **한 저장소, 한 서버**로 통합되어 있으며,
근무일을 등록하면 익일 자동으로 포인트가 적립되고 적립일 기준 31일 후 만료됩니다.

---

## ✨ 주요 기능

- 🗓 **근무일 등록 / 취소 / 결근 복원** — 소정근무일·연장근무 등록, 당일 취소, 결근 처리된 근무일 재등록 시 복원
- 💰 **포인트 자동 적립** — 근무일 등록 다음날 오전 1시(KST) 배치로 지급, 적립일 기준 31일 후 자동 만료
- 📦 **FIFO 포인트 소진** — 만료일이 가장 빠른 원장부터 우선 차감, 당일 사용 건에 한해 취소 가능
- 🔔 **Web Push 적립 알림** — VAPID 기반 웹 푸시로 포인트 적립/알림 발송, 알림 읽음 처리(optimistic update)
- 🛍 **상품 카탈로그 동기화** — 관리자가 엑셀 업로드로 상품 목록을 동기화(브랜드 추적용), 포인트 사용 시 상품명은 자유 입력
- 🛡 **어드민 화면** — 크루 등록/조회, 포인트 소급 지급, 근무일 강제 생성/취소/복원, 상품 업로드
- ⚡ **hx-boost 기반 SPA 감각의 서버 렌더링** — htmx로 하단 탭 전환 및 부분 갱신(풀 리로드 없음)

---

## 🛠 기술 스택

| 분류 | 사용 기술 |
|------|----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.7 |
| Frontend | Thymeleaf + htmx (서버 사이드 렌더링, 별도 프론트 저장소 없음) |
| ORM | Spring Data JPA / Hibernate |
| Auth | Spring Security 세션 기반 Form Login (`loginId`/`password`), BCrypt |
| Web Push | `nl.martijndwars:web-push` + BouncyCastle Provider (VAPID) |
| Database | PostgreSQL 17 |
| Build | Gradle |
| Deploy | Render |

> ⚠️ `jjwt` 의존성과 `JWT_SECRET`/`JWT_EXPIRE_MS` 설정이 남아있지만, 실제 인증은 JWT가 아니라 세션 기반 Form Login입니다(레거시 설정).

---

## 🚀 빠른 시작

### 1. 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성하세요.

```env
DB_URL=jdbc:postgresql://localhost:5432/mate
DB_USERNAME=postgres
DB_PASSWORD=postgres

# 레거시 설정 — 현재 인증 흐름에서는 사용되지 않지만 placeholder 해석을 위해 필요
JWT_SECRET=your-secret-key-32-chars-or-more
JWT_EXPIRE_MS=0

# 어드민 API(X-Admin-Key) 및 어드민 화면 인증 키
ADMIN_SECRET_KEY=mate-admin-secret-key

# Web Push (VAPID)
VAPID_PUBLIC_KEY=
VAPID_PRIVATE_KEY=

# 선택 — 미설정 시 비활성화
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
ANTHROPIC_API_KEY=

SPRING_PROFILES_ACTIVE=dev
```

### 2. 데이터베이스 생성

```sql
CREATE DATABASE mate;
```

### 3. 실행

```bash
# 환경변수 로드 후 실행
export $(grep -v '^#' .env | xargs) && ./gradlew bootRun
```

서버가 `http://localhost:8080`에서 시작되며, 로그인 화면(`/login`)부터 전체 화면을 이 서버 하나로 제공합니다.

---

## 📁 프로젝트 구조

레이어드 DDD 기반으로 설계했습니다. 도메인은 `crew`(크루/인증) · `attendance`(근무) · `point`(포인트) · `schedule`(근무 스케줄) · `notification`(Web Push) · `product`(상품 카탈로그)로 구성됩니다.

```
src/main/java/com/oliveyoung/mate/
├── presentation/
│   ├── web/               # Thymeleaf 페이지 컨트롤러 (로그인, 대시보드, 마이페이지, 어드민 등)
│   ├── AdminController.java       # /api/v1/admin/** — 배치성 어드민 API (X-Admin-Key)
│   ├── SecurityConfig.java        # 세션 기반 Form Login 설정
│   ├── GlobalExceptionHandler.java
│   ├── notification/              # PushSubscriptionController (/push/**)
│   ├── attendance/ · point/ · schedule/   # 각 도메인 Request DTO
│
├── application/           # UseCase(Service), Command, Result — crew/attendance/point/schedule/notification/product
├── domain/                # 핵심 비즈니스 로직, 도메인 모델·VO, Repository 인터페이스
└── infrastructure/        # JPA Entity, Repository 구현체, Web Push 발송, 상품 엑셀 파서 등

src/main/resources/templates/     # Thymeleaf 템플릿 (login, signup, dashboard, history, mypage, notifications, admin*)
```

---

## 🖥 주요 화면

| 경로 | 설명 |
|------|------|
| `GET /login`, `GET /signup` | 로그인 / 회원가입 |
| `GET /dashboard` | 홈 — 근무일 등록/취소, 포인트 사용/취소 |
| `GET /history` | 포인트 내역 조회, 사용 취소 |
| `GET /mypage` | 근무 스케줄(소정근무일) 설정 |
| `GET /notifications` | 알림 목록 |
| `GET /admin`, `/admin/crews/new`, `/admin/crews/{crewId}`, `/admin/products` | 어드민 — 크루 관리, 포인트 소급 지급, 상품 업로드 |

대부분의 등록/취소/사용 액션은 htmx로 해당 페이지 내 `POST`/`DELETE` 엔드포인트(예: `/dashboard/workdays/register`, `/dashboard/points/use`, `/history/points/cancel`)를 호출해 부분 갱신합니다.

---

## 📡 API (REST)

세션 인증 화면 외에, 배치·연동 목적의 순수 API는 다음이 전부입니다.

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| `GET` | `/health` | 헬스체크 | 없음 |
| `POST` | `/api/v1/admin/grant-points-all` | 전체 크루 포인트 소급 적립 | `X-Admin-Key` |
| `POST` | `/api/v1/admin/expire-points-all` | 전체 크루 포인트 만료 처리 | `X-Admin-Key` |
| `POST` | `/api/v1/admin/generate-workdays` | 근무일 배치 생성 | `X-Admin-Key` |
| `GET` | `/push/vapid-public-key` | VAPID 공개키 조회 | 세션 |
| `POST` | `/push/subscribe` | Web Push 구독 등록 | 세션 |
| `DELETE` | `/push/subscribe` | Web Push 구독 해제 | 세션 |

---

## 🔐 인증 방식

Thymeleaf 전 화면은 **Spring Security Form Login 세션 인증**을 사용합니다(JWT 아님).

```http
POST /login
loginId={사번}&password={비밀번호}
```

로그인 성공 시 역할에 따라 `/dashboard`(크루) 또는 `/admin`(관리자)으로 리다이렉트됩니다. `/api/v1/admin/**`은 세션 없이 `X-Admin-Key` 헤더만으로 호출되는 별도 인증 경로입니다.

---

## ⚙️ 환경별 설정

| 환경 | DDL | SQL 로그 |
|------|-----|---------|
| `dev` | `update` (자동 스키마 변경) | ON |
| `prod` | `validate` (검증만) | OFF |

> ⚠️ 프로덕션 배포 시 스키마 변경은 반드시 `resources/db/migration/*.sql` 수동 마이그레이션으로 처리하세요.

---

## 📊 포인트 정책

- **적립**: 근무일 등록 후 익일 오전 1시(KST) 배치로 지급
- **만료**: 적립일로부터 31일 후 자동 소멸
- **소진 방식**: FIFO — 만료일이 가장 빠른 원장부터 차감 (만료일 없는 원장은 맨 뒤)
- **사용 취소**: 당일 사용 건에 한해 `txId` 단위로 복원
- **원장 종류**: `INIT`(초기 지급) / `EARN`(적립) / `USE`(사용) / `EXPIRE`(소멸)
