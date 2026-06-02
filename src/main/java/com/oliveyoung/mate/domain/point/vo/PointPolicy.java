package com.oliveyoung.mate.domain.point.vo;

public record PointPolicy(
    long earnAmount,
    int  grantDelayDays,
    int  expiryDays
) {
    public static PointPolicy defaultPolicy() {
        return new PointPolicy(4_000L, 1, 30);
    }
}