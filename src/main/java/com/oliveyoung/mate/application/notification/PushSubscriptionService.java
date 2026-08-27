package com.oliveyoung.mate.application.notification;

import com.oliveyoung.mate.domain.notification.model.PushSubscription;
import com.oliveyoung.mate.domain.notification.repository.PushSubscriptionRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final String vapidPublicKey;

    public PushSubscriptionService(PushSubscriptionRepository pushSubscriptionRepository,
                                    @Value("${push.vapid.public-key}") String vapidPublicKey) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.vapidPublicKey = vapidPublicKey;
    }

    public String getPublicKey() {
        return vapidPublicKey;
    }

    @Transactional
    public void subscribe(CrewId crewId, String endpoint, String p256dh, String auth) {

        if (pushSubscriptionRepository.findByEndpoint(endpoint).isPresent()) {
        return;
        }
        PushSubscription subscription = PushSubscription.create(crewId, endpoint, p256dh, auth);
        pushSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
    }

    // 크루가 가진 모든 기기 구독에 동일하게 적용한다(기기별로 다르게 관리하지 않음)
    @Transactional
    public void updateChannels(CrewId crewId, boolean notifyPointEarned, boolean notifyPointExpiring, boolean notifyAdminAdjusted) {
        pushSubscriptionRepository.findByCrewId(crewId).forEach(sub -> {
            sub.updateChannels(notifyPointEarned, notifyPointExpiring, notifyAdminAdjusted);
            pushSubscriptionRepository.save(sub);
        });
    }
}