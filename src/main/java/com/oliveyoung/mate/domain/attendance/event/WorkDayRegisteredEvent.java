package com.oliveyoung.mate.domain.attendance.event;

import java.util.UUID;

public record WorkDayRegisteredEvent(
    UUID crewId,
    UUID workDayId
) {}