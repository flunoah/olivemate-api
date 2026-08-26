package com.oliveyoung.mate.application.point.result;

import java.util.UUID;

public record UsePointResult(
    long usedAmount,
    long remainingBalance,
    UUID usedLedgerId
) {}