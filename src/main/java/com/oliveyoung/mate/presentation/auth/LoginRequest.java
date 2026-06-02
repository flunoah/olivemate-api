package com.oliveyoung.mate.presentation.auth;

public record LoginRequest(
    String loginId,
    String password
) {}
