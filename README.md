# 🫒 MATE — 올리브영 자소 포인트 관리 시스템

올리브영 자소(자기소개서) 포인트를 자동으로 적립하고 관리하는 백엔드 서버입니다.  
크루(직원)의 근무 등록, 포인트 FIFO 소진, 만료 처리까지 도메인 중심 설계로 구현했습니다.

---

## ✨ 주요 기능

- 🗓 **근무일 등록 / 취소** — 당일 연장 근무 등록 및 결근 처리
- 💰 **포인트 자동 적립** — 근무일 기준 익일 지급, 만료일 자동 계산
- 📦 **FIFO 포인트 소진** — 만료일이 가장 빠른 포인트부터 우선 차감
- ⏰ **자동 만료 처리** — 스케줄러로 기간 초과 포인트 일괄 소멸
- 🔐 **JWT 인증** — Access Token / Refresh Token 기반 인증
- 🛡 **어드민 API** — 크루 관리, 포인트 소급 적립, 근무일 조회

---

## 🛠 기술 스택

| 분류 | 사용 기술 |
|------|----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Database | PostgreSQL 17 |
| Build | Gradle |
| Deploy | Render |

---

## 🚀 빠른 시작

### 1. 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성하세요.

```env
DB_URL=jdbc:postgresql://localhost:5432/mate
DB_USERNAME=postgres
DB_PASSWORD=postgres

JWT_SECRET=your-secret-key-32-chars-or-more
JWT_EXPIRE_MS=0

SPRING_PROFILES_ACTIVE=dev
```

### 2. 데이터베이스 생성

```sql
CREATE DATABASE mate;
```

### 3. 실행

```bash
# 환경변수 로드 후 실행
export $(cat .env | xargs) && ./gradlew bootRun
```

서버가 `http://localhost:8080`에서 시작됩니다.

---

## 📁 프로젝트 구조

클린 아키텍처(레이어드 DDD) 기반으로 설계했습니다.

```
src/main/java/com/oliveyoung/mate/
├── presentation/          # Controller, Request/Response DTO, Security 설정
│   ├── auth/              # 로그인, 회원가입
│   ├── attendance/        # 근무일 등록/취소
│   ├── point/             # 포인트 사용, 내역 조회
│   └── schedule/          # 근무 요일 설정
│
├── application/           # UseCase (Service), Command, Result
│   ├── crew/
│   ├── attendance/
│   ├── point/
│   └── schedule/
│
├── domain/                # 핵심 비즈니스 로직, 도메인 모델, Repository 인터페이스
│   ├── crew/
│   ├── attendance/
│   ├── point/
│   └── schedule/
│
└── infrastructure/        # JPA Entity, Repository 구현체, JWT
    ├── crew/
    ├── attendance/
    ├── point/
    └── schedule/
```

---

## 📡 API 명세

### 인증

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/auth/register` | 회원가입 |
| `POST` | `/api/v1/auth/login` | 로그인 |
| `POST` | `/api/v1/auth/refresh` | 토큰 갱신 |

**로그인 요청 예시**
```json
POST /api/v1/auth/login
{
  "loginId": "292000",
  "password": "password123"
}
```

**응답 예시**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

### 포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/v1/points/balance/{crewId}` | 잔액 조회 |
| `GET` | `/api/v1/points/history/{crewId}` | 내역 조회 |
| `POST` | `/api/v1/points/use/{crewId}` | 포인트 사용 |
| `POST` | `/api/v1/points/cancel` | 사용 취소 (당일만) |
| `POST` | `/api/v1/points/initialize/{crewId}` | 초기 포인트 등록 |

**포인트 사용 요청 예시**
```json
POST /api/v1/points/use/{crewId}
{
  "amount": 3000,
  "description": "닥터자르트 시카페어 크림",
  "usedAt": "2026-06-24"
}
```

---

### 근무

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/attendance/register` | 근무일 등록 |
| `DELETE` | `/api/v1/attendance/cancel` | 근무 취소 |
| `GET` | `/api/v1/attendance/week/{crewId}` | 이번 주 근무일 조회 |

---

### 근무 스케줄

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/schedule` | 근무 요일 저장 |
| `GET` | `/api/v1/schedule/me/{crewId}` | 내 스케줄 조회 |

---

## 🔐 인증 방식

모든 API(인증 엔드포인트 제외)는 JWT Bearer 토큰과 어드민 키가 필요합니다.

```http
Authorization: Bearer {accessToken}
X-Admin-Key: {adminSecretKey}
```

---

## ⚙️ 환경별 설정

| 환경 | DDL | SQL 로그 |
|------|-----|---------|
| `dev` | `update` (자동 스키마 변경) | ON |
| `prod` | `validate` (검증만) | OFF |

> ⚠️ 프로덕션 배포 시 스키마 변경은 반드시 수동 마이그레이션으로 처리하세요.

---

## 📊 포인트 정책

- **적립**: 근무일 등록 다음날 지급
- **만료**: 적립일로부터 21일
- **소진 방식**: 만료일 빠른 순서(FIFO)
- **사용 취소**: 당일 사용 건에 한해 가능
