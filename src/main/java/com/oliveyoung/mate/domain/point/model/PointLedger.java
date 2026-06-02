package com.oliveyoung.mate.domain.point.model;

import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import java.time.LocalDateTime;
import java.util.UUID;

public class PointLedger {

    public enum LedgerType { EARN, USE, EXPIRE, INIT }

    private final UUID          ledgerId;
    private final CrewId        crewId;
    private final UUID          workDayId;
    private final UUID          txId;
    private final LedgerType    type;
    private final Money         amount;
    private Money               remaining;
    private final LocalDateTime grantedAt;
    private final LocalDateTime expiredAt;
    private final LocalDateTime createdAt;

    // ── 팩토리 메서드 ──────────────────────────────

    // DB에서 복원할 때 사용 — ledgerId/remaining을 원본 값 그대로 보존
    public static PointLedger reconstruct(UUID ledgerId, CrewId crewId, UUID workDayId,
                                          UUID txId, LedgerType type, Money amount,
                                          Money remaining, LocalDateTime grantedAt,
                                          LocalDateTime expiredAt, LocalDateTime createdAt) {
        return new PointLedger(ledgerId, crewId, workDayId, txId, type, amount,
                               remaining, grantedAt, expiredAt, createdAt);
    }

    public static PointLedger earn(CrewId crewId, UUID workDayId,
                                   Money amount, LocalDateTime grantedAt,
                                   LocalDateTime expiredAt) {
        return new PointLedger(
            UUID.randomUUID(), crewId, workDayId, null,
            LedgerType.EARN, amount, amount,
            grantedAt, expiredAt, LocalDateTime.now()
        );
    }

    public static PointLedger use(CrewId crewId, UUID txId,
                                  Money amount, LocalDateTime usedAt) {
        return new PointLedger(
            UUID.randomUUID(), crewId, null, txId,
            LedgerType.USE, amount, Money.zero(),
            usedAt, null, LocalDateTime.now()
        );
    }

    public static PointLedger expire(CrewId crewId,
                                     Money amount, LocalDateTime expiredAt) {
        return new PointLedger(
            UUID.randomUUID(), crewId, null, null,
            LedgerType.EXPIRE, amount, Money.zero(),
            expiredAt, expiredAt, LocalDateTime.now()
        );
    }

    public static PointLedger init(CrewId crewId, Money amount, LocalDateTime grantedAt) {
    return new PointLedger(
        UUID.randomUUID(), crewId, null, null,
        LedgerType.INIT, amount, amount,
        grantedAt, null, LocalDateTime.now()
        );
    }

    // ── 비즈니스 메서드 ────────────────────────────
    public void deduct(Money deductAmount) {
        if (type != LedgerType.EARN && type != LedgerType.INIT) {
            throw new IllegalStateException("EARN 원장만 차감 가능합니다.");
        }
        this.remaining = this.remaining.subtract(deductAmount);
    }

    public boolean isEarnType()                { return type == LedgerType.EARN; }
    public boolean hasRemaining()              { return !remaining.isZero(); }
    public boolean isExpired(LocalDateTime at) {
        return expiredAt != null && expiredAt.isBefore(at);
    }

    // ── 생성자 ─────────────────────────────────────
    private PointLedger(UUID ledgerId, CrewId crewId, UUID workDayId,
                        UUID txId, LedgerType type, Money amount,
                        Money remaining, LocalDateTime grantedAt,
                        LocalDateTime expiredAt, LocalDateTime createdAt) {
        this.ledgerId  = ledgerId;
        this.crewId    = crewId;
        this.workDayId = workDayId;
        this.txId      = txId;
        this.type      = type;
        this.amount    = amount;
        this.remaining = remaining;
        this.grantedAt = grantedAt;
        this.expiredAt = expiredAt;
        this.createdAt = createdAt;
    }

    // ── Getters ────────────────────────────────────
    public UUID          getLedgerId()  { return ledgerId; }
    public CrewId        getCrewId()    { return crewId; }
    public UUID          getWorkDayId() { return workDayId; }
    public UUID          getTxId()      { return txId; }
    public LedgerType    getType()      { return type; }
    public Money         getAmount()    { return amount; }
    public Money         getRemaining() { return remaining; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}