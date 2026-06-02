package com.oliveyoung.mate.presentation.attendance;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterWorkDayRequest(
    UUID crewId,
    LocalDate workDate
) {}