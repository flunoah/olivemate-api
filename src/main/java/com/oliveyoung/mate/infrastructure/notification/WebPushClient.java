package com.oliveyoung.mate.infrastructure.notification;

import com.oliveyoung.mate.domain.notification.repository.PushSubscriptionRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebPushClient {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final PushSubscriptionRepository subscriptionRepository;
    private final PushService pushService;

    public WebPushClient(PushSubscriptionRepository subscriptionRepository,
                          @Value("${push.vapid.public-key}") String publicKey,
                          @Value("${push.vapid.private-key}") String privateKey,
                          @Value("${push.vapid.subject}") String subject) throws Exception {
        this.subscriptionRepository = subscriptionRepository;
        this.pushService = new PushService(publicKey, privateKey, subject);
    }

    public void sendPointEarned(CrewId crewId, Money amount, LocalDate grantedAt) {
        var subscriptions = subscriptionRepository.findByCrewId(crewId);
        if (subscriptions.isEmpty()) {
            return;
        }

        String amountText = "%,d".formatted(amount.amount());
        String payload = """
            {"title":"포인트가 적립됐어요 🎉",
             "body":"어제 근무하신 %sP가 적립됐어요.",
             "deepLink":"/points/history?date=%s"}
            """.formatted(amountText, grantedAt);

        for (var sub : subscriptions) {
            try {
                var pushSubscription = new nl.martijndwars.webpush.Subscription(
                    sub.getEndpoint(),
                    new nl.martijndwars.webpush.Subscription.Keys(sub.getP256dh(), sub.getAuth())
                );
                pushService.send(new Notification(pushSubscription, payload));
            } catch (Exception e) {
                subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
            }
        }
    }
}