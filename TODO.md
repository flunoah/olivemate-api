# TODO

> 상세 배경은 `docs/architecture.md`, `docs/api-spec.md`, `mate-front/docs/frontend-architecture.md` 참고
>
> **2026-08-26 최종 컷오버 완료**: `mate-front`(Next.js)는 은퇴했다 — Vercel에는 전체 경로를 백엔드로 302 리다이렉트하는 최소 배포만 남기고, 코드는 더 이상 손대지 않는다. 아래 `[FE]` 항목들은 전부 은퇴한 저장소 얘기라 실질적으로 moot하지만, 히스토리 보존을 위해 지우지 않고 남겨둔다.

## 🔴 High — 실사용자에게 영향 있음

(모두 처리 완료 — 아래 Done 참고)

## 🟡 Medium

- [ ] `[BE]` S16(`mypage`) 프로필 카드 — "매장" 필드는 `Crew` 도메인에 아예 없어 표시하지 못함(`사번`은 `loginId`로 대체 표시). 매장 데이터를 어디서 가져올지 결정되면 `crew` 테이블에 컬럼 추가 + 회원가입 플로우까지 이어지는 별도 작업 필요
- [ ] `[BE]` S18 알림 설정 시트가 현재 구독의 채널별 on/off 상태를 조회하는 API가 없어, 시트를 열 때마다 체크박스가 항상 전부 켜진 상태로 보임(저장은 정상 동작). `GET /push/subscribe/channels` 추가하면 해결
- [ ] `[FE]` 하단 네비 `<a>` → `next/link`. 탭 전환마다 전체 리로드 중
- [x] `[FE]` 알림 딥링크 `?date=` 파라미터 처리 (`useSearchParams`) — Phase 6에서 `/history`가 Thymeleaf `HistoryPageController`로 이관되며 `?date=` 쿼리파라미터를 서버에서 직접 처리하게 되어 해결(Next.js `useSearchParams` 구현은 더 이상 불필요)
- [ ] `[BE]` `OptimisticLockingFailureException` 전용 핸들러. 현재 500으로 떨어짐
- [ ] `[BE]` `notification`/`push_subscription` 테이블명 단수형 통일 검토
- [ ] `[FE]` `next.config.ts` 백엔드 주소를 `API_BASE_URL` 환경변수로 분리
- [ ] `[FE]` `/admin` 페이지가 존재하지 않는 백엔드 엔드포인트를 호출 중 — `POST /api/v1/admin/points/grant`(포인트적립 탭), `DELETE /api/v1/admin/crews/{id}`(크루 삭제). signup 때 발견한 것과 같은 종류의 프론트/백엔드 경로 불일치로, 두 기능 모두 프로덕션에서 항상 실패해왔을 것으로 보임

## 🟢 Low

