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
    private boolean notifyPointEarned;
    private boolean notifyPointExpiring;
    private boolean notifyAdminAdjusted;

    private PushSubscription(UUID id, CrewId crewId, String endpoint,
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

    public static PushSubscription create(CrewId crewId, String endpoint, String p256dh, String auth) {
        return new PushSubscription(UUID.randomUUID(), crewId, endpoint, p256dh, auth, LocalDateTime.now(),
            true, true, true);
    }

    public static PushSubscription reconstruct(UUID id, CrewId crewId, String endpoint,
                                                String p256dh, String auth, LocalDateTime registeredAt,
                                                boolean notifyPointEarned, boolean notifyPointExpiring, boolean notifyAdminAdjusted) {
        return new PushSubscription(id, crewId, endpoint, p256dh, auth, registeredAt,
            notifyPointEarned, notifyPointExpiring, notifyAdminAdjusted);
    }

    public void updateChannels(boolean notifyPointEarned, boolean notifyPointExpiring, boolean notifyAdminAdjusted) {
        this.notifyPointEarned = notifyPointEarned;
        this.notifyPointExpiring = notifyPointExpiring;
        this.notifyAdminAdjusted = notifyAdminAdjusted;
    }

    public UUID getId() { return id; }
    public CrewId getCrewId() { return crewId; }
    public String getEndpoint() { return endpoint; }
    public String getP256dh() { return p256dh; }
    public String getAuth() { return auth; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public boolean isNotifyPointEarned() { return notifyPointEarned; }
    public boolean isNotifyPointExpiring() { return notifyPointExpiring; }
    public boolean isNotifyAdminAdjusted() { return notifyAdminAdjusted; }
}