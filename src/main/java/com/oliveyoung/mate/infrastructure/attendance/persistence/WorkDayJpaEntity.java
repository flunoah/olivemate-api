package com.oliveyoung.mate.infrastructure.attendance.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "work_day",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_crew_work_date",
        columnNames = {"crew_id", "work_date"}
    )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkDayJpaEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID workDayId;

    @Column(name = "crew_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID crewId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "point_granted", nullable = false)
    private boolean pointGranted = false;

    @Column(name = "skipped", nullable = false)
    private boolean skipped = false;

    @CreatedDate
    @Column(name = "registered_at", updatable = false)
    private LocalDateTime registeredAt;

    @Builder
    public WorkDayJpaEntity(UUID workDayId, UUID crewId,
                             LocalDate workDate, boolean pointGranted,
                             boolean skipped) {
        this.workDayId    = workDayId;
        this.crewId       = crewId;
        this.workDate     = workDate;
        this.pointGranted = pointGranted;
        this.skipped      = skipped;
    }

    public void markPointGranted() { this.pointGranted = true; }
    public void markSkipped()      { this.skipped = true; }
}
