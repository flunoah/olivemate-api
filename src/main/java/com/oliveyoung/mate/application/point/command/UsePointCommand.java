package com.oliveyoung.mate.application.point.command;

import java.util.UUID;

public record UsePointCommand(
    UUID   crewId,
    long   amount,
    String description
) {}
