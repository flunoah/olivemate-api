package com.oliveyoung.mate.infrastructure.notification.persistence;

import com.oliveyoung.mate.domain.notification.model.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID crewId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    private String deepLink;

    @Column(nullable = false)
    private boolean read;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Builder
    public NotificationJpaEntity(UUID id, UUID crewId, NotificationType type,
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

    // PointAccountJpaEntity.updateBalance()와 동일한 뮤테이터 패턴
    public void markAsRead() {
        this.read = true;
    }
}