package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.application.JobReport;
import com.oliveyoung.mate.application.point.command.CancelUseCommand;
import com.oliveyoung.mate.application.point.command.EarnPointCommand;
import com.oliveyoung.mate.application.point.command.GrantPointManualCommand;
import com.oliveyoung.mate.application.point.command.InitPointCommand;
import com.oliveyoung.mate.application.point.command.UsePointCommand;
import com.oliveyoung.mate.application.point.result.LedgerHistoryResult;
import com.oliveyoung.mate.application.point.result.LifetimeStatsResult;
import com.oliveyoung.mate.application.point.result.PointBalanceResult;
import com.oliveyoung.mate.application.point.result.PointCancelPreviewResult;
import com.oliveyoung.mate.application.point.result.PointUsePreviewResult;
import com.oliveyoung.mate.application.point.result.UsePointResult;
import com.oliveyoung.mate.domain.attendance.model.WorkDay;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.point.PointAccountNotFoundException;
import com.oliveyoung.mate.domain.point.event.AdminAdjustedEvent;
import com.oliveyoung.mate.domain.point.event.PointExpiringEvent;
import com.oliveyoung.mate.domain.point.model.Point;
import com.oliveyoung.mate.domain.point.model.PointLedger;
import com.oliveyoung.mate.domain.point.repository.PointPolicyRepository;
import com.oliveyoung.mate.domain.point.repository.PointRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import com.oliveyoung.mate.domain.point.vo.PointPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository           pointRepository;
    private final PointPolicyRepository     policyRepository;
    private final WorkDayRepository         workDayRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 일괄 처리는 항목별로 트랜잭션을 직접 연다.
     * earn()/expirePoints()의 @Transactional은 자기 호출이라 프록시를 타지 않아 적용되지 않는다.
     */
    private final TransactionTemplate       txTemplate;

    // ── 포인트 적립 ────────────────────────────────
    @Transactional
    public void earn(EarnPointCommand cmd) {
        PointPolicy policy = policyRepository.findActivePolicy()
            .orElse(PointPolicy.defaultPolicy());

        CrewId crewId = CrewId.of(cmd.crewId());

        Point point = pointRepository.findByCrewId(crewId)
            .orElseGet(() -> Point.create(crewId));

        // 실제 근무일(workDate) 자정 기준 — 스케줄러가 다음날 실행돼도 올바른 날짜로 계산
        LocalDateTime registeredAt = cmd.workDate().atStartOfDay();
        LocalDateTime grantedAt    = registeredAt.plusDays(policy.grantDelayDays());
        LocalDateTime expiredAt    = registeredAt.plusDays(policy.expiryDays());

        point.earn(policy, cmd.workDayId(), grantedAt, expiredAt);

        pointRepository.save(point);
        workDayRepository.markPointGranted(cmd.workDayId());
        publishEvents(point);
    }

    // ── 포인트 사용 (FIFO) ─────────────────────────
    @Transactional
    public UsePointResult use(UsePointCommand cmd) {
        CrewId crewId = CrewId.of(cmd.crewId());

        Point point = pointRepository.findByCrewId(crewId)
            .orElseThrow(() -> new PointAccountNotFoundException(crewId));

        UUID txId = UUID.randomUUID();
        Money requestAmount = Money.of(cmd.amount());

        LocalDateTime usedAt = cmd.usedAt() != null
            ? cmd.usedAt().atStartOfDay(ZoneId.of("Asia/Seoul")).toLocalDateTime()
            : LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        point.use(requestAmount, txId, usedAt, cmd.description(), cmd.brand());

        pointRepository.save(point);
        publishEvents(point);

        UUID usedLedgerId = point.getNewLedgers().stream()
            .filter(l -> l.getType() == PointLedger.LedgerType.USE)
            .findFirst()
            .orElseThrow()
            .getLedgerId();

        return new UsePointResult(
            requestAmount.amount(),
            point.getBalance().amount(),
            usedLedgerId
        );
    }

    // ── 잔액 조회 ──────────────────────────────────
    @Transactional(readOnly = true)
    public PointBalanceResult getBalance(UUID crewId) {
        CrewId cid = CrewId.of(crewId);

        Money balance = pointRepository.findBalanceByCrewId(cid)
            .orElse(Money.zero());

        LocalDateTime now        = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd   = monthStart.plusMonths(1);

        Money expiringIn7Days  = pointRepository.sumExpiringBetween(cid, now, now.plusDays(7));
        Money expiringIn30Days = pointRepository.sumExpiringBetween(cid, now, now.plusDays(30));
        Money monthlyEarned    = pointRepository.sumByTypeAndPeriod(cid, "EARN", monthStart, monthEnd);
        Money monthlyUsed      = pointRepository.sumByTypeAndPeriod(cid, "USE", monthStart, monthEnd);
        Money monthlyExpiring  = pointRepository.sumExpiringBetween(cid, now, monthEnd);

        return new PointBalanceResult(
            balance.amount(),
            expiringIn7Days.amount(),
            expiringIn30Days.amount(),
            monthlyEarned.amount(),
            monthlyUsed.amount(),
            monthlyExpiring.amount()
        );
    }

    // ── 근무일당 적립액 (근무 등록 시트 미리보기용) ──
    @Transactional(readOnly = true)
    public long getEarnAmount() {
        return policyRepository.findActivePolicy().orElse(PointPolicy.defaultPolicy()).earnAmount();
    }

    // ── 누적 통계 (전체 기간) ──────────────────────
    @Transactional(readOnly = true)
    public LifetimeStatsResult getLifetimeStats(UUID crewId) {
        CrewId cid = CrewId.of(crewId);
        LocalDateTime from = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusYears(1);

        long earned = pointRepository.sumByTypeAndPeriod(cid, "EARN", from, to).amount()
            + pointRepository.sumByTypeAndPeriod(cid, "INIT", from, to).amount();
        long expired = pointRepository.sumByTypeAndPeriod(cid, "EXPIRE", from, to).amount();

        return new LifetimeStatsResult(earned, expired);
    }

    // ── 내역 조회 ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<LedgerHistoryResult> getLedgerHistory(UUID crewId) {
        Point point = pointRepository.findByCrewId(CrewId.of(crewId))
            .orElseThrow(() -> new PointAccountNotFoundException(CrewId.of(crewId)));

        return point.getLedgers().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(l -> new LedgerHistoryResult(
                l.getLedgerId(),
                l.getTxId(),
                l.getType(),
                l.getAmount().amount(),
                l.getRemaining().amount(),
                l.getGrantedAt(),
                l.getExpiredAt(),
                l.getCreatedAt(),
                l.getDescription(),
                l.getBrand()
            ))
            .toList();
    }

    // ── 자동 만료 ──────────────────────────────────
    @Transactional
    public void expirePoints(UUID crewId) {
        Point point = pointRepository.findByCrewId(CrewId.of(crewId))
            .orElseThrow(() -> new PointAccountNotFoundException(CrewId.of(crewId)));

        point.expireOld(LocalDateTime.now());

        pointRepository.save(point);
        publishEvents(point);
    }

    // ── 초기 포인트 등록 (최초 1회) ───────────────
    @Transactional
    public void initialize(InitPointCommand cmd) {
        CrewId crewId = CrewId.of(cmd.crewId());

        Point point = pointRepository.findByCrewId(crewId)
            .orElseGet(() -> Point.create(crewId));

        point.initialize(Money.of(cmd.amount()));

        pointRepository.save(point);
    }

    // ── 포인트 사용 취소 (당일 건) ─────────────────
    @Transactional
    public void cancelUse(CancelUseCommand cmd) {
        CrewId crewId = CrewId.of(cmd.crewId());

        PointLedger useLedger = pointRepository.findLedgerById(cmd.ledgerId())
            .orElseThrow(() -> new IllegalArgumentException("사용 내역을 찾을 수 없습니다."));

        if (useLedger.getType() != PointLedger.LedgerType.USE) {
            throw new IllegalArgumentException("사용 내역이 아닙니다.");
        }
        if (!useLedger.getCrewId().equals(crewId)) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }
        if (!useLedger.getCreatedAt().toLocalDate().equals(LocalDate.now())) {
            throw new IllegalStateException("당일 사용 건만 취소 가능합니다.");
        }

        Point point = pointRepository.findByCrewId(crewId)
            .orElseThrow(() -> new PointAccountNotFoundException(crewId));

        point.cancelUse(useLedger.getTxId());
        pointRepository.save(point);
        pointRepository.deleteLedgersByTxId(useLedger.getTxId());
    }

    // ── 포인트 사용 미리보기 (커밋 없음, FIFO 차감 내역만 계산) ──
    @Transactional(readOnly = true)
    public PointUsePreviewResult previewUse(UUID crewId, long amount, LocalDate usedAt) {
        CrewId cid = CrewId.of(crewId);
        Point point = pointRepository.findByCrewId(cid)
            .orElseThrow(() -> new PointAccountNotFoundException(cid));

        Map<UUID, Long> remainingBefore = point.getLedgers().stream()
            .collect(Collectors.toMap(PointLedger::getLedgerId, l -> l.getRemaining().amount()));

        LocalDateTime usedAtDateTime = usedAt != null
            ? usedAt.atStartOfDay(ZoneId.of("Asia/Seoul")).toLocalDateTime()
            : LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // 저장하지 않는다 — point.use()는 인메모리 애그리거트만 변경한다
        point.use(Money.of(amount), UUID.randomUUID(), usedAtDateTime, null, null);

        List<PointUsePreviewResult.Line> lines = point.getDirtyLedgers().stream()
            .sorted(Comparator.comparing(l -> l.getExpiredAt() == null ? LocalDateTime.MAX : l.getExpiredAt()))
            .map(l -> new PointUsePreviewResult.Line(
                l.getGrantedAt(),
                l.getExpiredAt(),
                remainingBefore.get(l.getLedgerId()),
                remainingBefore.get(l.getLedgerId()) - l.getRemaining().amount()
            ))
            .toList();

        return new PointUsePreviewResult(lines, point.getBalance().amount());
    }

    // ── 포인트 사용 취소 미리보기 (커밋 없음, 복원 내역만 계산) ──
    @Transactional(readOnly = true)
    public PointCancelPreviewResult previewCancel(UUID crewId, UUID ledgerId) {
        CrewId cid = CrewId.of(crewId);

        PointLedger useLedger = pointRepository.findLedgerById(ledgerId)
            .orElseThrow(() -> new IllegalArgumentException("사용 내역을 찾을 수 없습니다."));
        if (useLedger.getType() != PointLedger.LedgerType.USE) {
            throw new IllegalArgumentException("사용 내역이 아닙니다.");
        }
        if (!useLedger.getCrewId().equals(cid)) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }

        Point point = pointRepository.findByCrewId(cid)
            .orElseThrow(() -> new PointAccountNotFoundException(cid));

        Map<UUID, Long> remainingBefore = point.getLedgers().stream()
            .collect(Collectors.toMap(PointLedger::getLedgerId, l -> l.getRemaining().amount()));

        // 저장하지 않는다 — point.cancelUse()는 인메모리 애그리거트만 변경한다
        point.cancelUse(useLedger.getTxId());

        List<PointCancelPreviewResult.Line> lines = point.getDirtyLedgers().stream()
            .sorted(Comparator.comparing(l -> l.getExpiredAt() == null ? LocalDateTime.MAX : l.getExpiredAt()))
            .map(l -> new PointCancelPreviewResult.Line(
                l.getExpiredAt(),
                remainingBefore.get(l.getLedgerId()),
                l.getRemaining().amount() - remainingBefore.get(l.getLedgerId())
            ))
            .toList();

        return new PointCancelPreviewResult(lines, point.getBalance().amount());
    }

    // ── 소급 적립 (관리자 전용) ────────────────────
    @Transactional
    public void grantPointForDate(GrantPointManualCommand cmd) {
        WorkDay workDay = workDayRepository.findByCrewIdAndWorkDate(cmd.crewId(), cmd.workDate())
            .orElseThrow(() -> new IllegalArgumentException(
                "근무일을 찾을 수 없습니다. date=" + cmd.workDate()));

        if (workDay.isPointGranted()) {
            throw new IllegalStateException("이미 포인트가 지급된 근무일입니다.");
        }
        if (workDay.isSkipped()) {
            throw new IllegalStateException("결근 처리된 근무일입니다.");
        }

        earn(new EarnPointCommand(cmd.crewId(), workDay.getWorkDayId(), cmd.workDate()));

        // 관리자 소급 지급 알림 — earn()의 일반 EARN 알림과 별도로 발송된다(의도된 중복)
        eventPublisher.publishEvent(new AdminAdjustedEvent(CrewId.of(cmd.crewId()), cmd.workDate()));
    }

    // ── Cron/Admin 일괄 처리 ──────────────────────
    public JobReport grantPointsForAll() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        log.info("[Admin Cron] 포인트 적립 시작 {}", today);
        int[] count = {0, 0};
        workDayRepository.findAllNotGranted(today).forEach(workDay -> {
            try {
                EarnPointCommand cmd = new EarnPointCommand(
                    workDay.getCrewId(),
                    workDay.getWorkDayId(),
                    workDay.getWorkDate()
                );
                txTemplate.executeWithoutResult(status -> earn(cmd));
                count[0]++;
            } catch (Exception e) {
                count[1]++;
                log.error("[Admin Cron] 포인트 지급 실패. crewId={}", workDay.getCrewId(), e);
            }
        });
        log.info("[Admin Cron] 처리 완료 {}건 (실패 {}건)", count[0], count[1]);
        return JobReport.of("포인트 적립", today, count[0], count[1]);
    }

    // ── 소멸 임박 알림 (D-7/D-3/D-1) ────────────────
    public JobReport remindExpiringPoints() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        log.info("[Admin Cron] 소멸 임박 알림 시작 {}", today);
        int[] count = {0, 0};
        for (int daysLeft : new int[]{7, 3, 1}) {
            LocalDateTime from = today.plusDays(daysLeft).atStartOfDay();
            LocalDateTime to = from.plusDays(1);
            pointRepository.findExpiringAmountsBetween(from, to).forEach(r -> {
                try {
                    txTemplate.executeWithoutResult(status ->
                        eventPublisher.publishEvent(new PointExpiringEvent(r.crewId(), r.amount(), from, daysLeft)));
                    count[0]++;
                } catch (Exception e) {
                    count[1]++;
                    log.error("[Admin Cron] 소멸 임박 알림 실패. crewId={}", r.crewId(), e);
                }
            });
        }
        log.info("[Admin Cron] 처리 완료 {}건 (실패 {}건)", count[0], count[1]);
        return JobReport.of("소멸 임박 알림", today, count[0], count[1]);
    }

    public JobReport expireAllPoints() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        log.info("[Admin Cron] 포인트 만료 처리 시작 {}", today);
        int[] count = {0, 0};
        pointRepository.findAllCrewIdsWithExpiringPoints().forEach(crewId -> {
            try {
                txTemplate.executeWithoutResult(status -> expirePoints(crewId.id()));
                count[0]++;
            } catch (Exception e) {
                count[1]++;
                log.error("[Admin Cron] 만료 처리 실패. crewId={}", crewId.id(), e);
            }
        });
        log.info("[Admin Cron] 처리 완료 {}건 (실패 {}건)", count[0], count[1]);
        return JobReport.of("포인트 만료", today, count[0], count[1]);
    }

    // ── private helpers ────────────────────────────
    private void publishEvents(Point point) {
        point.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}