package com.oliveyoung.mate.infrastructure.point.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "point_use_request", uniqueConstraints = @UniqueConstraint(
    name = "uq_use_request_crew_idempotency", columnNames = {"crew_id", "idempotency_key"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointUseRequestJpaEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "crew_id", columnDefinition = "UUID", nullable = false)
    private UUID crewId;

    @Column(name = "idempotency_key", columnDefinition = "UUID", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "tx_id", columnDefinition = "UUID", nullable = false)
    private UUID txId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PointUseRequestJpaEntity(UUID id, UUID crewId, UUID idempotencyKey, UUID txId) {
        this.id             = id;
        this.crewId         = crewId;
        this.idempotencyKey = idempotencyKey;
        this.txId           = txId;
    }
}
