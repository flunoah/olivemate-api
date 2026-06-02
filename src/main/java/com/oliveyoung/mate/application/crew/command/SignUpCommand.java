package com.oliveyoung.mate.application.crew.command;

public record SignUpCommand(
    String loginId,
    String password,
    String name
) {}