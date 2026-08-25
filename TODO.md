# TODO

> 상세 배경은 `docs/architecture.md`, `docs/api-spec.md`, `mate-front/docs/frontend-architecture.md` 참고

## 🔴 High — 실사용자에게 영향 있음

(모두 처리 완료 — 아래 Done 참고)

## 🟡 Medium

- [ ] `[BE]` `UsePointResult`에 `txId` 추가 → 프론트 되돌리기 UI 활성화
- [ ] `[FE]` 되돌리기 UI 동작 확인 (위 작업 후)
- [ ] `[FE]` 하단 네비 `<a>` → `next/link`. 탭 전환마다 전체 리로드 중
- [ ] `[FE]` 알림 딥링크 `?date=` 파라미터 처리 (`useSearchParams`)
- [ ] `[BE]` `OptimisticLockingFailureException` 전용 핸들러. 현재 500으로 떨어짐
- [ ] `[BE]` `notification`/`push_subscription` 테이블명 단수형 통일 검토
- [ ] `[FE]` `next.config.ts` 백엔드 주소를 `API_BASE_URL` 환경변수로 분리
- [ ] `[문서]` 루트 `CLAUDE.md`가 참조하는 `docs/db-schema.md`가 실제로는 없고, 스키마 문서는 `src/main/resources/db/migration/CLAUDE.md`에 있음 — 참조 경로 정리 필요
- [ ] `[FE]` 대시보드 상품 자동완성이 401(토큰 만료) 시 조용히 실패함 — `authFetch`가 refresh까지 실패해도 자동완성 코드는 `if (!res.ok) return;`로 끝내버려 사용자는 "자동완성이 안 된다"고만 느끼고 로그인 문제인지 알 방법이 없음. 재현: 만료/무효 토큰 상태로 대시보드에서 제품명 입력
- [ ] `[BE]` `PointLedgerJpaRepository.findBalanceAggregates()`가 네이티브 SQL이라 `LedgerType`(`'EARN'`/`'INIT'`/`'USE'`) 값과 `point_ledger` 테이블/컬럼명이 문자열로 하드코딩됨 — enum 리네임이나 컬럼명 마이그레이션 시 컴파일러가 못 잡고 조용히 깨짐(enum 리네임은 0 반환, 컬럼명 변경은 호출 시점 500). 해당 enum/컬럼 리팩터링 시 이 쿼리 같이 확인할 것

## 🟢 Low

- [ ] `[BE]` FIFO 로직 단위 테스트 (`Point.use()`, `cancelUse()`, `expireOld()`)
- [ ] `[FE]` 공통 컴포넌트 추출 (`Toast`, `Card`, `Button`)
- [ ] `[FE]` 컬러 토큰 상수화
- [ ] `[FE]` `NEXT_PUBLIC_ADMIN_KEY` 사용 재검토 (브라우저 번들 노출)
- [ ] `[BE]` 응답 포맷 통일 (`ApiResponse` 래핑 여부)
- [ ] `[FE]` ESLint 도입 (`lint` 스크립트 부재)
- [ ] CI 파이프라인 확장 — `./gradlew build`(테스트 포함, 시크릿 주입 필요) + `npm run build`(프론트)
- [ ] `[BE]` `point_ledger.brand`는 있지만 `product_id` 등 상품 단위 구조적 참조는 여전히 없음 — 카테고리 집계 등 필요해지면 검토
- [ ] `[BE]` `ProductExcelParser`가 엑셀 컬럼 위치(상품코드/브랜드/상품명/정상가/판매가 순서)를 하드코딩 — 크롤러 출력 컬럼 순서가 바뀌면 조용히 잘못된 데이터를 넣음. 헤더 이름 기반 매칭으로 교체 검토
- [ ] `[FE]` 상품 자동완성 레이스 컨디션 — 빠르게 입력값을 바꾸면 먼저 나간 요청의 응답이 나중에 도착해 최신 입력과 안 맞는 결과가 뜰 수 있음(요청 취소/순번 검증 없음)
- [ ] `[FE]` 상품 자동완성이 한글 조합 중(`compositionstart`/`end` 미처리)에도 매 `onChange`마다 디바운스 타이머를 리셋 — 불완전한 자모 상태로 검색 요청이 나갈 수 있음
- [ ] `[FE]` 상품 카탈로그가 비어있을 때(엑셀 업로드 전) 자동완성이 항상 빈 배열만 반환 — 안내 문구 없이 조용히 비어있어 "고장났다"고 오인하기 쉬움
- [ ] `[FE]` 상품 자동완성 드롭다운의 바깥 클릭 감지가 `mousedown` 리스너만 등록 — 모바일 터치 전용 기기에서 바깥 탭 시 안 닫힐 가능성
- [ ] `[FE]` 자동완성으로 상품 선택 후 `productName`을 한 글자만 고쳐도 `selectedBrand`가 매번 null로 리셋됨 — 의도된 동작이지만 사용자가 "브랜드가 왜 또 빠지지" 혼란 소지

