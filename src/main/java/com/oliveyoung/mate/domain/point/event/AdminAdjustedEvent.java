package com.oliveyoung.mate.domain.point.event;

import com.oliveyoung.mate.domain.point.vo.CrewId;
import java.time.LocalDate;

public record AdminAdjustedEvent(CrewId crewId, LocalDate workDate) {}
