package com.oliveyoung.mate.application.notification;

import com.oliveyoung.mate.domain.notification.model.Notification;
import com.oliveyoung.mate.domain.notification.repository.NotificationRepository;
import com.oliveyoung.mate.domain.point.event.AdminAdjustedEvent;
import com.oliveyoung.mate.infrastructure.notification.WebPushClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AdminAdjustedNotificationListener {

    private final NotificationRepository notificationRepository;
    private final WebPushClient webPushClient;

    public AdminAdjustedNotificationListener(NotificationRepository notificationRepository,
                                              WebPushClient webPushClient) {
        this.notificationRepository = notificationRepository;
        this.webPushClient = webPushClient;
    }

    // 배치 트랜잭션 커밋 후에만 실행 → 소급 지급 롤백 시 알림 오발송 방지
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdminAdjusted(AdminAdjustedEvent event) {
        Notification notification = Notification.adminAdjusted(event.crewId(), event.workDate());
        notificationRepository.save(notification);
        webPushClient.sendAdminAdjusted(event.crewId(), event.workDate());
    }
}
