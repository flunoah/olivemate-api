package com.oliveyoung.mate.infrastructure.schedule.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrewScheduleJpaRepository
        extends JpaRepository<CrewScheduleJpaEntity, UUID> {

    Optional<CrewScheduleJpaEntity> findByCrewIdAndIsActiveTrue(UUID crewId);

    List<CrewScheduleJpaEntity> findAllByIsActiveTrue();

    @Modifying
    @Query("UPDATE CrewScheduleJpaEntity c SET c.isActive = false WHERE c.crewId = :crewId")
    void deactivateByCrewId(@Param("crewId") UUID crewId);
}