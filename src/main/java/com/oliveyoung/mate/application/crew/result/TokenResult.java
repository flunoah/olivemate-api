package com.oliveyoung.mate.application.crew.result;

public record TokenResult(
    String accessToken,
    String tokenType,
    long expiresIn
) {
    public static TokenResult of(String accessToken, long expireSeconds) {
        return new TokenResult(accessToken, "Bearer", expireSeconds);
    }
}