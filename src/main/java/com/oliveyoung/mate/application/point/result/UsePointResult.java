package com.oliveyoung.mate.application.point.result;

public record UsePointResult(
    long usedAmount,
    long remainingBalance
) {}