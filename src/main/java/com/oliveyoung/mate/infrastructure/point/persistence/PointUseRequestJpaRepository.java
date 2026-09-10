package com.oliveyoung.mate.infrastructure.point.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PointUseRequestJpaRepository
        extends JpaRepository<PointUseRequestJpaEntity, UUID> {

    Optional<PointUseRequestJpaEntity> findByCrewIdAndIdempotencyKey(UUID crewId, UUID idempotencyKey);
}
