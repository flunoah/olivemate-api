package com.oliveyoung.mate.infrastructure.notification;

import com.oliveyoung.mate.domain.notification.repository.PushSubscriptionRepository;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebPushClient {

    private static final Logger log = LoggerFactory.getLogger(WebPushClient.class);

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
                HttpResponse response = pushService.send(new Notification(pushSubscription, payload));
                int status = response.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                } else if (status >= 400) {
                    log.warn("Web Push 발송 실패 (endpoint={}, status={})", sub.getEndpoint(), status);
                }
            } catch (IOException | ExecutionException | InterruptedException e) {
                log.warn("Web Push 일시적 오류, 구독 유지 (endpoint={})", sub.getEndpoint(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } catch (GeneralSecurityException | JoseException e) {
                log.error("Web Push VAPID 서명 오류, 구독 유지 (endpoint={})", sub.getEndpoint(), e);
            }
        }
    }
}