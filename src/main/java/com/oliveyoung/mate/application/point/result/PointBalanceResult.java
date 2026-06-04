package com.oliveyoung.mate.application.point.result;

public record PointBalanceResult(
    long balance,
    long expiringIn7Days,
    long expiringIn30Days,
    long monthlyEarned,
    long monthlyUsed,
    long monthlyExpiring
) {}