# TODO

> 상세 배경은 `docs/architecture.md`, `docs/api-spec.md`, `mate-front/docs/frontend-architecture.md` 참고

## 🔴 High — 실사용자에게 영향 있음

(모두 처리 완료 — 아래 Done 참고)

## 🟡 Medium

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
- [ ] CI 파이프라인 확장 — `./gradlew build`(테스트 포함, 시크릿 주입 필요) + `npm run build`(프론트). 확장 시 CI 러너에 [Tailwind standalone CLI](https://github.com/tailwindlabs/tailwindcss/releases) 바이너리도 설치 필요(`processResources`가 `tailwindBuild`에 의존)

## 📦 Backlog — 신규 기능

- [ ] 제품명 자동완성/추천 (LLM)
- [ ] 버그 제보 LLM triage
- [ ] **프론트엔드 스택 전환(Phase 1+): `mate-front`(Next.js/React SPA) → `mate` 내 Thymeleaf + htmx.** SEO 불필요 + 백엔드(Java/Spring) 스택 활용도를 높이려는 목적. Phase 0(세션 인증 기반)은 완료(Done 참고). 병행 운영 없이 사용자 노출은 한 번에 전환하되, 코드는 phase마다 짧은 브랜치로 `main` merge + 배포(네비게이션은 전체 이관 완료 전까지 그대로 Next.js를 가리킴).
  - Phase 1(완료): 작고 읽기 위주인 `notifications` 페이지 하나를 끝까지 이관해 컨트롤러/템플릿/htmx 패턴 확립
  - Phase 2(완료): `admin`/`login` 역할 기반 리다이렉트 + `/admin` 최소 스텁
  - Phase 3(완료): `mypage`(근무요일 조회/변경) + `signup`(회원가입)
  - Phase 4(완료): `/admin` CRUD 확장(근무관리·포인트내역·포인트적립·회원정보·크루등록)
  - Phase 5(완료): `dashboard`(포인트 잔액, 이번 주 근무 현황, 포인트 사용, 되돌리기). 캘린더(`history`)와 소정근무일 등록(`mypage`, Phase 3에서 이미 이관)은 dashboard와 별도 페이지로 확인되어 이번 범위 밖. 자동완성은 원본 코드베이스에 애초에 존재하지 않아 이관 대상 없음. `UsePointResult`에 `usedLedgerId` 추가해 되돌리기 기능을 실제로 동작하게 수정(기존엔 dead code)
  - Phase 6(완료): `history`(캘린더 — 달력 그리드, 표시 월 요약, 만료 임박 배너, 날짜 클릭 상세, 당일 사용 건 취소, 지난달 적립 내역). `LedgerHistoryResult`에 `ledgerId`/`txId` 추가해 취소 기능을 실제로 동작하게 수정(기존 Next.js FE는 없는 `l.id`를 읽으려 해 취소 버튼이 항상 죽어 있었음 — `mergedIds`가 매번 빈 배열). 알림 딥링크(`/history?date=`)가 그대로 이 페이지로 연결되어 TODO의 "알림 딥링크 `?date=` 파라미터 처리" 항목도 함께 해결됨
  - 재구현 필요(자동으로 안 따라옴): `hx-boost`로 페이지 전환 무깜빡임 유지, `TopProgressBar` 단계별 페이크 프로그레스, 알림 읽음 처리 optimistic update
  - 미결정: Web Push 죽은 코드(`app/lib/push.ts`, 현재 어디서도 미호출) 이관 여부, admin 인증 이원화(`X-Admin-Key` vs `ROLE_ADMIN`) 통합 여부
  - 상세 설계는 `/Users/seon/.claude/plans/mate-front-next-js-react-dynamic-heron.md` 참고(임시 경로, 착수 시 이 TODO 기준으로 재설계할 것)

## ✅ Done

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