package com.oliveyoung.mate.application.attendance.command;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterWorkDayCommand(
    UUID crewId,
    LocalDate workDate
) {}