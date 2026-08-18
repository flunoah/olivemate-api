package com.oliveyoung.mate.application.notification.result;

import com.oliveyoung.mate.domain.notification.model.Notification;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResult(
    UUID id, String type, String title, String body,
    String deepLink, boolean read, LocalDateTime sentAt
) {
    public static NotificationResult from(Notification n) {
        return new NotificationResult(
            n.getId(), n.getType().name(), n.getTitle(), n.getBody(),
            n.getDeepLink(), n.isRead(), n.getSentAt()
        );
    }
}