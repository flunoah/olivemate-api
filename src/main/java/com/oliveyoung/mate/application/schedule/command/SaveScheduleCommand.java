package com.oliveyoung.mate.application.schedule.command;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SaveScheduleCommand(
    UUID crewId,
    List<Integer> daysOfWeek,
    LocalDate startDate,
    LocalDate endDate     // null = 무기한
) {}