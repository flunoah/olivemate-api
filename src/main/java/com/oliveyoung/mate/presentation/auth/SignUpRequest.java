package com.oliveyoung.mate.presentation.auth;

public record SignUpRequest(
    String loginId,
    String password,
    String name
) {}