- [ ] `[BE]` FIFO 로직 단위 테스트 (`Point.use()`, `cancelUse()`, `expireOld()`)
- [ ] `[BE]` `PointService.cancelUse()`의 당일 판정(`useLedger.getCreatedAt().toLocalDate().equals(LocalDate.now())`)이 시스템 기본 zone 사용 중 — `Asia/Seoul` 미적용. 자정 근처 KST 사용 건에서 취소 가능 여부가 어긋날 수 있음
- [ ] `[FE]` 공통 컴포넌트 추출 (`Toast`, `Card`, `Button`)
- [ ] `[FE]` 컬러 토큰 상수화
- [ ] `[FE]` `NEXT_PUBLIC_ADMIN_KEY` 사용 재검토 (브라우저 번들 노출)
- [ ] `[BE]` 응답 포맷 통일 (`ApiResponse` 래핑 여부)
- [ ] `[FE]` ESLint 도입 (`lint` 스크립트 부재)
- [ ] CI 파이프라인 확장 — `./gradlew build`(테스트 포함, 시크릿 주입 필요). 확장 시 CI 러너에 [Tailwind standalone CLI](https://github.com/tailwindlabs/tailwindcss/releases) 바이너리도 설치 필요(`processResources`가 `tailwindBuild`에 의존)
- [ ] `[BE]` Web Push 알림 아이콘 파일(`/icons/icon-192.png`, `/icons/badge-72.png`) 부재 — `service-worker.js`가 참조하지만 실제 파일이 없음. mate-front 시절부터 있던 기존 결함으로, 최종 컷오버 시 `static/service-worker.js`로 그대로 포팅됨 (`docs/api-spec.md` "알려진 결함" 참고)
- [ ] mate-front GitHub 레포 아카이브 — 최종 컷오버에서 Vercel 리다이렉트 배포까지는 완료. 레포 아카이브(`gh repo archive`)는 안정화 확인 후 사용자가 직접 진행하기로 함
- [ ] `product_request` 테이블(`V5`) 드롭 검토 — 포인트 사용 시트를 자유 텍스트로 되돌리며 관련 애플리케이션 코드는 전부 제거했으나(아래 Done 참고), 테이블 자체는 파괴적 작업이라 남겨둠

## 📦 Backlog — 신규 기능

- [ ] 버그 제보 LLM triage
- [ ] **프론트엔드 스택 전환(Phase 1+): `mate-front`(Next.js/React SPA) → `mate` 내 Thymeleaf + htmx.** SEO 불필요 + 백엔드(Java/Spring) 스택 활용도를 높이려는 목적. Phase 0(세션 인증 기반)은 완료(Done 참고). 병행 운영 없이 사용자 노출은 한 번에 전환하되, 코드는 phase마다 짧은 브랜치로 `main` merge + 배포(네비게이션은 전체 이관 완료 전까지 그대로 Next.js를 가리킴).
  - Phase 1(완료): 작고 읽기 위주인 `notifications` 페이지 하나를 끝까지 이관해 컨트롤러/템플릿/htmx 패턴 확립
  - Phase 2(완료): `admin`/`login` 역할 기반 리다이렉트 + `/admin` 최소 스텁
  - Phase 3(완료): `mypage`(근무요일 조회/변경) + `signup`(회원가입)
  - Phase 4(완료): `/admin` CRUD 확장(근무관리·포인트내역·포인트적립·회원정보·크루등록)
  - Phase 5(완료): `dashboard`(포인트 잔액, 이번 주 근무 현황, 포인트 사용, 되돌리기). 캘린더(`history`)와 소정근무일 등록(`mypage`, Phase 3에서 이미 이관)은 dashboard와 별도 페이지로 확인되어 이번 범위 밖. 자동완성은 원본 코드베이스에 애초에 존재하지 않아 이관 대상 없음. `UsePointResult`에 `usedLedgerId` 추가해 되돌리기 기능을 실제로 동작하게 수정(기존엔 dead code)
  - Phase 6(완료): `history`(캘린더 — 달력 그리드, 표시 월 요약, 만료 임박 배너, 날짜 클릭 상세, 당일 사용 건 취소, 지난달 적립 내역). `LedgerHistoryResult`에 `ledgerId`/`txId` 추가해 취소 기능을 실제로 동작하게 수정(기존 Next.js FE는 없는 `l.id`를 읽으려 해 취소 버튼이 항상 죽어 있었음 — `mergedIds`가 매번 빈 배열). 알림 딥링크(`/history?date=`)가 그대로 이 페이지로 연결되어 TODO의 "알림 딥링크 `?date=` 파라미터 처리" 항목도 함께 해결됨
  - [x] `hx-boost`로 페이지 전환 무깜빡임 유지 — 하단 탭 내비게이션(`dashboard`/`history`/`notifications`/`mypage`)에 한해 적용(`fragments/layout.html`의 `nav` 프래그먼트). 로그인/회원가입/관리자 페이지는 폼 제출 위주라 boost 범위에서 제외
  - [x] 알림 읽음 처리 optimistic update — 클릭 즉시 읽음 스타일로 전환(`htmx:beforeRequest`), 실제 리다이렉트는 기존대로 서버 응답 후 진행
  - 재구현 필요(자동으로 안 따라옴, 이번 phase 밖): `TopProgressBar` 단계별 페이크 프로그레스
  - (참고) Web Push 죽은 코드 이관, admin 인증 이원화 통합은 별도 미해결 항목이 아니라 Phase 7에서 이미 해결됨 — 아래 참고
  - Phase 7(완료): **최종 컷오버.** `SecurityConfig`를 `webFilterChain` 단일 체인으로 축소 — CORS 설정 전면 제거, `JwtAuthFilter`/`JwtProvider`/`TokenProvider` 삭제, `CrewService.signUp()`에서 토큰 발급 제거(`login()`/`refresh()`도 삭제). mate-front 전용이었던 `/api/v1/{auth,attendance,schedule,point,notification}` REST 컨트롤러 삭제, `AdminController`의 `ROLE_ADMIN` 기반 `getCrews`/`getWorkDays`도 삭제(admin 인증 이원화 문제는 이렇게 X-Admin-Key 단일화로 해결). `PushSubscriptionController`를 `/api/v1/push`→`/push`로 옮겨 세션 인증 체계에 편입, Web Push 구독/해지 UI+서비스워커를 `mypage.html`로 이관(그동안 "미결정"이던 항목 해결). 루트(`/`) 경로가 없던 문제도 `RootController` 추가로 해결(역할별 리다이렉트). mate-front는 `next.config.ts`를 전체 경로→백엔드 302 리다이렉트로 교체하는 최소 배포만 남기고 은퇴
  - 상세 설계는 `/Users/seon/.claude/plans/kind-meandering-pike.md` 참고

## ✅ Done

- [x] 포인트 사용 시트 자유 입력으로 원복 + 상품 자동완성/등록요청 기능 제거 (2026-08-29) — 상품명 검색 자동완성이 검색 결과를 클릭해야만 다음 단계로 넘어갈 수 있어 불편하다는 피드백으로 되돌림. `dashboard.html` 포인트 사용 1단계를 `productName` 자유 텍스트 입력(브랜드 필드는 아예 없앰) + 항상 활성화된 "다음" 버튼으로 복원. `UsePointCommand`/`PointService`/`point_ledger.brand`는 원래 `goodsNo` 의존 없이 자유 텍스트였어서 백엔드 변경 없음(브랜드는 폼에서 안 보내면 자연히 null). 유일한 소비처를 잃은 `ProductSearchPageController`/`ProductSearchService`/`ProductSearchResult`/`fragments/product-search-results.html`과, 이 흐름에 종속돼 있던 "상품 등록·정정 요청" 기능(`ProductRequestPageController`/`AdminProductRequestPageController`/`application·domain·infrastructure/productrequest` 전체/`admin-product-requests.html`) 삭제. `product_request` 테이블(`V5`)은 드롭하지 않고 남김(위 Medium 참고). 엑셀 업로드 기반 상품 카탈로그 관리(`Product` 도메인/`ProductSyncService`/`AdminProductPageController`/`admin-products.html`)는 무관한 기능이라 그대로 유지
- [x] **Myjaso A 전체화면 리디자인 S8~S18 전체 구현** — Claude Design 시안(`f885925`에서 이미 반영된 S1~S7 이후 나머지). 3개 신규 백엔드 기능 포함:
  - 포인트 사용 3단계 플로우(S8~S10): `PointService.previewUse()`가 `Point.use()`를 커밋 없이 인메모리로만 실행해 FIFO 차감 미리보기 제공(`POST /dashboard/points/preview`). 완료 화면은 기존 10초 되돌리기 카운트다운을 제거하고 "오늘 안에 내역에서 취소" 정적 안내로 교체
  - 상품 등록·정정 요청(S11) — `ProductRequest` 신규 도메인(`Product`와 동일 레이어 구조), 크루 제출(`POST /products/requests`) + 관리자 승인/반려(`admin-product-requests.html`). 승인 시 NEW는 `product`에 신규 행(가격 미상이면 0원), CORRECTION은 연결 상품 갱신. 마이그레이션 `V5`
  - 사용 취소 확인(S13) — `PointService.previewCancel()`이 같은 커밋 없는 미리보기 기법으로 복구처 원장을 보여줌(`POST /history/points/cancel/preview`)
  - 관리자 조정 알림(S14) — `NotificationType.ADMIN_ADJUSTED` 신규. 관리자 소급 지급 시 기존 일반 "포인트 적립" 알림과 **별도로 추가 발송**됨(의도된 중복, `PointService.grantPointForDate()`에서 직접 이벤트 발행)
  - 채널별 푸시 알림(S15/S18) — `push_subscriptions`에 `notify_point_earned`/`notify_point_expiring`/`notify_admin_adjusted` 3개 boolean 컬럼(마이그레이션 `V6`), `PATCH /push/subscribe/channels`로 토글. 크루의 모든 기기 구독에 동일 적용(기기별 개별 설정 아님)
  - 요일 변경 영향 미리보기(S17) — `ScheduleService.previewChange()`가 `PointPolicy.earnAmount() × 4.33주`로 월 예상 적립 변화를 저장 없이 계산(`GET /mypage/schedule/preview`). 요일 이름 diff 없이 개수 기준 요약만 제공
  - `history`/`notifications`/`mypage` 템플릿 브랜드 팔레트(`#82DC28`/`#3F7D0A`/`#FF7878`) 전면 재작성
  - 상세 계획: `/Users/seon/.claude/plans/foamy-tickling-trinket.md`
  - **2026-08-27 후속**: 위 "전체 구현"이 실제 Claude Design 원본(S7~S13)과 디테일이 상당히 어긋나 있었음을 발견 — 근무등록 시트(S7)를 요일 토글+일괄 저장 방식으로, 포인트 사용(S8~S10)을 상품→금액→완료 3단계로, 내역(S12)을 캘린더 대신 필터+시간순 리스트로 재작업. 상세: `/Users/seon/.claude/plans/jazzy-petting-tide.md`
- [x] 제품명 자동완성/추천 — LLM이 아니라 상품 카탈로그(`product` 테이블, 엑셀 업로드로 관리자가 동기화) 대상 키워드 검색(`LIKE`, 최대 20건)으로 구현. `/dashboard` 포인트 사용 폼에서 htmx 라이브서치(입력 후 300ms)로 상품명 자동완성 + 선택 시 브랜드를 `point_ledger.brand`(`V4`)에 저장, `/history`에서 브랜드 표시. 관리자 엑셀 업로드는 `/admin/products`(세션 `ROLE_ADMIN`)
- [x] `[BE]` Thymeleaf+htmx 마이그레이션 Phase 0(세션 기반 인증 전환) — `spring-boot-starter-thymeleaf` 추가. `/api/v1/**`는 기존 stateless JWT `SecurityFilterChain`을 그대로 유지(회귀 없음, `mate-front`가 컷오버 전까지 계속 의존), 신규 `/login` 페이지는 별도 세션 기반 `SecurityFilterChain`(폼 로그인 + CSRF + 로그아웃, `@Order`로 두 체인 분리)으로 구성. `CrewPrincipal`(`UserDetails`)로 JWT/세션 두 인증 경로의 principal 타입 통일, `SecurityUtils`도 갱신. 디자인은 Tailwind CSS(Node 없이 standalone CLI + Gradle `tailwindBuild` 태스크)로 `mate-front`와 동일한 클래스 재사용
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