package com.oliveyoung.mate.infrastructure.point.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "point_account")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccountJpaEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID accountId;

    @Column(name = "crew_id", columnDefinition = "BINARY(16)",
            nullable = false, unique = true)
    private UUID crewId;

    @Column(nullable = false)
    private Long balance;

    @Version
    private Long version;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public PointAccountJpaEntity(UUID accountId, UUID crewId, Long balance) {
        this.accountId = accountId;
        this.crewId    = crewId;
        this.balance   = balance;
    }

    public void updateBalance(long balance) {
        this.balance = balance;
    }
}