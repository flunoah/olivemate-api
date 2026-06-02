package com.oliveyoung.mate.infrastructure.schedule.persistence;

import com.oliveyoung.mate.domain.schedule.model.CrewSchedule;
import com.oliveyoung.mate.domain.schedule.repository.CrewScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CrewScheduleRepositoryImpl implements CrewScheduleRepository {

    private final CrewScheduleJpaRepository jpaRepository;

    @Override
    public CrewSchedule save(CrewSchedule schedule) {
        String days = schedule.getDaysOfWeek().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        CrewScheduleJpaEntity entity = CrewScheduleJpaEntity.builder()
            .scheduleId(schedule.getScheduleId())
            .crewId(schedule.getCrewId())
            .daysOfWeek(days)
            .startDate(schedule.getStartDate())
            .endDate(schedule.getEndDate())
            .isActive(schedule.isActive())
            .build();

        jpaRepository.save(entity);
        return schedule;
    }

    @Override
    public Optional<CrewSchedule> findActiveByCrewId(UUID crewId) {
        return jpaRepository.findByCrewIdAndIsActiveTrue(crewId)
            .map(this::toDomain);
    }

    @Override
    public List<CrewSchedule> findAllActive() {
        return jpaRepository.findAllByIsActiveTrue().stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void deactivateByCrewId(UUID crewId) {
        jpaRepository.deactivateByCrewId(crewId);
    }

    private CrewSchedule toDomain(CrewScheduleJpaEntity entity) {
        List<Integer> days = Arrays.stream(entity.getDaysOfWeek().split(","))
            .map(Integer::parseInt)
            .toList();
        return CrewSchedule.of(
            entity.getScheduleId(),
            entity.getCrewId(),
            days,
            entity.getStartDate(),
            entity.getEndDate(),
            entity.isActive(),
            entity.getCreatedAt()
        );
    }
}