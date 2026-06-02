package com.oliveyoung.mate.application.crew.command;

public record LoginCommand(
    String loginId,
    String password
) {}