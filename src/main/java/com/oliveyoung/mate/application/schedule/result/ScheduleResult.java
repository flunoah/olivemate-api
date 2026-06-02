package com.oliveyoung.mate.application.schedule.result;

import java.time.LocalDate;
import java.util.List;

public record ScheduleResult(
    List<Integer> daysOfWeek,
    LocalDate startDate,
    LocalDate endDate
) {}
