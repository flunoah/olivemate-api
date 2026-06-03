package com.oliveyoung.mate.application.point;

import com.oliveyoung.mate.application.point.command.CancelUseCommand;
import com.oliveyoung.mate.application.point.command.EarnPointCommand;
import com.oliveyoung.mate.application.point.command.GrantPointManualCommand;
import com.oliveyoung.mate.application.point.command.InitPointCommand;
import com.oliveyoung.mate.application.point.command.UsePointCommand;
import com.oliveyoung.mate.application.point.result.LedgerHistoryResult;
import com.oliveyoung.mate.application.point.result.PointBalanceResult;
import com.oliveyoung.mate.application.point.result.UsePointResult;
import com.oliveyoung.mate.domain.attendance.model.WorkDay;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import com.oliveyoung.mate.domain.point.PointAccountNotFoundException;
import com.oliveyoung.mate.domain.point.model.Point;
import com.oliveyoung.mate.domain.point.model.PointLedger;
import com.oliveyoung.mate.domain.point.repository.PointPolicyRepository;
import com.oliveyoung.mate.domain.point.repository.PointRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import com.oliveyoung.mate.domain.point.vo.PointPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository           pointRepository;
    private final PointPolicyRepository     policyRepository;
    private final WorkDayRepository         workDayRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        point.use(requestAmount, txId, LocalDateTime.now());

        pointRepository.save(point);
        publishEvents(point);

        return new UsePointResult(
            requestAmount.amount(),
            point.getBalance().amount()
        );
    }

    // ── 잔액 조회 ──────────────────────────────────
    @Transactional(readOnly = true)
    public PointBalanceResult getBalance(UUID crewId) {
        CrewId cid = CrewId.of(crewId);

        Money balance = pointRepository.findBalanceByCrewId(cid)
            .orElseThrow(() -> new PointAccountNotFoundException(cid));

        LocalDateTime now = LocalDateTime.now();
        Money expiringIn7Days  = pointRepository.sumExpiringBetween(cid, now, now.plusDays(7));
        Money expiringIn30Days = pointRepository.sumExpiringBetween(cid, now, now.plusDays(30));

        return new PointBalanceResult(
            balance.amount(),
            expiringIn7Days.amount(),
            expiringIn30Days.amount()
        );
    }

    // ── 내역 조회 ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<LedgerHistoryResult> getLedgerHistory(UUID crewId) {
        Point point = pointRepository.findByCrewId(CrewId.of(crewId))
            .orElseThrow(() -> new PointAccountNotFoundException(CrewId.of(crewId)));

        return point.getLedgers().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(l -> new LedgerHistoryResult(
                l.getType(),
                l.getAmount().amount(),
                l.getRemaining().amount(),
                l.getGrantedAt(),
                l.getExpiredAt(),
                l.getCreatedAt()
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
    }

    // ── private helpers ────────────────────────────
    private void publishEvents(Point point) {
        point.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}