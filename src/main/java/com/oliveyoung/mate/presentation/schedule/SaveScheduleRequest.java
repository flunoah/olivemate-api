package com.oliveyoung.mate.presentation.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SaveScheduleRequest(
    UUID crewId,
    List<Integer> daysOfWeek,
    LocalDate startDate,
    LocalDate endDate     // null = 무기한
) {}