package com.oliveyoung.mate.application.notification;

import com.oliveyoung.mate.domain.notification.model.Notification;
import com.oliveyoung.mate.domain.notification.repository.NotificationRepository;
import com.oliveyoung.mate.domain.point.event.PointExpiringEvent;
import com.oliveyoung.mate.infrastructure.notification.WebPushClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PointExpiringNotificationListener {

    private final NotificationRepository notificationRepository;
    private final WebPushClient webPushClient;

    public PointExpiringNotificationListener(NotificationRepository notificationRepository,
                                              WebPushClient webPushClient) {
        this.notificationRepository = notificationRepository;
        this.webPushClient = webPushClient;
    }

    // 배치 트랜잭션 커밋 후에만 실행 → 알림 대상 조회 롤백 시 오발송 방지
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPointExpiring(PointExpiringEvent event) {
        var expiryDate = event.expiredAt().toLocalDate();

        Notification notification = Notification.pointExpiring(
            event.crewId(), event.amount(), expiryDate, event.daysLeft()
        );
        notificationRepository.save(notification);
        webPushClient.sendPointExpiring(event.crewId(), event.amount(), expiryDate, event.daysLeft());
    }
}
