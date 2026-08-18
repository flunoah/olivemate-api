package com.oliveyoung.mate.domain.notification.model;

import com.oliveyoung.mate.domain.point.vo.CrewId;
import java.time.LocalDateTime;
import java.util.UUID;

public class PushSubscription {

    private final UUID id;
    private final CrewId crewId;
    private final String endpoint;
    private final String p256dh;
    private final String auth;
    private final LocalDateTime registeredAt;

    private PushSubscription(UUID id, CrewId crewId, String endpoint,
                              String p256dh, String auth, LocalDateTime registeredAt) {
        this.id = id;
        this.crewId = crewId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.registeredAt = registeredAt;
    }

    public static PushSubscription create(CrewId crewId, String endpoint, String p256dh, String auth) {
        return new PushSubscription(UUID.randomUUID(), crewId, endpoint, p256dh, auth, LocalDateTime.now());
    }

    public static PushSubscription reconstruct(UUID id, CrewId crewId, String endpoint,
                                                String p256dh, String auth, LocalDateTime registeredAt) {
        return new PushSubscription(id, crewId, endpoint, p256dh, auth, registeredAt);
    }

    public UUID getId() { return id; }
    public CrewId getCrewId() { return crewId; }
    public String getEndpoint() { return endpoint; }
    public String getP256dh() { return p256dh; }
    public String getAuth() { return auth; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
}