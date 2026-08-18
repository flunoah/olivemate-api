package com.oliveyoung.mate.application.notification;

import com.oliveyoung.mate.domain.notification.model.Notification;
import com.oliveyoung.mate.domain.notification.repository.NotificationRepository;
import com.oliveyoung.mate.domain.point.event.PointEarnedEvent;
import com.oliveyoung.mate.infrastructure.notification.WebPushClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PointEarnedNotificationListener {

    private final NotificationRepository notificationRepository;
    private final WebPushClient webPushClient;

    public PointEarnedNotificationListener(NotificationRepository notificationRepository,
                                            WebPushClient webPushClient) {
        this.notificationRepository = notificationRepository;
        this.webPushClient = webPushClient;
    }

    // 배치 트랜잭션 커밋 후에만 실행 → 적립 롤백 시 알림 오발송 방지
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPointEarned(PointEarnedEvent event) {
        var grantedDate = event.grantedAt().toLocalDate();

        Notification notification = Notification.pointEarned(
            event.crewId(), event.amount(), grantedDate
        );
        notificationRepository.save(notification);
        webPushClient.sendPointEarned(event.crewId(), event.amount(), grantedDate);
    }
}