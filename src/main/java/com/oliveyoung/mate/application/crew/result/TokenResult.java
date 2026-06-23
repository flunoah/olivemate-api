package com.oliveyoung.mate.application.crew.result;

public record TokenResult(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {
    public static TokenResult of(String accessToken, String refreshToken, long expireSeconds) {
        return new TokenResult(accessToken, refreshToken, "Bearer", expireSeconds);
    }
}
