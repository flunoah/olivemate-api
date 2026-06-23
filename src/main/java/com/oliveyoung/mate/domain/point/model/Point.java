package com.oliveyoung.mate.domain.point.model;

import com.oliveyoung.mate.domain.point.InsufficientPointException;
import com.oliveyoung.mate.domain.point.event.PointEarnedEvent;
import com.oliveyoung.mate.domain.point.event.PointExpiredEvent;
import com.oliveyoung.mate.domain.point.event.PointUsedEvent;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import com.oliveyoung.mate.domain.point.vo.PointPolicy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Point {

    private final CrewId crewId;
    private Money balance;
    private final List<PointLedger> ledgers;
    private final List<Object>      domainEvents   = new ArrayList<>();
    private final List<PointLedger> newLedgers     = new ArrayList<>();  // 이번 세션에서 추가된 원장
    private final Set<UUID>         dirtyLedgerIds = new HashSet<>();    // remaining이 변경된 원장 ID

    // ── 생성자 ─────────────────────────────────────
    public Point(CrewId crewId, Money balance, List<PointLedger> ledgers) {
        this.crewId  = crewId;
        this.balance = balance;
        this.ledgers = new ArrayList<>(ledgers);
    }

    public static Point create(CrewId crewId) {
        return new Point(crewId, Money.zero(), new ArrayList<>());
    }

    // ── 초기 포인트 등록 (최초 1회) ───────────────
    public void initialize(Money amount) {
        boolean alreadyInitialized = ledgers.stream()
            .anyMatch(l -> l.getType() == PointLedger.LedgerType.INIT);

        if (alreadyInitialized) {
            throw new IllegalStateException("이미 초기 포인트가 등록되어 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        PointLedger ledger = PointLedger.init(crewId, amount, now, now.plusWeeks(3));
        ledgers.add(ledger);
        newLedgers.add(ledger);
        balance = balance.add(amount);
    }

    // ── 포인트 적립 ────────────────────────────────
    // expiredAt은 호출자(Service)가 "등록일 기준" 정책을 반영해 계산해서 전달
    public void earn(PointPolicy policy, UUID workDayId,
                     LocalDateTime grantedAt, LocalDateTime expiredAt) {
        Money earnAmount = Money.of(policy.earnAmount());

        PointLedger ledger = PointLedger.earn(
            crewId, workDayId, earnAmount, grantedAt, expiredAt
        );
        ledgers.add(ledger);
        newLedgers.add(ledger);
        balance = balance.add(earnAmount);

        domainEvents.add(new PointEarnedEvent(crewId, earnAmount, grantedAt, expiredAt));
    }

    // ── 포인트 사용 (FIFO) ─────────────────────────
    public void use(Money requestAmount, UUID txId, LocalDateTime usedAt, String description) {
        validateBalance(requestAmount);

        Money remaining = requestAmount;

        List<PointLedger> targets = ledgers.stream()
            .filter(l -> (l.getType() == PointLedger.LedgerType.EARN
                       || l.getType() == PointLedger.LedgerType.INIT)
                      && l.hasRemaining()
                      && !l.isExpired(usedAt))
            .sorted(Comparator.comparing(l ->
                l.getExpiredAt() == null ? LocalDateTime.MAX : l.getExpiredAt()))
            .toList();

        for (PointLedger earn : targets) {
            if (remaining.isZero()) break;
            Money deduct = earn.getRemaining().isGreaterThan(remaining)
                ? remaining : earn.getRemaining();
            earn.deduct(deduct);
            dirtyLedgerIds.add(earn.getLedgerId());
            PointLedger useLedger = PointLedger.use(crewId, txId, deduct, usedAt, description);
            ledgers.add(useLedger);
            newLedgers.add(useLedger);
            remaining = remaining.subtract(deduct);
        }

        balance = balance.subtract(requestAmount);
        domainEvents.add(new PointUsedEvent(crewId, requestAmount, usedAt));
    }

    // ── 포인트 자동 만료 ───────────────────────────
    public void expireOld(LocalDateTime now) {
        List<PointLedger> expiredTargets = ledgers.stream()
            .filter(l -> (l.getType() == PointLedger.LedgerType.EARN
                       || l.getType() == PointLedger.LedgerType.INIT)
                      && l.hasRemaining()
                      && l.isExpired(now))
            .toList();

        for (PointLedger earn : expiredTargets) {
            Money expiredAmount = earn.getRemaining();
            earn.deduct(expiredAmount);
            dirtyLedgerIds.add(earn.getLedgerId());
            PointLedger expireLedger = PointLedger.expire(crewId, expiredAmount, now);
            ledgers.add(expireLedger);
            newLedgers.add(expireLedger);
            balance = balance.subtract(expiredAmount);
            domainEvents.add(new PointExpiredEvent(crewId, expiredAmount, now));
        }
    }

    // ── 포인트 사용 취소 (당일 txId 기준) ─────────
    public void cancelUse(UUID txId) {
        List<PointLedger> useLedgers = ledgers.stream()
            .filter(l -> l.getType() == PointLedger.LedgerType.USE && txId.equals(l.getTxId()))
            .toList();

        if (useLedgers.isEmpty()) {
            throw new IllegalArgumentException("취소할 사용 내역이 없습니다.");
        }

        Money totalToRestore = useLedgers.stream()
            .map(PointLedger::getAmount)
            .reduce(Money.zero(), Money::add);

        // 차감된 EARN 원장을 FIFO 순서대로 복원
        List<PointLedger> earnTargets = ledgers.stream()
            .filter(l -> (l.getType() == PointLedger.LedgerType.EARN
                       || l.getType() == PointLedger.LedgerType.INIT)
                      && l.getAmount().amount() > l.getRemaining().amount())
            .sorted(Comparator.comparing(l ->
                l.getExpiredAt() == null ? LocalDateTime.MAX : l.getExpiredAt()))
            .toList();

        Money toRestore = totalToRestore;
        for (PointLedger earn : earnTargets) {
            if (toRestore.isZero()) break;
            long capacity = earn.getAmount().amount() - earn.getRemaining().amount();
            if (capacity <= 0) continue;
            Money restore = toRestore.amount() > capacity ? Money.of(capacity) : toRestore;
            earn.restore(restore);
            dirtyLedgerIds.add(earn.getLedgerId());
            toRestore = toRestore.subtract(restore);
        }

        balance = balance.add(totalToRestore);
    }

    // ── 만료 예정 포인트 조회 ──────────────────────
    public Money getExpiringAmount(LocalDateTime from, LocalDateTime to) {
        return ledgers.stream()
            .filter(l -> l.getType() == PointLedger.LedgerType.EARN
                      && l.hasRemaining()
                      && l.getExpiredAt() != null
                      && !l.getExpiredAt().isBefore(from)
                      && l.getExpiredAt().isBefore(to))
            .map(PointLedger::getRemaining)
            .reduce(Money.zero(), Money::add);
    }

    // ── private helpers ────────────────────────────
    private void validateBalance(Money requestAmount) {
        if (balance.amount() < requestAmount.amount()) {
            throw new InsufficientPointException(
                "잔액 부족. balance=%d, requested=%d"
                    .formatted(balance.amount(), requestAmount.amount())
            );
        }
    }

    // ── Getters ────────────────────────────────────
    public CrewId getCrewId()             { return crewId; }
    public Money  getBalance()            { return balance; }
    public List<PointLedger> getLedgers() { return Collections.unmodifiableList(ledgers); }

    public List<PointLedger> getNewLedgers() {
        return Collections.unmodifiableList(newLedgers);
    }

    public List<PointLedger> getDirtyLedgers() {
        return ledgers.stream()
            .filter(l -> dirtyLedgerIds.contains(l.getLedgerId()))
            .toList();
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}