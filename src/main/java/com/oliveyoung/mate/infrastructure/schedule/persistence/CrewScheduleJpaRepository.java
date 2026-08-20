package com.oliveyoung.mate.infrastructure.schedule.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrewScheduleJpaRepository
        extends JpaRepository<CrewScheduleJpaEntity, UUID> {

    Optional<CrewScheduleJpaEntity> findByCrewIdAndIsActiveTrue(UUID crewId);

    @Query("SELECT c FROM CrewScheduleJpaEntity c " +
           "WHERE c.startDate <= :date AND (c.endDate IS NULL OR c.endDate >= :date)")
    List<CrewScheduleJpaEntity> findAllEffectiveCandidates(@Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE CrewScheduleJpaEntity c SET c.isActive = false WHERE c.crewId = :crewId")
    void deactivateByCrewId(@Param("crewId") UUID crewId);
}