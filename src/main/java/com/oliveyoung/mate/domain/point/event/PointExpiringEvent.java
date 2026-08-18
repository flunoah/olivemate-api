package com.oliveyoung.mate.domain.point.event;

import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import java.time.LocalDateTime;

public record PointExpiringEvent(
    CrewId crewId,
    Money amount,
    LocalDateTime expiredAt,
    int daysLeft
) {}
