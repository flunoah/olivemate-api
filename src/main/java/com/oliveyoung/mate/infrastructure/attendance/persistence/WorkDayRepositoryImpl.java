package com.oliveyoung.mate.infrastructure.attendance.persistence;

import com.oliveyoung.mate.domain.attendance.model.WorkDay;
import com.oliveyoung.mate.domain.attendance.repository.WorkDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WorkDayRepositoryImpl implements WorkDayRepository {

    private final WorkDayJpaRepository workDayJpaRepository;

    @Override
    public WorkDay save(WorkDay workDay) {
        WorkDayJpaEntity entity = WorkDayJpaEntity.builder()
            .workDayId(workDay.getWorkDayId())
            .crewId(workDay.getCrewId())
            .workDate(workDay.getWorkDate())
            .pointGranted(workDay.isPointGranted())
            .skipped(workDay.isSkipped())
            .build();
        workDayJpaRepository.save(entity);
        return workDay;
    }

    @Override
    public Optional<WorkDay> findByCrewIdAndWorkDate(UUID crewId, LocalDate workDate) {
        return workDayJpaRepository
            .findByCrewIdAndWorkDate(crewId, workDate)
            .map(e -> WorkDay.reconstitute(
                e.getWorkDayId(), e.getCrewId(), e.getWorkDate(),
                e.isPointGranted(), e.isSkipped(), e.getRegisteredAt()));
    }

    @Override
    public boolean existsByCrewIdAndWorkDate(UUID crewId, LocalDate workDate) {
        return workDayJpaRepository.existsByCrewIdAndWorkDate(crewId, workDate);
    }

    @Override
    public List<WorkDay> findAllNotGranted(LocalDate before) {
        return workDayJpaRepository
            .findAllNotGranted(before)
            .stream()
            .map(e -> WorkDay.reconstitute(
                e.getWorkDayId(), e.getCrewId(), e.getWorkDate(),
                e.isPointGranted(), e.isSkipped(), e.getRegisteredAt()))
            .toList();
    }

    @Override
    public List<WorkDay> findByCrewIdAndWorkDateBetween(UUID crewId,
                                                         LocalDate from,
                                                         LocalDate to) {
        return workDayJpaRepository
            .findByCrewIdAndWorkDateBetween(crewId, from, to)
            .stream()
            .map(e -> WorkDay.reconstitute(
                e.getWorkDayId(), e.getCrewId(), e.getWorkDate(),
                e.isPointGranted(), e.isSkipped(), e.getRegisteredAt()))
            .toList();
    }

    @Override
    public void deleteByCrewIdAndWorkDate(UUID crewId, LocalDate workDate) {
        workDayJpaRepository.deleteByCrewIdAndWorkDate(crewId, workDate);
    }

    @Override
    public void markPointGranted(UUID workDayId) {
        workDayJpaRepository.markPointGranted(workDayId);
    }
}
