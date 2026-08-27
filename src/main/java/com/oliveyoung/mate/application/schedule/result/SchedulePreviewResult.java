package com.oliveyoung.mate.application.schedule.result;

public record SchedulePreviewResult(
    int oldDayCount, int newDayCount,
    long oldMonthlyEstimate, long newMonthlyEstimate
) {}
