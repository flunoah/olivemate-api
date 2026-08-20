package com.oliveyoung.mate.domain.schedule.repository;

import com.oliveyoung.mate.domain.schedule.model.CrewSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrewScheduleRepository {
    CrewSchedule save(CrewSchedule schedule);
    Optional<CrewSchedule> findActiveByCrewId(UUID crewId);
    List<CrewSchedule> findAllEffectiveOn(LocalDate date);
    void deactivateByCrewId(UUID crewId);
}