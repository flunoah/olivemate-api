package com.oliveyoung.mate.infrastructure.notification.persistence;

import com.oliveyoung.mate.domain.notification.model.PushSubscription;
import com.oliveyoung.mate.domain.notification.repository.PushSubscriptionRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PushSubscriptionRepositoryImpl implements PushSubscriptionRepository {

    private final PushSubscriptionJpaRepository jpaRepository;
    private final PushSubscriptionMapper mapper;

    @Override
    public PushSubscription save(PushSubscription subscription) {
        PushSubscriptionJpaEntity saved = jpaRepository.save(mapper.toJpa(subscription));
        return mapper.toDomain(saved);
    }

    @Override
    public List<PushSubscription> findByCrewId(CrewId crewId) {
        return jpaRepository.findByCrewId(crewId.id()).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByEndpoint(String endpoint) {
        jpaRepository.deleteByEndpoint(endpoint);
    }

    @Override
    public Optional<PushSubscription> findByEndpoint(String endpoint) {
        return jpaRepository.findByEndpoint(endpoint).map(mapper::toDomain);
    }
}