# TODO

> 상세 배경은 `docs/architecture.md`, `docs/api-spec.md`, `mate-front/docs/frontend-architecture.md` 참고

## 🔴 High — 실사용자에게 영향 있음

- [ ] `[FE]` `proxy.ts` → `middleware.ts` 리네임 + 함수명 변경. 현재 라우트 가드 미작동
- [ ] `[FE]` `next.config.ts` rewrites를 `/api/v1/:path*`로 좁히기. `/api/bugs`가 백엔드로 넘어가 404
- [ ] `[FE]` 사용 취소 중복 호출 제거. 백엔드는 `txId` 단위 처리라 2번째부터 400 → "취소 시간 지남"으로 오표시
- [ ] `[BE]` `WebPushClient`의 `catch (Exception)` 세분화. 일시적 오류에도 구독을 영구 삭제 중

## 🟡 Medium

- [ ] `[BE]` `UsePointResult`에 `txId` 추가 → 프론트 되돌리기 UI 활성화
- [ ] `[FE]` 되돌리기 UI 동작 확인 (위 작업 후)
- [ ] `[FE]` 하단 네비 `<a>` → `next/link`. 탭 전환마다 전체 리로드 중
- [ ] `[FE]` 알림 딥링크 `?date=` 파라미터 처리 (`useSearchParams`)
- [ ] `[BE]` `OptimisticLockingFailureException` 전용 핸들러. 현재 500으로 떨어짐
- [ ] `[BE]` `notification`/`push_subscription` 테이블명 단수형 통일 검토
- [ ] `[FE]` `next.config.ts` 백엔드 주소를 `API_BASE_URL` 환경변수로 분리

## 🟢 Low

- [ ] `[BE]` FIFO 로직 단위 테스트 (`Point.use()`, `cancelUse()`, `expireOld()`)
- [ ] `[FE]` 공통 컴포넌트 추출 (`Toast`, `Card`, `Button`)
- [ ] `[FE]` 컬러 토큰 상수화
- [ ] `[FE]` `NEXT_PUBLIC_ADMIN_KEY` 사용 재검토 (브라우저 번들 노출)
- [ ] `[BE]` 응답 포맷 통일 (`ApiResponse` 래핑 여부)
- [ ] `[FE]` ESLint 도입 (`lint` 스크립트 부재)
- [ ] CI 파이프라인 확장 — `./gradlew build`(테스트 포함, 시크릿 주입 필요) + `npm run build`(프론트)

## 📦 Backlog — 신규 기능

- [ ] 소멸 임박 알림 (D-7/D-3/D-1) — 기존 이벤트 인프라 재사용
- [ ] 인앱 알림 리스트 UI — API는 있으나 프론트 미사용, Push 실패 시 알림 유실
- [ ] 제품명 자동완성/추천 (LLM)
- [ ] 버그 제보 LLM triage

## ✅ Done

- [x] `[BE]` Web Push 알림 기능 (Domain → Infra → API → 프론트 연동)
- [x] `[BE]` `grantPointsForAll()` self-invocation 트랜잭션 수정 (이중 지급 방지)
- [x] `[BE]` `@EnableAsync` + `AsyncConfig` 추가
- [x] `[BE]` Push 구독 API 멱등성 확보
- [x] `docs/` 4종 작성 (architecture, db-schema, api-spec, frontend-architecture)
- [x] CI (GitHub Actions: `./gradlew compileJava`, main push/PR 트리거)