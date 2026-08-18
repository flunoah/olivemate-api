package com.oliveyoung.mate.domain.point.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PointAccruedEvent(
    UUID crewId,
    UUID pointId,
    BigDecimal amount,
    LocalDate workDate,     // 근무일 (전날)
    LocalDate expireDate    // 적립일 + 21일
) {}