package com.oliveyoung.mate.domain.attendance.repository;

import com.oliveyoung.mate.domain.attendance.model.WorkDay;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkDayRepository {
    WorkDay save(WorkDay workDay);
    Optional<WorkDay> findByCrewIdAndWorkDate(UUID crewId, LocalDate workDate);
    boolean existsByCrewIdAndWorkDate(UUID crewId, LocalDate workDate);
    List<WorkDay> findAllNotGranted(LocalDate before);   // before: 이 날짜보다 이전 미지급 근무일
    List<WorkDay> findByCrewIdAndWorkDateBetween(UUID crewId, LocalDate from, LocalDate to);
    void deleteByCrewIdAndWorkDate(UUID crewId, LocalDate workDate);
    void markPointGranted(UUID workDayId);
}