package com.oliveyoung.mate.domain.attendance.model;

import com.oliveyoung.mate.domain.attendance.event.WorkDayRegisteredEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkDay {

    private final UUID workDayId;
    private final UUID crewId;
    private final LocalDate workDate;
    private boolean pointGranted;
    private boolean skipped;
    private final LocalDateTime registeredAt;
    private final List<Object> domainEvents = new ArrayList<>();

    private WorkDay(UUID workDayId, UUID crewId, LocalDate workDate,
                    boolean pointGranted, boolean skipped, LocalDateTime registeredAt) {
        this.workDayId     = workDayId;
        this.crewId        = crewId;
        this.workDate      = workDate;
        this.pointGranted  = pointGranted;
        this.skipped       = skipped;
        this.registeredAt  = registeredAt;
    }

    // ── 근무일 등록 ────────────────────────────────
    public static WorkDay register(UUID crewId, LocalDate workDate) {
        WorkDay workDay = new WorkDay(
            UUID.randomUUID(), crewId, workDate, false, false, LocalDateTime.now()
        );
        workDay.domainEvents.add(
            new WorkDayRegisteredEvent(crewId, workDay.workDayId)
        );
        return workDay;
    }

    // ── DB 복원 — 도메인 이벤트 발행 없음 ──────────
    public static WorkDay reconstitute(UUID workDayId, UUID crewId, LocalDate workDate,
                                       boolean pointGranted, boolean skipped,
                                       LocalDateTime registeredAt) {
        return new WorkDay(workDayId, crewId, workDate, pointGranted, skipped, registeredAt);
    }

    // ── 결근·조퇴 선처리 (스케줄러 생성 전 차단용) ─
    public static WorkDay createAbsent(UUID crewId, LocalDate workDate) {
        return new WorkDay(UUID.randomUUID(), crewId, workDate, false, true, LocalDateTime.now());
    }

    // ── 포인트 지급 완료 처리 ──────────────────────
    public void markPointGranted() {
        this.pointGranted = true;
    }

    // ── 결근·조퇴 처리 ────────────────────────────
    public void skip() {
        this.skipped = true;
    }

    // ── 결근 복원 ──────────────────────────────────
    public void reinstate() {
        this.skipped = false;
    }

    // ── Getters ────────────────────────────────────
    public UUID          getWorkDayId()    { return workDayId; }
    public UUID          getCrewId()       { return crewId; }
    public LocalDate     getWorkDate()     { return workDate; }
    public boolean       isPointGranted()  { return pointGranted; }
    public boolean       isSkipped()       { return skipped; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}