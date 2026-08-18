package com.oliveyoung.mate.domain.notification.repository;

import com.oliveyoung.mate.domain.notification.model.Notification;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);
    List<Notification> findByCrewId(CrewId crewId, boolean unreadOnly);
    void markAsRead(UUID id, CrewId crewId);
}