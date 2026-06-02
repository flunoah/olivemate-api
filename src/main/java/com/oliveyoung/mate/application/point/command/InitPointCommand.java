package com.oliveyoung.mate.application.point.command;

import java.util.UUID;

public record InitPointCommand(
    UUID crewId,
    long amount
) {}