package com.oliveyoung.mate.presentation.notification;

import com.oliveyoung.mate.application.notification.result.NotificationResult;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String type,
    String title,
    String body,
    String deepLink,
    boolean read,
    LocalDateTime sentAt
) {
    public static NotificationResponse from(NotificationResult result) {
        return new NotificationResponse(
            result.id(),
            result.type(),
            result.title(),
            result.body(),
            result.deepLink(),
            result.read(),
            result.sentAt()
        );
    }
}