package com.oliveyoung.mate.domain.notification.repository;

import com.oliveyoung.mate.domain.notification.model.PushSubscription;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository {
    PushSubscription save(PushSubscription subscription);
    List<PushSubscription> findByCrewId(CrewId crewId);
    void deleteByEndpoint(String endpoint);
    Optional<PushSubscription> findByEndpoint(String endpoint);
}