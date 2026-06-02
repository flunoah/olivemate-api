package com.oliveyoung.mate.domain.schedule.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CrewSchedule {

    private final UUID          scheduleId;
    private final UUID          crewId;
    private final List<Integer> daysOfWeek;   // 0=일, 1=월 ... 6=토
    private final LocalDate     startDate;
    private final LocalDate     endDate;      // null = 무기한
    private boolean             isActive;
    private final LocalDateTime createdAt;

    private CrewSchedule(UUID scheduleId, UUID crewId, List<Integer> daysOfWeek,
                         LocalDate startDate, LocalDate endDate,
                         boolean isActive, LocalDateTime createdAt) {
        this.scheduleId = scheduleId;
        this.crewId     = crewId;
        this.daysOfWeek = daysOfWeek;
        this.startDate  = startDate;
        this.endDate    = endDate;
        this.isActive   = isActive;
        this.createdAt  = createdAt;
    }

    public static CrewSchedule create(UUID crewId, List<Integer> daysOfWeek,
                                      LocalDate startDate, LocalDate endDate) {
        return new CrewSchedule(
            UUID.randomUUID(), crewId, daysOfWeek,
            startDate, endDate, true, LocalDateTime.now()
        );
    }

    public static CrewSchedule of(UUID scheduleId, UUID crewId, List<Integer> daysOfWeek,
                                   LocalDate startDate, LocalDate endDate,
                                   boolean isActive, LocalDateTime createdAt) {
        return new CrewSchedule(scheduleId, crewId, daysOfWeek,
                                startDate, endDate, isActive, createdAt);
    }

    public void deactivate() { this.isActive = false; }

    public UUID          getScheduleId() { return scheduleId; }
    public UUID          getCrewId()     { return crewId; }
    public List<Integer> getDaysOfWeek() { return daysOfWeek; }
    public LocalDate     getStartDate()  { return startDate; }
    public LocalDate     getEndDate()    { return endDate; }
    public boolean       isActive()      { return isActive; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}