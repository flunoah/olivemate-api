package com.oliveyoung.mate.infrastructure.point.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PointAccountJpaRepository
        extends JpaRepository<PointAccountJpaEntity, UUID> {

    Optional<PointAccountJpaEntity> findByCrewId(UUID crewId);
}