package com.oliveyoung.mate.infrastructure.schedule.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crew_schedule")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewScheduleJpaEntity {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID scheduleId;

    @Column(name = "crew_id", columnDefinition = "UUID", nullable = false)
    private UUID crewId;

    @Column(name = "days_of_week", nullable = false)
    private String daysOfWeek;  // "1,2,3" 형식으로 저장

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public CrewScheduleJpaEntity(UUID scheduleId, UUID crewId, String daysOfWeek,
                                  LocalDate startDate, LocalDate endDate, boolean isActive) {
        this.scheduleId = scheduleId;
        this.crewId     = crewId;
        this.daysOfWeek = daysOfWeek;
        this.startDate  = startDate;
        this.endDate    = endDate;
        this.isActive   = isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }
}