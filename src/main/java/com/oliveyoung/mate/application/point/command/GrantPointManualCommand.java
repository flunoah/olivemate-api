package com.oliveyoung.mate.application.point.command;

import java.time.LocalDate;
import java.util.UUID;

public record GrantPointManualCommand(UUID crewId, LocalDate workDate) {}
