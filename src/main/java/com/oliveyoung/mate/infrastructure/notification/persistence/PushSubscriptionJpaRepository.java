package com.oliveyoung.mate.infrastructure.notification.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionJpaRepository extends JpaRepository<PushSubscriptionJpaEntity, UUID> {
    List<PushSubscriptionJpaEntity> findByCrewId(UUID crewId);
    void deleteByEndpoint(String endpoint);
    Optional<PushSubscriptionJpaEntity> findByEndpoint(String endpoint);
}