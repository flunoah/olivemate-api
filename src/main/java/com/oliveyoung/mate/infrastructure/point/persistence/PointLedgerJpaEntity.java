package com.oliveyoung.mate.infrastructure.point.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "point_ledger", indexes = {
    @Index(name = "idx_ledger_crew_type_expired",
           columnList = "crew_id, ledger_type, expired_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLedgerJpaEntity {

    public enum LedgerType { EARN, USE, EXPIRE, INIT }

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID ledgerId;

    @Column(name = "crew_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID crewId;

    @Column(name = "work_day_id", columnDefinition = "BINARY(16)")
    private UUID workDayId;

    @Column(name = "tx_id", columnDefinition = "BINARY(16)")
    private UUID txId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ledger_type", nullable = false, length = 10)
    private LedgerType ledgerType;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long remaining;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PointLedgerJpaEntity(UUID ledgerId, UUID crewId, UUID workDayId,
                                 UUID txId, LedgerType ledgerType, Long amount,
                                 Long remaining, LocalDateTime grantedAt,
                                 LocalDateTime expiredAt) {
        this.ledgerId   = ledgerId;
        this.crewId     = crewId;
        this.workDayId  = workDayId;
        this.txId       = txId;
        this.ledgerType = ledgerType;
        this.amount     = amount;
        this.remaining  = remaining;
        this.grantedAt  = grantedAt;
        this.expiredAt  = expiredAt;
    }

    public void updateRemaining(long remaining) {
        this.remaining = remaining;
    }
}