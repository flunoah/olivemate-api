package com.oliveyoung.mate.infrastructure.notification.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "push_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscriptionJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID crewId;

    @Column(nullable = false, length = 1000, unique = true)
    private String endpoint;

    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @Column(nullable = false)
    private boolean notifyPointEarned;

    @Column(nullable = false)
    private boolean notifyPointExpiring;

    @Column(nullable = false)
    private boolean notifyAdminAdjusted;

    @Builder
    public PushSubscriptionJpaEntity(UUID id, UUID crewId, String endpoint,
                                      String p256dh, String auth, LocalDateTime registeredAt,
                                      boolean notifyPointEarned, boolean notifyPointExpiring, boolean notifyAdminAdjusted) {
        this.id = id;
        this.crewId = crewId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.registeredAt = registeredAt;
        this.notifyPointEarned = notifyPointEarned;
        this.notifyPointExpiring = notifyPointExpiring;
        this.notifyAdminAdjusted = notifyAdminAdjusted;
    }
}