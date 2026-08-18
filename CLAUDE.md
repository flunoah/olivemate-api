# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository

## Project Overview

CrewCheck는 올리브영 매장 크루(직원)를 위한 근무 등록 및 포인트 관리 풀스택 웹 애플리케이션이다.
크루가 근무일을 등록하면 익일 자동으로 포인트가 적립되고, 적립일 기준 21일 후 만료된다.
포인트 사용 시 만료가 임박한 것부터 FIFO로 차감되며, 당일 사용 건에 한해 취소할 수 있다.
현재 Web Push 기반 적립 알림 기능이 추가되어 있다.

## Critical Rules (절대 규칙)
- 프로덕션 DB에 직접 쿼리 금지 - 반드시 Staging 환경에서 먼저 테스트
- .env, credentials.json 등 시크릿 파일 절대 커밋 금지
- main 브랜치에 직접 push 금지 - 반드시 PR을 통해 머지
- 추측 금지 - VO(Money 등)/도메인 이벤트/Repository 커스텀 메서드/프로퍼티 키는 파일을 읽어 확인한 뒤에만 사용. 확인 안 되면 추측 코드 대신 필요한 파일을 요청할 것
- 파일 재생성 금지 - "메서드 추가" 지시는 기존 파일에 삽입. 새 파일 생성 전 `find src -name "{Class}.java"`로 중복 확인
- 시그니처 변경 시 `grep -rn "{변경명}" src/`로 참조처 전수 확인 후 전부 수정 + `./gradlew compileJava`로 검증
- 요청받지 않은 리팩토링 금지 - 발견한 개선점은 TODO.md에 제안만

## Definition of Done

| 작업 | 완료 조건 |
|---|---|
| 코드 작성 | `./gradlew compileJava` 통과 |
| 기능 구현 | `./gradlew build` 통과 (테스트 포함) |
| 스키마 변경 | 마이그레이션 SQL 작성 + 로컬 build 통과 + `docs/db-schema.md` 갱신 |
| API 변경 | 위 + `docs/api-spec.md` 갱신 |

## Tech Stack

- **Repo 구성**: 백엔드(`mate`)와 프론트엔드(`mate-front`)는 **별도 저장소**로 분리 관리. 백엔드는 Render 배포, 프론트는 `next.config.ts`의 `rewrites()`로 `/api/*`를 백엔드로 프록시.
- **Frontend**: Next.js 15 (App Router), React, TypeScript. 전 페이지 `"use client"`로 사실상 SPA (SSR 미사용). 스타일은 inline style 객체 (CSS 프레임워크 없음).
- **Backend**: Java 21, Spring Boot 4.0.7, Spring Data JPA / Hibernate, Lombok. Web Push는 `nl.martijndwars:web-push` + BouncyCastle Provider.
- **Database**: PostgreSQL 17 (dev: `ddl-auto=update` / prod: `ddl-auto=validate`). prod 스키마의 최종 소스는 `resources/db/migration/*.sql`.
- **Auth**: Spring Security + JWT (jjwt 0.12.6). Access Token은 `Authorization: Bearer`, 갱신은 `X-Refresh-Token` 헤더로 `/api/v1/auth/refresh` 호출. 관리자 API는 `X-Admin-Key` 헤더 추가 요구.

## Monorepo Commands

```bash
# 백엔드 (루트)
export $(grep -v '^#' .env | xargs)   # 필수. 생략 시 Hibernate Dialect 오류
./gradlew build
./gradlew bootRun
./gradlew compileJava                  # 빠른 컴파일 검증만

# 프론트엔드
cd mate-front && npm run dev
```

## Domain Context

| 정책 | 내용 |
|---|---|
| 적립 | 근무일 등록 후 **익일 오전 1시**(KST) 배치로 자동 지급 |
| 만료 | 적립일 기준 **21일 후** 자동 소멸 |
| 차감 | **FIFO** — 만료일이 가장 빠른 원장부터. 만료일 없는 원장은 맨 뒤로 |
| 취소 | **당일 사용 건만** 가능. `txId` 단위로 묶어 복원 |
| 원장 종류 | `INIT`(초기 지급) / `EARN`(적립) / `USE`(사용) / `EXPIRE`(소멸) |

- 근무일은 `소정근무일`(정기)과 `연장근무`(비정기)로 나뉜다. `skipped=true`면 결근 처리되어 적립되지 않는다.
- 모든 날짜·배치 기준 시각은 `ZoneId.of("Asia/Seoul")`.
- **요일 번호 규약이 서버와 프론트에서 다르다.** 서버는 1=월~7=일, JS `Date.getDay()`는 0=일~6=토. 프론트에서 `serverDayToJsDay()`로 변환 필수.

### File Processing Pipeline

이 저장소의 문서는 계층적으로 분리되어 있다. `CLAUDE.md`는 항상 로드되는 최소 컨텍스트이며, 상세 스펙은 필요할 때만 참조한다.

## Reference Docs

Detailed specs in
- `docs/api-spec.md` — 엔드포인트, 요청/응답 DTO, 에러 코드
- `docs/db-schema.md` — 테이블·컬럼 정의, 인덱스, 마이그레이션 운영 방식
- `docs/frontend-architecture.md` — 페이지 구조, `authFetch` 토큰 흐름, 컴포넌트
- `docs/architecture.md` — 레이어드 DDD, 도메인 이벤트, 트랜잭션 전략

**규칙**
- 작업과 무관한 문서는 읽지 않는다. 전체를 통독하지 말 것.
- 문서와 코드가 다르면 **코드가 정답**. 발견 시 문서 수정을 함께 제안할 것.
- 코드 변경 시 대응 문서도 같은 PR에서 갱신한다.
- `TODO.md` — 미해결 이슈 및 개선 백로그. 작업 완료 시 함께 갱신할 것