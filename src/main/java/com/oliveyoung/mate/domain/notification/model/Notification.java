package com.oliveyoung.mate.domain.notification.model;

import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Notification {

    private final UUID id;
    private final CrewId crewId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final String deepLink;
    private boolean read;
    private final LocalDateTime sentAt;

    private Notification(UUID id, CrewId crewId, NotificationType type,
                          String title, String body, String deepLink,
                          boolean read, LocalDateTime sentAt) {
        this.id = id;
        this.crewId = crewId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.deepLink = deepLink;
        this.read = read;
        this.sentAt = sentAt;
    }

    public static Notification pointEarned(CrewId crewId, Money amount, LocalDate grantedAt) {
        String amountText = "%,d".formatted(amount.amount());
        return new Notification(
            UUID.randomUUID(),
            crewId,
            NotificationType.POINT_EARNED,
            "포인트가 적립됐어요 🎉",
            "어제 근무하신 " + amountText + "P가 적립됐어요.",
            "/history?date=" + grantedAt,
            false,
            LocalDateTime.now()
        );
    }

    public static Notification pointExpiring(CrewId crewId, Money amount, LocalDate expiryDate, int daysLeft) {
        String amountText = "%,d".formatted(amount.amount());
        return new Notification(
            UUID.randomUUID(),
            crewId,
            NotificationType.POINT_EXPIRING,
            "포인트가 곧 소멸돼요 ⏰",
            amountText + "P가 " + daysLeft + "일 후 소멸 예정이에요.",
            "/history?date=" + expiryDate,
            false,
            LocalDateTime.now()
        );
    }

    // DB 조회 결과 복원용 — 반드시 이걸 써야 id·read·sentAt이 DB 값 그대로 복원됨
    public static Notification reconstruct(UUID id, CrewId crewId, NotificationType type,
                                            String title, String body, String deepLink,
                                        boolean read, LocalDateTime sentAt) {
        return new Notification(id, crewId, type, title, body, deepLink, read, sentAt);
}

    public void markAsRead() { this.read = true; }

    public UUID getId() { return id; }
    public CrewId getCrewId() { return crewId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getDeepLink() { return deepLink; }
    public boolean isRead() { return read; }
    public LocalDateTime getSentAt() { return sentAt; }
}