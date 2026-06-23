package com.oliveyoung.mate.application.crew.command;

import com.oliveyoung.mate.domain.crew.model.Crew;

public record SignUpCommand(
    String email,
    String password,
    String name,
    Crew.Role role
) {}
