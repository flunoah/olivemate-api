package com.oliveyoung.mate.application.point.command;

import java.util.UUID;

public record CancelUseCommand(UUID ledgerId, UUID crewId) {}