## 📦 Backlog — 신규 기능

- [ ] 버그 제보 LLM triage

## ✅ Done

- [x] `[BE/FE]` 어드민 상품 카탈로그 엑셀 업로드(`POST /api/v1/admin/products/upload`, `goods_no` 기준 upsert) + 크루 대시보드 자동완성/사용액 자동 입력 + `history` 페이지 이번 달 브랜드별 소비 집계(`point_ledger.brand` 비정규화 저장, 프론트 클라이언트 집계, 새 백엔드 엔드포인트 없음)
- [x] 소멸 임박 알림 (D-7/D-3/D-1) — `PointExpiryReminderScheduler`(매일 07:00 KST) → `PointService.remindExpiringPoints()` → `PointExpiringEvent` → `PointExpiringNotificationListener`(기존 `PointEarnedNotificationListener`와 동일 패턴)가 인앱 알림 저장 + 웹푸시 발송. 겸사겸사 `Notification.pointEarned()`/`WebPushClient`의 잘못된 deepLink(`/points/history` → `/history`, 존재하지 않는 라우트였음)도 수정
- [x] 인앱 알림 리스트 UI — `app/notifications/page.tsx` 신규, 대시보드 헤더 종 아이콘(안 읽음 뱃지 포함)에서 진입
- [x] `[BE]` `WebPushClient`의 `catch (Exception)` 세분화. `HttpResponse` 상태 코드(404/410)로만 구독 삭제, 그 외 일시적 오류(`IOException`/`ExecutionException`/`InterruptedException`)·설정 오류(`GeneralSecurityException`/`JoseException`)는 로그만 남기고 구독 유지
- [x] `[FE]` `next.config.ts` rewrites를 `/api/v1/:path*`로 좁힘. `/api/bugs`가 로컬 Route Handler로 정상 라우팅됨
- [x] `[FE]` 사용 취소 중복 호출 제거. `history` 모달 "취소하기" 버튼에 `cancelLoading` 가드 연결, `dashboard` undo 버튼에 `undoLoading` state 신규 추가
- [x] `[FE]` `proxy.ts` → `middleware.ts` 항목 재확인 결과 **오진단으로 판명**. 이 저장소의 Next.js 16.2.6부터는 `middleware`가 deprecated이고 `proxy.ts`/`export function proxy`가 현재 컨벤션(빌드 로그 "ƒ Proxy (Middleware)"로 정상 인식 확인). 라우트 가드 자체는 정상 작동 중이므로 리네임 불필요 — 만약 실사용자가 가드 미작동을 여전히 겪는다면 다른 원인(예: `middleware`/`proxy` 컨벤션 문제가 아님)을 재조사할 것
- [x] `[BE]` Web Push 알림 기능 (Domain → Infra → API → 프론트 연동)
- [x] `[BE]` `grantPointsForAll()` self-invocation 트랜잭션 수정 (이중 지급 방지)
- [x] `[BE]` `@EnableAsync` + `AsyncConfig` 추가
- [x] `[BE]` Push 구독 API 멱등성 확보
- [x] `docs/` 4종 작성 (architecture, db-schema, api-spec, frontend-architecture)
- [x] CI (GitHub Actions: `./gradlew compileJava`, main push/PR 트리거)