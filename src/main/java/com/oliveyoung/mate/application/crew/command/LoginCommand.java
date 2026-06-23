package com.oliveyoung.mate.application.crew.command;

public record LoginCommand(
    String email,
    String password
) {}
