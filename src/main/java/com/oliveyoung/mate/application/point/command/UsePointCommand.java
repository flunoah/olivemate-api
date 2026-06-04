package com.oliveyoung.mate.application.point.command;

import java.time.LocalDate;
import java.util.UUID;

public record UsePointCommand(
    UUID      crewId,
    long      amount,
    String    description,
    LocalDate usedAt
) {}
