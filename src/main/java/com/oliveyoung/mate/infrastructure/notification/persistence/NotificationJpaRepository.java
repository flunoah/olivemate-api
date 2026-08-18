package com.oliveyoung.mate.infrastructure.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    List<NotificationJpaEntity> findByCrewIdOrderBySentAtDesc(UUID crewId);

    List<NotificationJpaEntity> findByCrewIdAndReadFalseOrderBySentAtDesc(UUID crewId);

    Optional<NotificationJpaEntity> findByIdAndCrewId(UUID id, UUID crewId);
}