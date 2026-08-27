package com.oliveyoung.mate.infrastructure.notification.persistence;

import com.oliveyoung.mate.domain.notification.model.PushSubscription;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import org.springframework.stereotype.Component;

@Component
public class PushSubscriptionMapper {

    public PushSubscription toDomain(PushSubscriptionJpaEntity entity) {
        return PushSubscription.reconstruct(
            entity.getId(),
            CrewId.of(entity.getCrewId()),
            entity.getEndpoint(),
            entity.getP256dh(),
            entity.getAuth(),
            entity.getRegisteredAt(),
            entity.isNotifyPointEarned(),
            entity.isNotifyPointExpiring(),
            entity.isNotifyAdminAdjusted()
        );
    }

    public PushSubscriptionJpaEntity toJpa(PushSubscription subscription) {
        return PushSubscriptionJpaEntity.builder()
            .id(subscription.getId())
            .crewId(subscription.getCrewId().id())
            .endpoint(subscription.getEndpoint())
            .p256dh(subscription.getP256dh())
            .auth(subscription.getAuth())
            .registeredAt(subscription.getRegisteredAt())
            .notifyPointEarned(subscription.isNotifyPointEarned())
            .notifyPointExpiring(subscription.isNotifyPointExpiring())
            .notifyAdminAdjusted(subscription.isNotifyAdminAdjusted())
            .build();
    }
}