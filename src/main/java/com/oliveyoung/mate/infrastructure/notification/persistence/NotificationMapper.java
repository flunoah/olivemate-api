package com.oliveyoung.mate.infrastructure.notification.persistence;

import com.oliveyoung.mate.domain.notification.model.Notification;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public Notification toDomain(NotificationJpaEntity entity) {
        return Notification.reconstruct(
            entity.getId(),
            CrewId.of(entity.getCrewId()),
            entity.getType(),
            entity.getTitle(),
            entity.getBody(),
            entity.getDeepLink(),
            entity.isRead(),
            entity.getSentAt()
        );
    }

    public NotificationJpaEntity toJpa(Notification notification) {
        return NotificationJpaEntity.builder()
            .id(notification.getId())
            .crewId(notification.getCrewId().id())
            .type(notification.getType())
            .title(notification.getTitle())
            .body(notification.getBody())
            .deepLink(notification.getDeepLink())
            .read(notification.isRead())
            .sentAt(notification.getSentAt())
            .build();
    }
}