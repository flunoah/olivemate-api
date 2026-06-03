package com.oliveyoung.mate.infrastructure.attendance.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkDayJpaRepository
        extends JpaRepository<WorkDayJpaEntity, UUID> {

    Optional<WorkDayJpaEntity> findByCrewIdAndWorkDate(UUID crewId, LocalDate workDate);
    boolean existsByCrewIdAndWorkDate(UUID crewId, LocalDate workDate);
    boolean existsByCrewIdAndWorkDateAndSkippedTrue(UUID crewId, LocalDate workDate);
    List<WorkDayJpaEntity> findByCrewIdAndWorkDateBetween(UUID crewId, LocalDate from, LocalDate to);
    void deleteByCrewIdAndWorkDate(UUID crewId, LocalDate workDate);

    @Query("SELECT w FROM WorkDayJpaEntity w WHERE w.pointGranted = false AND w.skipped = false AND w.workDate < :today")
    List<WorkDayJpaEntity> findAllNotGranted(@Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE WorkDayJpaEntity w SET w.pointGranted = true WHERE w.workDayId = :workDayId")
    void markPointGranted(@Param("workDayId") UUID workDayId);
}
